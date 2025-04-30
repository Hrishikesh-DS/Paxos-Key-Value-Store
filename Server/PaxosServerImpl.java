package Server;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import Log.Logger;

/**
 * PaxosServerImpl:
 * 
 * Implements the PaxosServerInterface for handling a distributed Key-Value
 * store using the Paxos consensus algorithm. Each instance represents one
 * replicated server node. This class internally manages:
 * - A local key-value mapStore.
 * - A reference list of other servers in the cluster (serverList).
 * - State needed for the Paxos protocol (lastPrepared, lastAcceptedId, etc.).
 * - An AcceptorRunnable thread simulating an acceptor that can fail/restart.
 * 
 * Each server can receive client requests (GET, PUT, DELETE) via RMI and
 * ensures they go through Paxos for consistent replication across the cluster.
 */
public class PaxosServerImpl implements PaxosServerInterface {

  private static Logger logger = new Logger(System.out);

  final int port;

  private Map<String, String> mapStore;

  private List<PaxosServerInterface> serverList;

  private long lastPrepared;

  private long lastAcceptedId;

  private Consumer<Map<String, String>> lastAcceptedCommand;

  private AcceptorSimulator acceptorRunnable;

  private Thread acceptorRunnableThread;

  /**
   * Constructs the PaxosServerImpl with a local concurrent map store,
   * initializes Paxos state, starts the acceptor failure-simulation thread,
   * and logs a startup message.
   * 
   * @param port The RMI registry port for this server.
   */
  public PaxosServerImpl(int port) {
    // ConcurrentHashMap is used for thread-safe access to the key-value store
    mapStore = new ConcurrentHashMap<>();
    serverList = new ArrayList<>();

    // Initialize Paxos state variables
    lastAcceptedId = -1;
    lastPrepared = -1;
    mapStore = new ConcurrentHashMap<>();
    this.port = port;

    // Create and start the AcceptorRunnable for random failures
    acceptorRunnable = new AcceptorSimulator(this);
    acceptorRunnableThread = new Thread(acceptorRunnable);
    acceptorRunnableThread.start();

    log("Server started at port:" + port);
  }

  /**
   * Retrieves the value for a specified key from the local mapStore.
   * Does not invoke Paxos, because GET is a read-only operation. However,
   * the data is only as up-to-date as the server's last accepted writes.
   *
   * @param key The key to retrieve from the KV-store.
   * @return The value if the key exists, otherwise a 'key not present' message.
   * @throws RemoteException Standard RMI exception.
   */
  @Override
  public String get(String key) throws RemoteException {
    log("Received GET request: " + key);

    if (mapStore.containsKey(key)) {
      String response = mapStore.get(key);
      logResponse(response);
      return response;
    }
    return "Key " + key + " not present in store";
  }

  /**
   * Puts a new key-value pair into the store (or updates if the key exists).
   * This operation must go through Paxos consensus to ensure all replicas
   * apply it consistently.
   *
   * @param key   The key to be inserted/updated.
   * @param value The value to associate with 'key'.
   * @return A success or fail message depending on whether Paxos reached
   *         consensus.
   * @throws RemoteException, ExecutionException, InterruptedException RMI or
   *                          concurrency issues.
   */
  @Override
  public String put(String key, String value) throws RemoteException, ExecutionException, InterruptedException {
    // proceedPaxos tries to propose the "PUT" to all servers
    boolean status = proceedPaxos(mapStore -> mapStore.put(key, value));

    String response = status
        ? "Value updated for " + key + " successfully"
        : "Value update for " + key + " failed. Please try again.";

    logResponse(response);
    log("Current value:" + mapStore.get(key));
    return response;
  }

  /**
   * Deletes an existing key from the store, if present. Also must go
   * through Paxos for consistency across replicas.
   *
   * @param key The key to be deleted.
   * @return A message indicating success, failure, or if the key was not present.
   * @throws RemoteException, ExecutionException, InterruptedException RMI or
   *                          concurrency issues.
   */
  @Override
  public String delete(String key) throws RemoteException, ExecutionException, InterruptedException {
    log("Received DELETE request - Key: " + key);

    if (mapStore.containsKey(key)) {
      // Propose a "DELETE" operation to the cluster
      boolean status = proceedPaxos(mapStore -> mapStore.remove(key));

      String response = status
          ? key + " removed successfully"
          : key + " unable to be deleted. Please try again.";
      logResponse(response);
      return response;
    } else {
      String response = key + " was not present in the store to be deleted";
      logResponse(response);
      return response;
    }
  }

  /**
   * Handles a Paxos 'prepare' request (Phase 1).
   * If this server is "down" (AcceptorRunnable not running), returns null.
   * Otherwise, if 'prepareId' is greater than our lastPrepared, we
   * promise not to accept proposals below 'prepareId'. We return
   * a PreparePromise containing info about any previously accepted proposal.
   *
   * @param prepareId The proposal ID from the proposer.
   * @return A PreparePromise with lastAccepted info or null if we reject.
   */
  @Override
  public PreparePromise onPrepare(long prepareId) {
    if (!acceptorRunnable.isRunning()) {
      log("Acceptor thread not running, skipping prepare request.");
      return null; // Simulate "down" acceptor
    }

    // Reject if we've already prepared a higher ID
    if (lastPrepared > prepareId) {
      return null;
    }

    // Otherwise, update 'lastPrepared' and return any accepted proposal info
    lastPrepared = prepareId;
    if (lastAcceptedId == -1) {
      return new PreparePromise(prepareId);
    } else {
      return new PreparePromise(prepareId, lastAcceptedId, lastAcceptedCommand);
    }
  }

  /**
   * Handles a Paxos 'accept' request (Phase 2).
   * If this server is "down," or if 'acceptId' is less than our last promise,
   * we reject by returning null. If we accept, we set lastAcceptedId and
   * lastAcceptedCommand, then notify the entire cluster to update their learner
   * state.
   *
   * @param acceptId    The proposal ID being accepted.
   * @param mapConsumer The consumer function representing the operation to apply
   *                    (PUT, DELETE, etc.).
   * @return An AcceptMessage if accepted, or null if rejected.
   */
  @Override
  public AcceptMessage onAccept(long acceptId, Consumer<Map<String, String>> mapConsumer) {
    if (!acceptorRunnable.isRunning()) {
      log("Acceptor thread not running, skipping accept request.");
      return null;
    }

    // If we've promised something higher or accepted something higher, reject
    if (acceptId < lastPrepared || acceptId < lastAcceptedId) {
      return null;
    }

    // Accept this proposal
    lastAcceptedId = acceptId;
    lastAcceptedCommand = mapConsumer;

    // Create an AcceptMessage to notify other servers' learners
    AcceptMessage acceptMessage = new AcceptMessage(acceptId, mapConsumer);
    for (PaxosServerInterface server : serverList) {
      try {
        server.notifyLearner(acceptMessage);
      } catch (RemoteException e) {
        log("Remote exception encountered");
      }
    }

    return acceptMessage;
  }

  /**
   * Called when a server learns that a proposal was accepted.
   * We reset our Paxos state (lastAcceptedId, lastPrepared) and apply the
   * operation to our local mapStore by invoking the 'consumer'.
   *
   * @param message The AcceptMessage containing the accepted ID and command.
   */
  @Override
  public void notifyLearner(AcceptMessage message) {
    // Reset local Paxos state for new proposals
    lastAcceptedId = -1;
    lastPrepared = -1;

    // Apply the operation to local store
    message.getOperation().accept(mapStore);
    // Optionally log that the learner was updated
  }

  /**
   * Allows the newly created server to receive references to all other servers
   * in the cluster. These references are used for Paxos calls during 'prepare'
   * and 'accept'.
   *
   * @param servers A List of all PaxosServerInterface references for the cluster.
   */
  @Override
  public void syncConnectedServers(List<PaxosServerInterface> servers) {
    serverList = servers;
  }

  /**
   * Logs a message with the server port included for clarity.
   *
   * @param message The text to log.
   */
  private void log(String message) {
    logger.log("Port:" + port + ":" + message);
  }

  /**
   * Logs a response message with a prefix. Used for GET/PUT/DELETE responses.
   *
   * @param response The message to log as a response.
   */
  private void logResponse(String response) {
    logger.log("Sending Response: " + response);
  }

  /**
   * Orchestrates the Paxos flow for a single update command:
   * 1) Phase 1: 'prepare' calls to all servers to see if a majority promise
   * 2) Phase 2: 'accept' calls to attempt to finalize the update
   *
   * @param commandIssued A Consumer that, when invoked, mutates the mapStore
   *                      (e.g., mapStore.put).
   * @return True if a majority responded during prepare; otherwise false
   *         (operation is aborted).
   */
  private boolean proceedPaxos(Consumer<Map<String, String>> commandIssued) {
    long id = System.currentTimeMillis(); // Using current time as the proposal ID
    int prepareAccepted = 0;
    Consumer<Map<String, String>> command = commandIssued;
    long currGreatestAccepted = -1;

    // Phase 1 (Prepare): gather promises
    for (PaxosServerInterface server : serverList) {
      try {
        PreparePromise promise = server.onPrepare(id);
        if (promise != null) {
          prepareAccepted++;
          // If the acceptor had a higher accepted ID, adopt its command
          if (promise.getPreviousAcceptedId() > currGreatestAccepted) {
            currGreatestAccepted = promise.getPreviousAcceptedId();
            command = promise.getAcceptedCommand();
          }
        }
      } catch (RemoteException e) {
        log("Remote Exception occurred.");
      }
    }

    // Check if we got a majority
    if (prepareAccepted <= Math.floor(serverList.size() / 2)) {
      log("Majority not received. Aborting...");
      return false; // Not enough servers promised
    }

    // If majority promised, move to accept phase
    log("Majority of " + prepareAccepted + " received. Accepting phase initiating...");
    runPhaseTwo(id, command);
    return true;
  }

  /**
   * Phase 2 (Accept): sends 'accept' calls to the servers with the final chosen
   * command.
   * Counts how many servers actually accept it. If a majority accept, logs
   * "Consensus reached."
   *
   * @param id      The proposal ID chosen in Phase 1
   * @param command The final operation to propose (could be ours or an adopted
   *                one)
   * @return True (the method doesn't currently handle a re-try if majority fails,
   *         but logs the result).
   */
  private boolean runPhaseTwo(long id, Consumer<Map<String, String>> command) {
    int acceptedCount = 0;

    for (PaxosServerInterface server : serverList) {
      try {
        if (server.onAccept(id, command) != null) {
          acceptedCount++;
        }
      } catch (RemoteException e) {
        log("Remote exception encountered");
      }
    }

    // Check if a majority accepted
    if (acceptedCount > Math.floor(serverList.size() / 2)) {
      log("Consensus reached");
    } else {
      log("Majority did not accept");
    }
    return true;
  }

}

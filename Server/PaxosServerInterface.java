package Server;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Represents a Paxos node interface with all required methods
 * for the Paxos consensus protocol over RMI.
 * Extends ServerInterface to support base server functionality.
 */
public interface PaxosServerInterface extends ServerInterface {

  /**
   * Processes a prepare phase request using the provided proposal ID.
   *
   * @param prepareId unique identifier for the prepare attempt
   * @return a PreparePromise representing the response of the prepare phase
   * @throws RemoteException if RMI communication fails
   */
  PreparePromise onPrepare(long prepareId) throws RemoteException;

  /**
   * Processes an accept phase request using the given proposal ID
   * and a consumer to apply the proposed operation.
   *
   * @param acceptId    ID associated with the accept attempt
   * @param mapConsumer consumer that applies the update logic on the key-value
   *                    map
   * @return an AcceptMessage containing the acceptance response
   * @throws RemoteException if a remote invocation error occurs
   */
  AcceptMessage onAccept(long acceptId, Consumer<Map<String, String>> mapConsumer) throws RemoteException;

  /**
   * Notifies the learner component with an accepted message to commit the value.
   *
   * @param message the message accepted during the consensus process
   * @throws RemoteException if RMI communication fails
   */
  void notifyLearner(AcceptMessage message) throws RemoteException;

  /**
   * Refreshes the server's list of known Paxos participants to ensure
   * connectivity.
   *
   * @param servers the updated list of PaxosServerInterface instances
   * @throws RemoteException if there is an error during remote method invocation
   */
  void syncConnectedServers(List<PaxosServerInterface> servers) throws RemoteException;

}

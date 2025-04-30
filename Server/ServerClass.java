package Server;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ServerClass is the entry point for starting the server.
 */
public class ServerClass {

  private static final int SERVER_COUNT = 5;

  private static List<PaxosServerInterface> servers = new ArrayList<>();

  private static List<ServerInterface> stubs = new ArrayList<>();

  private static List<Integer> serverPorts = new ArrayList<>();

  /**
   * The main method starts the server by initializing and binding the RMI server
   * implementation.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {

    if (args.length == SERVER_COUNT) {
      try {
        for (int i = 0; i < SERVER_COUNT; i++) {
          serverPorts.add(Integer.parseInt(args[i]));
        }
      } catch (NumberFormatException e) {
        System.out.println("Invalid port provided. Reverting to defaults");
      }
    } else
      serverPorts = Arrays.asList(5000, 5001, 5002, 5003, 5004);

    initializeServers(serverPorts);
    ServerOperator serverOperator = new ServerOperator();
    serverOperator.start();
    try {
      serverOperator.join(); // Wait for ServerOperator to complete
    } catch (InterruptedException e) {
      System.out.println("Main thread interrupted while waiting for server operator to close.");
    }
    System.out.println("Main thread exiting");
    shutdownServer();
    System.exit(0);
  }

  /**
   * Shuts down the created servers after removing from the registry.
   */
  private static void shutdownServer() {
    for (int i = 0; i < SERVER_COUNT; i++) {
      try {
        Registry registry = LocateRegistry.getRegistry(serverPorts.get(i));
        registry.unbind("Store");
      } catch (RemoteException | NotBoundException e) {
        System.out
            .println("Error during server shutdown at port - " + serverPorts.get(i) + " with error: " + e.getMessage());
      }
    }
    System.out.println("All servers shut down successfully.");
  }

  /**
   * Setting up multiple paxos server instances and connects them with each other
   * 
   * @param serverPorts list of port numbers for the server ports
   */
  private static void initializeServers(List<Integer> ports) {
    List<Integer> assignedPorts = new ArrayList<>(ports);

    for (int index = 0; index < SERVER_COUNT; index++) {
      boolean isInitialized = false;
      int attempts = 0;
      final int maxAttempts = 5;

      while (attempts < maxAttempts) {
        int currentPort = assignedPorts.get(index);
        try {
          Registry rmiRegistry = LocateRegistry.createRegistry(currentPort);
          PaxosServerInterface paxosNode = new PaxosServerImpl(currentPort);
          ServerInterface remoteStub = (ServerInterface) UnicastRemoteObject.exportObject(paxosNode, 0);

          stubs.add(remoteStub);
          servers.add(paxosNode);
          rmiRegistry.rebind("Store", remoteStub);

          isInitialized = true;
          break;
        } catch (RemoteException ex) {
          System.out.printf("Failed to start server %d on port %d. Reason: %s%n", index, currentPort, ex.getMessage());
          assignedPorts.set(index, generateNewPort(5500, 6000));
          attempts++;
        }
      }

      if (!isInitialized) {
        System.err.println("Terminating setup: Unable to initialize server after multiple attempts.");
        System.exit(1);
      }
    }

    connectAllServers();
  }

  private static void connectAllServers() {
    for (PaxosServerInterface serverInstance : servers) {
      try {
        serverInstance.syncConnectedServers(servers);
      } catch (RemoteException err) {
        System.err.println("Error linking server peers: " + err.getMessage());
      }
    }
  }

  private static int generateNewPort(int min, int max) {
    return getRandomNumber(min, max);
  }

  private static int getRandomNumber(int min, int max) {
    return (int) ((Math.random() * (max - min)) + min);
  }
}

package Server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutionException;

/**
 * The ServerInterface interface defines the methods for performing operations
 * on a remote key-value store.
 * It extends the Remote interface to support remote method invocation (RMI).
 */
public interface ServerInterface extends Remote {

  /**
   * Retrieves the value associated with the specified key from the store.
   *
   * @param key the key whose associated value is to be returned
   * @return the value associated with the specified key, or a message if the key
   *         is not present
   * @throws RemoteException if a remote communication error occurs
   */
  String get(String key) throws RemoteException, ExecutionException, InterruptedException;

  /**
   * Puts the specified key-value pair into the store. If the key already exists,
   * its value is updated.
   *
   * @param key   the key with which the specified value is to be associated
   * @param value the value to be associated with the specified key
   * @return a message indicating the result of the operation
   * @throws RemoteException if a remote communication error occurs
   */
  String put(String key, String value) throws RemoteException, ExecutionException, InterruptedException;

  /**
   * Deletes the key-value pair associated with the specified key from the store.
   *
   * @param key the key whose key-value pair is to be removed
   * @return a message indicating the result of the operation
   * @throws RemoteException if a remote communication error occurs
   */
  String delete(String key) throws RemoteException, ExecutionException, InterruptedException;

}

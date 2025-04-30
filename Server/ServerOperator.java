package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * ServerOperator is a thread that listens for user input to shut down the
 * server.
 * It interacts with the Coordinator to handle the shutdown process.
 */
public class ServerOperator extends Thread {

  /**
   * Runs the server operator thread, listening for user input to initiate server
   * shutdown.
   * The server will close when the user inputs "exit".
   */
  public void run() {
    String input = "";
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    while (!input.equalsIgnoreCase("exit")) {
      System.out.println("The server will close when user inputs \"exit\"");
      try {
        input = in.readLine();
      } catch (IOException e) {
        System.out.println("Server Operator Error:" + e.getMessage());
      }
    }
  }
}

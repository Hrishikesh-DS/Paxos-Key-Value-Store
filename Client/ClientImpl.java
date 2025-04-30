package Client;

import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

import Server.ServerInterface;
import Log.Logger;

/**
 * ClientImpl is the implementation of the Client interface.
 * It handles sending requests to the remote server and logging the responses.
 */
public class ClientImpl implements ClientInterface {

    private static Logger logger = new Logger(System.out);
    private ServerInterface server;

    /**
     * Constructs a ClientImpl with the specified server.
     *
     * @param server the remote server to which requests are sent
     */
    public ClientImpl(ServerInterface server) {
        this.server = server;
    }

    /**
     * Sends requests to the server and handles responses.
     *
     * @param in the input stream from which to read requests
     */
    @Override
    public boolean sendRequests(InputStream in) {
        Scanner inputScanner = new Scanner(in);
        while (inputScanner.hasNext()) {
            String inputText = inputScanner.nextLine();
            if (inputText.equalsIgnoreCase("exit")) {
                System.out.println("Exiting...");
                return false;
            }
            if (inputText.equalsIgnoreCase("switch")) {
                System.out.println("Disconnected from server");
                return true;
            }
            log("Request to be sent to server - " + inputText);
            processRequest(inputText.split(" "));
            System.out.println("Please enter the command i.e, put, get or delete");
            System.out.println(
                    "You can also enter 'exit' to quit or 'switch' to disconnect from this server and connect to a new server");
        }

        return false;
    }

    /**
     * Parses a command based on the command arguments and dispatches the request
     * via server object.
     *
     * @param command the array of command arguments
     */
    private void processRequest(String[] command) {

        try {
            String operation = command[0].toLowerCase();
            String key = command[1];
            String value = command.length > 2 ? command[2] : null;
            String request;
            String response;
            switch (operation) {
                case "put":
                    if (command.length != 3) {
                        log("Invalid command. Put command requires 2 arguments");
                    } else {
                        request = "PUT " + key + " " + value;
                        log(request);
                        response = server.put(key, value);
                        logResponse(response);
                    }
                    break;
                case "get":
                    if (command.length != 2) {
                        log("Invalid command. Get command require 1 argument");
                    } else {
                        request = "GET " + key;
                        log(request);
                        response = server.get(key);
                        logResponse(response);
                    }
                    break;
                case "delete":
                    if (command.length != 2) {
                        log("Invalid command. Delete command require 1 argument");
                    } else {
                        request = "DELETE " + key;
                        log(request);
                        response = server.delete(key);
                        logResponse(response);
                    }
                    break;
                default:
                    log("Invalid command please retry");
            }
        } catch (RemoteException | ExecutionException | InterruptedException e) {
            log("Error encountered while sending request to the server : " + e.getMessage());
        }
    }

    /**
     * Logs a message using the logger.
     *
     * @param message the message to log
     */
    private void log(String message) {
        logger.log(message);
    }

    /**
     * Logs a response message from the server using the logger.
     *
     * @param message the response message to log
     */
    private void logResponse(String message) {
        logger.log("Response from server : " + message);
    }
}

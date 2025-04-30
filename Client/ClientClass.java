package Client;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import Server.ServerInterface;

/**
 * ClientClass is the entry point for the client application.
 * It connects to the remote server and sends requests based on input.
 */
public class ClientClass {

    /**
     * The main method initializes the client application, connects to the remote
     * server,
     * and sends requests from both a file and the standard input.
     *
     * @param args command-line arguments specifying the host of the RMI registry
     */
    public static void main(String[] args) {

        Integer port = (args.length < 1) ? 5002 : Integer.parseInt(args[0]);
        Registry registry = null;
        boolean reconnect = false;
        int retry = 1;
        do {
            if (reconnect) {
                port = readPort();
                if (port == 0) {
                    System.out.println("Exiting the client app...");
                    break;
                }
            }
            try {
                registry = LocateRegistry.getRegistry(port);
                ServerInterface stub = (ServerInterface) registry.lookup("Store");
                ClientInterface client = new ClientImpl(stub);
                InputStream in = null;
                System.out.println("Connected to server in port:" + port);
                try {
                    in = new FileInputStream("ClientData.txt");
                    client.sendRequests(in);
                } catch (FileNotFoundException e) {
                    System.out.println("Error encountered during initial file read. " + e.getMessage());
                    retry++;
                }
                retry = 1;
                in = System.in;
                reconnect = client.sendRequests(in);
            } catch (RemoteException | NotBoundException e) {
                reconnect = true;
                System.out.println("Error encountered while fetching remote object from registry. " + e.getMessage());
                retry++;
            }
        } while (reconnect && retry <= 3);

        if (retry > 3) {
            System.out.println("Retry limit exceeded. Exiting app");
        }
    }

    /**
     * Method to read user input for server port. Invalid entry will result in using
     * the
     * default port 5002.
     * 
     * @return port number read from user input
     */
    private static int readPort() {

        try {
            System.out.println("Enter the port number of the server to connect to - or 0 to exit");
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            return Integer.parseInt(in.readLine());
        } catch (NumberFormatException | IOException e) {
            System.out.println("Port number invalid. Trying to connect to default port 5001...");
            return 5002;
        }

    }
}

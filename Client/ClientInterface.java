package Client;

import java.io.InputStream;

/**
 * ClientInterface interface represents a client that can send requests to a
 * server.
 */
public interface ClientInterface {

    /**
     * Sends requests to the server.
     *
     * @param in the input stream from which to read requests
     */
    boolean sendRequests(InputStream in);
}

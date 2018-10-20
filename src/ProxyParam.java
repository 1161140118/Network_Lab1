import java.net.Socket;

/**
 * 
 * @author chen
 *
 */
public class ProxyParam {
    public final Socket clientSocket;
    public final Socket serverSocket;

    /**
     * Socket pair.
     * @param clientSocket  client socket.
     * @param serverSocket  server socket that the client is to connect.
     */
    public ProxyParam(Socket clientSocket, Socket serverSocket) {
        super();
        this.clientSocket = clientSocket;
        this.serverSocket = serverSocket;
    }
}

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.Line;

/**
 * Create a thread to process the socket.
 * 
 * @author chen
 *
 */
public class Processer extends Thread {
    private InputStream clientInput = null;
    private InputStream serverInput = null;
    private OutputStream clientOutput = null;
    private OutputStream serverOutput = null;
    private BufferedReader clientReader = null;
    private BufferedReader serverReader = null;
    private PrintWriter clientWriter = null;
    private PrintWriter serverWriter = null;



    private final Socket clientSocket;
    private int num;

    /**
     * 
     * @throws IOException Set stream exception.
     * 
     */
    public Processer(Socket clientSocket, int num) throws IOException {
        this.clientSocket = clientSocket;

        // 初始化客户端输入输出
        clientInput = clientSocket.getInputStream();
        clientReader = new BufferedReader(new InputStreamReader(clientInput));
        clientOutput = clientSocket.getOutputStream();
        clientWriter = new PrintWriter(clientOutput);

        // TODO enable multithreading
//        start();
         run();

        System.err.println(" 关闭套接字 : " + num);
        clientSocket.close();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {

        // Get message from client.
        // List<String> httpline = getInput(clientReader);
        // if (httpline == null || httpline.size() == 0) {
        // System.err.println("Failed to get input from client.");
        // return;
        // }

        String firstline = "";// the first line of request http header.

        // Parse httphead.
        try {
            firstline = clientReader.readLine();
            System.out.println("first line      >>> " + firstline);

        } catch (IOException e) {
            System.err.println("Failed to read first line.");
            return;
        }

        // HttpHeader header = new HttpHeader(httpline);

        if (HttpHeader.getMethod(firstline) == null) {
            System.err.println("Ignored method!");
            return;
        }

        String host = HttpHeader.getHostFromURL(HttpHeader.getURL(firstline));
        System.out.println("Get host : " + host);

        // Connect server.
//        Socket serverSocket = connectToServer(host, HttpProxyServer.httpPort, 3);
        Socket serverSocket = connectToServer("192.168.199.131", 10241, 3);
        if (serverSocket == null) {
            return;
        }
        System.out.println("Successful to connect to target server.");


        // Send message to server.
        getAndSendToServer(clientReader, serverWriter, firstline);
        System.err.println("----- Successful to send message to server -----");

        // Send message back to client.
        getAndSendToClient(serverInput, clientOutput);
        System.err.println("----- Successful to send message to client -----");


    }

    // Turn the request from client to server.
    private void getAndSendToServer(BufferedReader clientReader, PrintWriter serverWriter,
            String buffer) {
        while (!buffer.equals("")) {
            buffer += "\r\n";
            serverWriter.write(buffer);
            System.out.print("Send to server      >>> " + buffer);
            try {
                buffer = clientReader.readLine();
                System.out.println("Receive from client <<< " + buffer);
            } catch (IOException e) {
                System.err.println("Failed to read a line from client.");
                return;
            }
        }
        serverWriter.write("\r\n");
        serverWriter.flush();
    }

    // Get message from server and send to client.
    private void getAndSendToClient(InputStream serverInput, OutputStream clientOutput) {
        int length;
        byte[] bytes = new byte[2048];
        while (true) {
            try {
                if ((length = serverInput.read(bytes)) > 0) {
                    clientOutput.write(bytes, 0, length);
                    System.out.println("Receive from server <<< " + new String(bytes));
                } else if (length < 0) {
                    break;
                }
            } catch (IOException e) {
                System.err.println("Failed to get message from server");
            }
        }
        clientWriter.write("\r\n");
        clientWriter.flush();
        clientWriter.close();
    }

    private List<String> getInput(BufferedReader bufferedReader) {
        List<String> httpline = new ArrayList<>();
        String line = "";

        System.out.println(">>> Start read input.");

        // Get request message from client.
        try {
            // while ((line = bufferedReader.readLine()) != null && line.length() != 0) {
            while (bufferedReader.ready()) {
                line = bufferedReader.readLine();
                httpline.add(line);
                System.out.println(">>> " + line);
            }
        } catch (IOException e) {
            System.err.println("Get IOException.");
            return null;
        }
        return httpline;
    }

    private Socket connectToServer(String host, int port, int times) {
        for (int i = 1; i <= times; i++) {
            try {
                Socket serverSocket = new Socket(host, port);

                serverInput = serverSocket.getInputStream();
                serverReader = new BufferedReader(new InputStreamReader(serverInput));
                serverOutput = serverSocket.getOutputStream();
                serverWriter = new PrintWriter(serverOutput);

                return serverSocket;
            } catch (IOException e) {
                serverInput = null;
                serverReader = null;
                serverOutput = null;
                serverWriter = null;
                System.err.println("Failure to connect to target server, repeat for " + (times - i)
                        + " times at most.");
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return null;
    }

    private String getRequestURL(String buffer) {
        String[] tokens = buffer.split(" ");
        String URL = "";
        if (tokens[0].equals("GET"))
            for (int index = 0; index < tokens.length; index++) {
                if (tokens[index].startsWith("http://")) {
                    URL = tokens[index];
                    break;
                }
            }
        return URL;
    }



}

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
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
    private BufferedReader clientInputReader;
    private BufferedWriter clientOutputWriter;
    private BufferedReader serverInputReader;
    private PrintWriter serverOutputWriter;
    private int num;

    /**
     * 
     * @throws IOException Set stream exception.
     * 
     */
    public Processer(Socket clientSocket, int num) throws IOException {
        clientInputReader =
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        clientOutputWriter =
                new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        // TODO enable multithreading
        // start();
        run();

        System.err.println(" ¹Ø±ÕÌ×½Ó×Ö : " + num);
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
        List<String> httpline = getInput(clientInputReader);
        if (httpline == null || httpline.size() == 0) {
            System.err.println("Failed to get input from client.");
            return;
        }

        // Parse httphead.
        HttpHeader header = new HttpHeader(httpline);

        if (header.getMethod() == null) {
            System.err.println("Ignored method!");
            return;
        }

        System.out.println(header.getMethod());
        System.out.println(header.getTargetURL());
        System.out.println(header.getTargetHost());
        System.out.println(header.getCookie());

        // Connect server.
        Socket serverSocket = connectToServer(header.getTargetHost(), HttpProxyServer.httpPort, 3);
        if (serverSocket == null) {
            return;
        }
        System.out.println("Successful to connect to target server.");


        // Send message to server.
        // try {
        System.err.println("\nThe following strings will be send to server:\n");
        for (String string : httpline) {
            System.err.println(string);
            serverOutputWriter.write(string + HttpHeader.delimiter);
            serverOutputWriter.flush();
        }
        // } catch (IOException e) {
        // System.err.println("Failed to send message to server");
        // return;
        // }
        System.out.println("Successful to send message to server");


        // Get message from server
        List<String> response = getInput(serverInputReader);
        if (response == null) {
            System.err.println("Failed to get input.");
        }
        System.err.println(response);



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
                serverInputReader =
                        new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
                // serverOutputWriter = serverSocket.getOutputStream();
                serverOutputWriter = new PrintWriter(serverSocket.getOutputStream(), true);
                return serverSocket;
            } catch (IOException e) {
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



}

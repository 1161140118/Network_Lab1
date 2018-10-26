import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Create a thread to process the socket.
 * 
 * @author chen
 *
 */
public class Processer extends Thread {
    private InputStream serverInput = null;
    private OutputStream clientOutput = null;
    private OutputStream serverOutput = null;
    private BufferedReader clientReader = null;
    private BufferedReader serverReader = null;
    private PrintWriter clientWriter = null;
    private PrintWriter serverWriter = null;

    private HttpHeader httpHeader = null;
    private Socket clientSocket;
    private List<String> cachelines;
    private InputStream cache;
    private int num;


    private static OutputStream file;
    private static HashSet<String> forbidURL = new HashSet<>();
    private static HashSet<String> forbidUsers = new HashSet<>();

    static {
        file = HttpProxyServer.file;
        System.out.println("Successful to init cache file.");
        forbidURL.add("yinle.cc");
        // forbidURL.add("jwts.hit.edu.cn");
        // forbidUsers.add("127.0.0.1");

        // print forbidden message.
        System.out.println("Forbidden URL key word:");
        for (String string : forbidURL) {
            System.out.println("[ " + string + " ]");
        }
        System.out.println("Forbidden user ip:");
        for (String string : forbidUsers) {
            System.out.println("[ " + string + " ]");
        }
        System.out.println();
    }

    /**
     * 
     * @throws IOException Set stream exception.
     * 
     */
    public Processer(Socket clientSocket, int num) throws IOException {
        this.clientSocket = clientSocket;
        this.num = num;
        System.out.println("\n正在处理第 " + num + " 个连接请求...");

        // 初始化客户端输入输出
        clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        clientOutput = clientSocket.getOutputStream();
        clientWriter = new PrintWriter(clientOutput);

        // start();
        run();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {

        /*
         * User filtering
         */
        if (usersFiltering(clientSocket.getInetAddress().getHostAddress())) {
            System.err.println("The \"" + clientSocket.getInetAddress().getHostAddress()
                    + "\" has been filtered.");
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Failed to close client socket!");
            }
            return;
        }

        /*
         * Get message from client.
         */
        List<String> httplines = getFromClient();
        httpHeader = new HttpHeader(httplines); // pares the httphead.

        if (httpHeader.getMethod() == null) {
            System.err.println("Ignored method!");
            return;
        }

        String host = httpHeader.getTargetHost();
        if (host == null) {
            System.err.println("Failed to get host from : " + httpHeader.getFirstline());
            return;
        }
        System.out.println("Get host : " + host);

        /*
         * URL filtering
         */
        if (URLfiltering(httpHeader.getTargetURL())) {
            System.err.println("The \"" + httpHeader.getTargetURL() + "\" has been filtered.");
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Failed to close client socket!");
            }
            return;
        }



        /*
         * Connect server.
         */
        Socket serverSocket = connectToServer(host, HttpProxyServer.httpPort, 3);
        if (serverSocket == null) {
            return;
        }
        System.err.println("----- Successful to connect to target server. ------");


        /*
         * Search the cache.
         */
        String lastModifyTime = null;
        try {
            lastModifyTime = searchCache(httpHeader.getTargetURL() + " ");
        } catch (IOException e2) {
            System.err.println("Failed to search cache.");
            e2.printStackTrace();
        }
        if (lastModifyTime != null) {
            // Find the cache.
            System.out.println("----- Get last modify time: " + lastModifyTime);
            if (!modified(lastModifyTime)) {
                // Not modified, send cache to client.
                try {
                    sendCacheToClient(cachelines);
                } catch (IOException e1) {
                    System.err.println("Falied to send cache to client!");
                    return;
                }
                System.err.println("----- Successful to send cache to client. -----");
                try {
                    clientSocket.close();
                    return;
                } catch (IOException e) {
                    System.err.println("Failed to close client socket!");
                    return;
                }
            }
        }
        System.err.println("----- There is no cache for the request. -----");


        /*
         * No cache. Send message to server.
         */
        writeCache(httpHeader.getFirstline().getBytes(), httpHeader.getFirstline().length());
        writeCache("\r\n".getBytes(), 2);
        SendToServer(httplines);
        System.err.println("----- Successful to send message to server. -----");

        /*
         * Send message back to client.
         */
        getAndSendToClient();
        System.err.println("----- Successful to send message to client. -----");

        try {
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Failed to close client socket!");
        }
    }

    private List<String> getFromClient() {
        List<String> httplines = new LinkedList<>();
        try {
            String buffer = clientReader.readLine();
            System.out.println("Receive from client <<< " + buffer);
            httplines.add(buffer);
            while (!buffer.equals("")) {
                buffer = clientReader.readLine();
                System.out.println("Receive from client <<< " + buffer);
                httplines.add(buffer);
            }
        } catch (IOException e) {
            System.err.println("Failed to read a line from client!");
            return null;
        }
        return httplines;
    }

    /**
     * The url in forbid set will be invalid.
     * 
     * @param url to be checked
     * @return true if filtered.
     */
    private boolean URLfiltering(String url) {
        for (String string : forbidURL) {
            if (url.contains(string)) {
                clientWriter.write("The url has been filtered!\r\n");
                clientWriter.write("\r\n");
                clientWriter.flush();
                return true;
            }
        }
        return false;
    }

    /**
     * The user ip in forbid set will be invalid.
     * 
     * @param usrip
     * @return
     */
    private boolean usersFiltering(String usrip) {
        for (String string : forbidUsers) {
            if (usrip.contains(string)) {
                clientWriter.write("YOU HAS BEEN FILTERED!\r\n");
                clientWriter.write("\r\n");
                clientWriter.flush();
                return true;
            }
        }
        return false;
    }


    // confirm modification time.
    private boolean modified(String lastModifyTime) {
        // send confirming message
        List<String> confirmMessages = new LinkedList<>();
        confirmMessages.add(httpHeader.getFirstline());
        confirmMessages.add("Host: " + httpHeader.getTargetHost());
        confirmMessages.add("If-modified-since: " + lastModifyTime);
        SendToServer(confirmMessages);

        // get confirming message
        try {
            String confirm = serverReader.readLine();
            System.out.println("Get from server     <<< " + confirm);
            if (confirm.contains("Not Modified")) {
                // haven't been modified.
                System.err.println("----- Not modified, and using cache. -----");
                return false;
            }
        } catch (IOException e) {
            System.err.println("Failed to get confirming message from server.");
        }
        return true;
    }

    // Send cache to client.
    private void sendCacheToClient(List<String> cache) throws IOException {
        for (String string : cache) {
            string += "\r\n";
            clientOutput.write(string.getBytes());
            System.out.println("Send cache to client    >>> cache message");
        }
        clientOutput.write("\r\n".getBytes());
        clientOutput.flush();
    }

    // Turn the request from client to server and save the http header lines.
    private void SendToServer(List<String> httplines) {
        for (String buffer : httplines) {
            buffer += "\r\n";
            System.out.print("Send to server      >>> " + buffer);
            serverWriter.write(buffer);
            serverWriter.flush();
        }
        serverWriter.write("\r\n");
        serverWriter.flush();
    }

    // Get message from server and send to client.
    private void getAndSendToClient() {
        int length;
        byte[] bytes = new byte[2048];
        while (true) {
            try {
                if ((length = serverInput.read(bytes)) > 0) {
                    clientOutput.write(bytes, 0, length);
                    writeCache(bytes, length);
                    System.out.println("Receive from server <<< response message...");
                } else if (length < 0) {
                    break;
                }
            } catch (IOException e) {
                System.err.println(num + ": Failed to get message from server!");
                break;
            }
        }
        clientWriter.write("\r\n");
        clientWriter.flush();
        writeCache("\r\n".getBytes(), 2);
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

    // read by line, called in function searchCache.
    private String readCache() throws IOException {
        String line = "";
        int c;
        while (true) {
            c = cache.read();
            if (c == -1 || c == '\n')
                break; // terminal flag
            if (c == '\r') {
                cache.read();
                break;// read by line
            }
            line = line + (char) c;
        }
        return line;
    }

    private String searchCache(String url) throws IOException {
        cachelines = new ArrayList<String>();
        String result = null;
        cache = new FileInputStream("cache.txt");
        String currentline = readCache();
        System.out.println("The first line is   ：" + currentline);
        System.out.println("Finding the url     ：" + url);

        int ch = 0;
        while ((ch = cache.read()) != -1 && currentline != null) {
            // haven't get the terminal.
            if (currentline.contains(url)) {
                // find url, the remaining lines is useful.
                System.out.println("Successful to find  : " + currentline);
                String remaining;
                do {
                    remaining = "";
                    if (ch != '\r' && ch != '\n') {
                        remaining += (char) ch;
                    }
                    // remaining += readCache();
                    while (true) {
                        ch = cache.read();
                        if (ch == -1 || ch == '\n')
                            break;
                        if (ch == '\r') {
                            cache.read();
                            break;
                        }
                        remaining += (char) ch;
                    }
                    System.out.println("The next line       ：" + remaining);
                    if (remaining.contains("Last-Modified:")) {
                        result = remaining.substring(15);
                    }
                    cachelines.add(remaining);
                    if (remaining.equals("")) {
                        return result;
                    }
                } while (!remaining.equals("") && remaining != null && ch != -1);
            }
            // info = readCache(cache);
            // continue finding.
            currentline = "";
            while (true) {
                if (ch == -1 || ch == '\n')
                    break;
                if (ch == '\r') {
                    cache.read();
                    break;
                }
                currentline += (char) ch;
                ch = cache.read();
            }
        }

        return result;
    }


    private void writeCache(byte[] cache, int length) {
        try {
            for (int i = 0; i < length; i++) {
                file.write((char) ((int) cache[i]));// change the coded format.
            }
        } catch (IOException e) {
            System.err.println("Failed to write cache :" + new String(cache));
        }
    }

    // private String getRequestURL(String buffer) {
    // String[] tokens = buffer.split(" ");
    // String URL = "";
    // if (tokens[0].equals("GET"))
    // for (int index = 0; index < tokens.length; index++) {
    // if (tokens[index].startsWith("http://")) {
    // URL = tokens[index];
    // break;
    // }
    // }
    // return URL;
    // }

}

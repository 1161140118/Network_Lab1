import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;


/**
 * @author chen
 *
 */
public class HttpProxyServer {
    public static ServerSocket proxyServer;
    public static final int httpPort = 80;
    public static int proxyPort = 10240;
    public static final int proxyTimeOut = 60000; // wait for no more than 1 minute.
    // public static final int requestTimeOut = 500 ;
    public static String cachePath = "cache.txt";
    public static OutputStream file;


    public static Boolean initSocket(int timeout) {

        try {
            proxyServer = new ServerSocket(proxyPort);
            proxyServer.setSoTimeout(timeout); // set timeout in millisecond

        } catch (IOException e) {
            System.err.println("Failure to initialize proxy server socket.");
            return false;
        }

        System.out.println("初始化成功！");
        return true;
    }

    public static Boolean initCache() {
        // TODO
        return true;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        
        System.out.println("代理服务器正在启动...");
        System.out.println("初始化...");
        
        // parse the cmd parameters.
        if (args.length >= 1) {
            proxyPort = Integer.valueOf(args[0]); // Get proxy port from cmd.
        }
        if (args.length >= 2) {
            cachePath = args[1];    // Get cache file path.
        }

        // Create the main socket and set listening.
        if (!initSocket(proxyTimeOut)) {
            return;
        }
        
        try {
            file = new FileOutputStream(new File(cachePath),true);
        } catch (FileNotFoundException e1) {
            System.err.println("Failed to access cache file!");
            return;
        }

        System.out.println("代理服务器正在运行，监听端口 " + proxyPort);

        Socket acceptSocket = null;
        int i = 0;

        // 代理服务器持续监听，等待客户端连接请求
        while (!proxyServer.isClosed()) {
            i++;

            // 处理连接请求
            try {
                acceptSocket = proxyServer.accept();
                new Processer(acceptSocket, i);
                Thread.sleep(200);
            } catch (IOException e) {
                System.err.println("A connect was refused: " + i);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // 关闭服务器套接字
        try {
            proxyServer.close();
        } catch (IOException e) {
            System.err.println("Failure to close proxyServer!");
        }
    }

}



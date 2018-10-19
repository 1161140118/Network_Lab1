import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;


/**
 * @author chen
 *
 */
public class HttpProxyServer {
    public static ServerSocket proxyServer;
    public static SocketAddress proxyServerAddr;
    public static final int httpPort = 80;
    public static final int proxyPort = 10240;
    public static final int proxyTimeOut = 600000;  // wait for no more than 1 minute.
    public static final int requestTimeOut = 500 ;
    public static String cachePath;


    public static Boolean initSocket(int timeout) {

        try {
            proxyServer = new ServerSocket(proxyPort);
            proxyServer.setSoTimeout(timeout); // set timeout in millisecond
            
        } catch (IOException e) {
            System.err.println("Failure to initialize proxy server socket.");
            return false;
        }

        return true;
    }

    public static Boolean initCache() {

        return true;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        System.out.println("代理服务器正在启动...");
        System.out.println("初始化...");

        // 创建主套接字并监听
        if (!initSocket(proxyTimeOut)) {
            return;
        }

        System.out.println("代理服务器正在运行，监听端口 " + proxyPort);

        Socket acceptSocket = null;



        // 代理服务器持续监听，等待客户端连接请求
        while (!proxyServer.isClosed()) {
            
            
            // 处理连接请求
            try {
                acceptSocket = proxyServer.accept();
                new Processer(acceptSocket);
                
            } catch (IOException e) {
            }

            
        }



        // 关闭服务器套接字
        try {
            proxyServer.close();
        } catch (IOException e) {
            System.out.println("Failure to close proxyServer!");
            e.printStackTrace();
        }
    }

}



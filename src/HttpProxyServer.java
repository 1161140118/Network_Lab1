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
        System.out.println("�����������������...");
        System.out.println("��ʼ��...");

        // �������׽��ֲ�����
        if (!initSocket(proxyTimeOut)) {
            return;
        }

        System.out.println("����������������У������˿� " + proxyPort);

        Socket acceptSocket = null;



        // ��������������������ȴ��ͻ�����������
        while (!proxyServer.isClosed()) {
            
            
            // ������������
            try {
                acceptSocket = proxyServer.accept();
                new Processer(acceptSocket);
                
            } catch (IOException e) {
            }

            
        }



        // �رշ������׽���
        try {
            proxyServer.close();
        } catch (IOException e) {
            System.out.println("Failure to close proxyServer!");
            e.printStackTrace();
        }
    }

}



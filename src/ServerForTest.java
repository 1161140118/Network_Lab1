import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/**
 * @author chen
 *
 */
public class ServerForTest {
    public static final int testServerPort = 10241;
    
    
    
    
    /**
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(testServerPort);
        System.out.println("  测试服务器已启动... ");
        
        Scanner in = new Scanner(System.in);
        
        Socket accept = serverSocket.accept();
        
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(accept.getInputStream()));
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(accept.getOutputStream()));
        
        String line ="";
        
        while( (line=bufferedReader.readLine())!=null) {
            System.out.println(line);
            System.out.println(">>");
            bufferedWriter.write(in.nextLine());
        }
    }

}

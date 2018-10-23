import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;

/**
 * @author chen
 *
 */
public class ClientForTest {
    private static final String serverIp = "172.20.118.52";
    private static final int localPort = 10241;


    /**
     * @param args
     * @throws IOException
     * @throws UnknownHostException
     */
    public static void main(String[] args) throws UnknownHostException, IOException {
        Socket server = new Socket(serverIp, localPort);

        OutputStream bufferedWriter = server.getOutputStream();
        // BufferedWriter bufferedWriter = new BufferedWriter(new
        // OutputStreamWriter(server.getOutputStream()));
        BufferedReader bufferedReader =
                new BufferedReader(new InputStreamReader(server.getInputStream()));
        // InputStream bufferedReader = server.getInputStream();
        Scanner in = new Scanner(System.in);
        String line = "";

        bufferedWriter.write(" I am client.\n".getBytes());
        bufferedWriter.flush();
        bufferedWriter.write(" I am client again.\n".getBytes());
        bufferedWriter.flush();

        // while ((line = in.nextLine()) != null) {
        // bufferedWriter.write((line + "\n").getBytes());
        // }

        int recvsize = 64;
        byte[] buffer = new byte[recvsize];

        // while (bufferedReader.read(buffer) != -1) {
        while ((line = bufferedReader.readLine()) != null) {
            System.out.println(">>>" + line);
        }



    }

}

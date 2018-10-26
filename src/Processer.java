import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.util.LinkedList;
import java.util.List;
import javax.sound.sampled.Line;

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
	private HttpHeader httpHeader;
	private Socket clientSocket;
	private List<String> cachelines;

	private static OutputStream file;
	private int num;

	static {
		file = HttpProxyServer.file;
		System.err.println("----- Successful to init cache file. -----");
	}

	/**
	 * 
	 * @throws IOException Set stream exception.
	 * 
	 */
	public Processer(Socket clientSocket, int num) throws IOException {
		this.clientSocket = clientSocket;
		this.num = num;

		// 初始化客户端输入输出
		clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		clientOutput = clientSocket.getOutputStream();
		clientWriter = new PrintWriter(clientOutput);

//		start();
		 run();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		// Get message from client.
		List<String> httplines = getFromClient();
		HttpHeader header = new HttpHeader(httplines); // pares the httphead.

		if (header.getMethod() == null) {
			System.err.println("Ignored method!");
			return;
		}

		String host = header.getTargetHost();
		if (host==null) {
			System.err.println("Failed to get host from : "+header.getFirstline());
			return;
		}
		System.out.println("Get host : " + host);

		// Connect server.
		Socket serverSocket = connectToServer(host, HttpProxyServer.httpPort, 3);
		if (serverSocket == null) {
			return;
		}
		System.err.println("----- Successful to connect to target server. ------");
		
		// Search the cache.
		String lastModifyTime = searchCache(header.getTargetURL());
		if (lastModifyTime !=null) {
			// Find the cache.
			System.out.println("----- Get last modify time: "+lastModifyTime);
			if (!modified(lastModifyTime)) {
				// Not modified, send cache to client.
				
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

		// Send message to server.
		SendToServer(httplines,false);
		System.err.println("----- Successful to send message to server. -----");

		// Send message back to client.
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
	
	// confirm modification time.
	private boolean modified(String lastModifyTime) {
		// send confirming message
		List<String> confirmMessages = new LinkedList<>();
		confirmMessages.add(httpHeader.getFirstline());
		confirmMessages.add("Host: "+httpHeader.getTargetHost());
		confirmMessages.add("If-modified-since: "+lastModifyTime);
		SendToServer(confirmMessages,true);
		
		// get confirming message
		try {
			String confirm = serverReader.readLine();
			System.out.println("Get from server  	<<< "+confirm);   
			if (confirm.contains("Not Modified")) {
				// haven't been modified.
				System.out.println("Not modified, and use cache.");
				return false;
			}
		} catch (IOException e) {
			System.err.println("Failed to get confirming message from server.");
		}
		return true;
	}

	// Turn the request from client to server and save the http header lines.
	private void SendToServer(List<String> httplines,boolean confirm) {
		for (String buffer : httplines) {
			buffer += "\r\n";
			if (!confirm) {
				// just request message, write into  cache.
				writeCache(buffer.getBytes(), buffer.length());
			}
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
				System.err.println("Failed to get message from server!");
				break;
			}
		}
		clientWriter.write("\r\n");
		clientWriter.flush();
		writeCache("\r\n".getBytes(), 2);
//		System.err.println("----- Transponding to client complete. ----- " + num);
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
				System.err
						.println("Failure to connect to target server, repeat for " + (times - i) + " times at most.");
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
	private String readCache(InputStream cache) throws IOException {
		String line = "";
		while (true) {
			int c = cache.read();
			if (c == -1)
				break; // terminal flag
			if (c == '\r') {
				cache.read();
				break;// read by line
			}
			if (c == '\n')
				break;
			line = line + (char) c;
		}
		return line;
	}

	private String searchCache(String url) {
		cachelines = new ArrayList<String>();
		String result = null;
//		int count = 0;
		try {

			// 直接在存有url和相应信息的文件中查找
			InputStream cache = new FileInputStream("cache.txt");
			String info = readCache(cache);
//            while (true) {
//                int c = cache.read();
//                if (c == -1)
//                    break; // -1为结尾标志
//                if (c == '\r') {
//                    cache.read();
//                    break;// 读入每一行数据
//                }
//                if (c == '\n')
//                    break;
//                info = info + (char) c;
//            }

			System.out.println("第一次得到：" + info);
			System.out.println("要找的是：" + url);

			int m = 0;
			while ((m = cache.read()) != -1 && info != null) {
				// System.out.println("在寻找："+info);
				// 找到相同的，那么它下面的就是响应信息，找上次修改的时间
				if (info.contains(url)) {
					String info1;
					do {
						System.out.println("找到相同的了：" + info);
						info1 = "";
						if (m != '\r' && m != '\n') {
							info1 += (char) m;
						}
						info1 += readCache(cache);
//                        while (true) {
//                            m = cache.read();
//                            if (m == -1)
//                                break;
//                            if (m == '\r') {
//                                cache.read();
//                                break;
//                            }
//                            if (m == '\n') {
//                                break;
//                            }
//                            info1 += (char) m;
//                        }
						System.out.println("info1是：" + info1);
						if (info1.contains("Last-Modified:")) {
							result = info1.substring(16);
						}
						cachelines.add(info1);
						if (info1.equals("")) {
							System.out.print("我是空");
							return result;
						}
					} while (!info1.equals("") && info1 != null && m != -1);
				}
//				info = readCache(cache);
				info = "";
                while (true) {
                    if (m == -1)
                        break;
                    if (m == '\r') {
                        cache.read();
                        break;
                    }
                    if (m == '\n')
                        break;
                    info += (char) m;
                    m = cache.read();
                }
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
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

//	private String getRequestURL(String buffer) {
//		String[] tokens = buffer.split(" ");
//		String URL = "";
//		if (tokens[0].equals("GET"))
//			for (int index = 0; index < tokens.length; index++) {
//				if (tokens[index].startsWith("http://")) {
//					URL = tokens[index];
//					break;
//				}
//			}
//		return URL;
//	}

}

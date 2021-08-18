import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {

	private final String servername;
	private final int serverport;
	private Socket sock;
	private OutputStream serverOut;
	private InputStream serverIn;
	private BufferedReader buffer;
	
	public ChatClient(String server, int port) {
		this.servername = server;
		this.serverport = port;
	}
	
	public static void main(String args[]) throws IOException {
		ChatClient client = new ChatClient("localhost", 9004);
		if (!client.connect()) {
			System.err.println("Could not connect to the server!");
		} else {
			System.out.println("Connected to the server");
			client.login("simon", "simon");
		}
	}
	
	private boolean login(String username, String password) throws IOException {
		String resp;
		String serverMsg;
		Scanner scanner = new Scanner(System.in);
		
		serverOut.write("login\n".getBytes());
		resp = buffer.readLine();
		while (true) {
			System.out.println(resp);
			serverMsg = scanner.nextLine() + "\n";
			serverOut.write(serverMsg.getBytes());
			resp = buffer.readLine();
			if (resp.equalsIgnoreCase("You have successfully logged in!")) {
				return true;
			}
		}
	}
	
	private boolean connect() {
		try {
			this.sock = new Socket(servername, serverport);
			this.serverOut = sock.getOutputStream();
			this.serverIn = sock.getInputStream();
			this.buffer = new BufferedReader(new InputStreamReader(serverIn));
			return true;
		} catch (IOException e) {
			return false;
		}
	}
}

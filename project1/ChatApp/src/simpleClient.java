import java.io.*;
import java.net.*;

public class simpleClient {
	Socket clientSock;
	String address;
	int port;
	OutputStream clientOut;
	BufferedReader clientIn;
	
	public simpleClient(int port) {
		this.port = port;
	}
	
	public static void main(String[] args) throws IOException {
		simpleClient c = new simpleClient(9006);
		if (!c.connect()) {
			System.out.println("Failure to connect");
		} else {
			System.out.println("Successfully connected");
			
			c.login("simon", "simosn");
		}
	}
	
	public void sendMessage(String message) {
		
	}
	
	public boolean login(String username, String password) throws IOException {
		String command = "login "+username+" "+password+"\n";
		clientOut.write(command.getBytes());
		String res = clientIn.readLine();
		System.out.println(res);
		return true;
	}
	
	public boolean connect() {
		try {
			this.clientSock = new Socket("localhost", port);
			clientOut = clientSock.getOutputStream();
			clientIn = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
			String res = clientIn.readLine();
			System.out.println(res);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
}

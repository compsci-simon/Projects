import java.io.*;
import java.net.*;

public class simpleClient {
	Socket clientSock;
	String address;
	int port;
	OutputStream clientOut;
	BufferedReader clientIn;
	
	public simpleClient(String address, int port) {
		this.address = address;
		this.port = port;
	}
	
	public static void main(String[] args) throws IOException {
		simpleClient simon = new simpleClient("localhost", 9005);
		if (!simon.connect()) {
			System.out.println("Failure to connect");
			return;
		} else {
			System.out.println("Successfully connected");
		}
		
		simon.handle_client();
		
		//c.send_message("simon", "hello");
		simon.quit();
		
	}
	
	public void handle_client() throws IOException {
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String resp;
		
		while ((resp = stdIn.readLine()) != null) {
	    	String userText = stdIn.readLine();
	        if (userText != null) {
	            clientOut.write((userText+"\n").getBytes());
	        }
		}
	}
	
	
	public boolean login(String username, String password) throws IOException {
		String command = "login "+username+" "+password+"\n";
		clientOut.write(command.getBytes());
		String res = clientIn.readLine();
		System.out.println(res);
		if (res.compareTo("Login failed!") == 0) {
			return false;
		} else {
			return true;
		}
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
	
	public boolean send_message(String destination, String message) throws IOException {
		String command = "msg " + destination + " " + message + "\n";
		clientOut.write(command.getBytes());
		String res = clientIn.readLine();
		System.out.println(res);
		return true;
	}
	
	public boolean broadcast_message(String message) throws IOException {
		String command = "bcast " + message + "\n";
		clientOut.write(command.getBytes());
		String res = clientIn.readLine();
		System.out.println(res);
		return true;
	}
	
	public void list_users() throws IOException {
		clientOut.write("list\n".getBytes());
		String users = clientIn.readLine();
		System.out.println(users);
	}
	
	public boolean help() throws IOException {
		String command = "help\n";
		clientOut.write(command.getBytes());
		String res = clientIn.readLine();
		System.out.println(res);
		return true;
		
	}
	
	public boolean quit() throws IOException {
		String command = "quit\n";
		clientOut.write(command.getBytes());
		String res = clientIn.readLine();
		System.out.println(res);
		return true;
	}
	
	
	
}

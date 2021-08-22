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
	
	public void recv_messages() throws IOException {
		String line;
		while ((line = clientIn.readLine()) != null) {
			System.out.println(line);
			if (line.equals("Successfully logged out")) {
				break;
			}
		}
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
		if (res == null) {
			System.out.println("You are no longer connected, try again later");
			System.exit(1);
		}
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
			return false;
		}
	}
	
	public void send_message(String destination, String message) throws IOException {
		String command = "msg " + destination + " " + message + "\n";
		try {
			clientOut.write(command.getBytes());
		} catch (Exception e) {
			System.out.println("You are no longer connected, try again later");
			System.exit(1);
		}
	}
	
	public boolean broadcast_message(String message) throws IOException {
		String command = "bcast " + message + "\n";
		try {
			clientOut.write(command.getBytes());
		} catch (Exception e) {
			System.out.println("You are no longer connected, try again later");
			System.exit(1);
		}
		return true;
	}
	
	public void list_users() throws IOException {
		String command = "list\n";
		try {
			clientOut.write(command.getBytes());
		} catch (Exception e) {
			System.out.println("You are no longer connected, try again later");
			System.exit(1);
		}
	}
	
	public boolean help() throws IOException {
		String command = "help\n";
		try {
			clientOut.write(command.getBytes());
		} catch (Exception e) {
			System.out.println("You are no longer connected, try again later");
			System.exit(1);
		}
		return true;
		
	}
	
	public boolean quit() throws IOException {
		String command = "quit\n";
		try {
			clientOut.write(command.getBytes());
		} catch (Exception e) {
			System.out.println("You are no longer connected, try again later");
			System.exit(1);
		}
		return true;
	}
}

package test;
import java.net.*;
import java.io.*;

public class client {
	
	Socket client;
	OutputStream clientOut;
	InputStream clientIn;
	BufferedReader reader;
	
	public client() {
		
	}
	
	public static void main(String[] args) throws IOException {
//		client c = new client();
//		
//		if (!c.connect()) {
//			System.out.println("Connection failed");
//		} else {
//			System.out.println("Connection success");
//			
//			c.write("Hello");
//			System.out.println("Server says: "+ c.getMessages());
//		}
		try {
			    Socket echoSocket = new Socket("localhost", 5000);        // 1st statement
			    OutputStream toServer = (echoSocket.getOutputStream());
			    BufferedReader fromServer =                                          // 3rd statement 
			        new BufferedReader(
			            new InputStreamReader(echoSocket.getInputStream()));
			    BufferedReader stdIn =                                       // 4th statement 
			        new BufferedReader(
			            new InputStreamReader(System.in));
			    
			    String line;
			    while ((line = fromServer.readLine()) != null) {
			    	System.out.println("Server: "+line);
			    	if (line.equals("Bye.")) 
			    		break;
			    	String userText = stdIn.readLine();
			        if (userText != null) {
			            System.out.println("Me: "+userText);
			            toServer.write((userText+"\n").getBytes());
			        }
			    }
		} finally {
			System.out.println();
		}
	}
	
	public void write(String message) throws IOException {
		clientOut.write(message.getBytes());
	}
	
	public String getMessages() throws IOException {
		return reader.readLine();
	}
	
	public boolean connect() {
		try {
			client = new Socket("localhost", 5000);
			clientOut = client.getOutputStream();
			clientIn = client.getInputStream();
			reader = new BufferedReader(new InputStreamReader(clientIn));
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}

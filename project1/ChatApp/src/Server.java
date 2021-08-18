import java.io.IOException;
import java.net.*;
import java.util.*;

public class Server {

	int port;
	ServerSocket socket;
	ArrayList<Worker> workers = new ArrayList<Worker>();
	
	public Server(int port) {
		try {
			this.socket = new ServerSocket(port);
			System.out.println("Server started...");
			
			while (true) {
				
				Socket clientSocket = socket.accept();
				System.out.printf("%s connected to the server!\n", clientSocket);
				Worker worker = new Worker(this, clientSocket);
				workers.add(worker);
				worker.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void broadcast(String username, String message) throws IOException {
		String newMessage = "BROADCAST FROM "+username+": "+message;
		for (Worker worker: workers) {
			if (!worker.username.equals(username) ) {
				worker.send(newMessage);
			}
		}
	}
	
}

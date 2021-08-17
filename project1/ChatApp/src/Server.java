import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server extends ServerMain {
	
	ServerSocket serverSocket;
	ArrayList<ServerWorker> workers = new ArrayList();
	
	public Server(int port) {

		try {
			this.serverSocket = new ServerSocket(port);
			System.out.println("Server started...\n");
			while (true) {
				Socket clientSocket = serverSocket.accept();
				System.out.println("Accepted an incoming connection from "+clientSocket);
				ServerWorker worker = new ServerWorker(this, clientSocket);
				workers.add(worker);
				worker.start();
			}
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	public void userDisconnected(String user) {
		System.out.printf("%s disconected\n", user);
		
//		This causes and error for some reason
//		for (ServerWorker worker: workers) {
//			try {
//				worker.sendMessageToClient(user + " has disconnected!\n");
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
		
	}
	
	public void broadcast(String message) throws IOException {
		for (ServerWorker worker: workers) {
			worker.sendMessageToClient(message);
		}
	}
	
	public void broadcastUsers() throws IOException {
		String users = "";
		for (ServerWorker worker: workers) {
			users = users + worker.clientName+"\n";
		}
		users += "Are all logged in.\n\n";
		for (ServerWorker worker: workers) {
			worker.sendMessageToClient(users);
		}
	}
	
	public void sendMessage(String user, String message) {
		for (ServerWorker worker: workers) {
			if (worker.clientName.equals(user)) {
				try {
					worker.sendMessageToClient(message);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

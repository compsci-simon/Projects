import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Server {

	int port;
	ServerSocket socket;
	ArrayList<Worker> workers = new ArrayList<Worker>();
	Lock lock = new ReentrantLock();
	int workerid = 0;
	
	public Server(int port) {
		try {
			this.socket = new ServerSocket(port);
			System.out.println("Server started...");
			
			while (true) {
				
				Socket clientSocket = socket.accept();
				System.out.printf("%s connected to the server!\n", clientSocket);
				Worker worker = new Worker(this, clientSocket, workerid++);
				workers.add(worker);
				worker.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void logoff(int id) {
		lock.lock();
		Worker toRemove = null;
		try {
			for (Worker worker: workers) {
				if (worker.id == id) {
					toRemove = worker;
					break;
				}
			}
			if (toRemove != null) {
				workers.remove(toRemove);
			}
		} finally {
			lock.unlock();
		}
	}
	
	public String sendMessage(String from, String to, String message) throws IOException {
		String returnMessage = "Failure. User does not exist!";
		String newMessage = "DIRECT MESSAGE FROM "+from+": "+message+"\n";
		lock.lock();
		try {
			for (Worker worker: workers) {
				if (worker.username != null) {
					if (worker.username.equals(to)) {
						worker.send(newMessage);
						returnMessage = "Successfully sent message.\n";
						break;
					}
				}
			}
		} finally {
			lock.unlock();
		}
		return returnMessage;
	}
	
	public void broadcast(String username, String message) throws IOException {
		String newMessage = "BROADCAST FROM "+username+": "+message+"\n";
		lock.lock();
		try {
			for (Worker worker: workers) {
				if (worker.username != null) {
					if (!worker.username.equals(username) ) {
						worker.send(newMessage);
					}
				}
			}
		} finally {
			lock.unlock();
		}
	}
	
	public String listUsers(String username) {
		String users = "";
		lock.lock();
		try {
			for (Worker worker: workers) {
				if (worker.username != null) {
					if (!worker.username.equals(username) ) {
						users += worker.username + " ";
					}
				}
			}
		} finally {
			lock.unlock();
		}
		return users;
	}
	
}

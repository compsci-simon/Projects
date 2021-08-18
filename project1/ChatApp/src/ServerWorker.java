import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class ServerWorker extends Thread {

	Socket clientSocket;
	String clientName = null;
	Server server = null;
	OutputStream outputStream;
	InputStream inputStream;
	
	public ServerWorker(Server server, Socket clientSocket) throws IOException {
		this.clientSocket = clientSocket;
		this.server = server;
		this.outputStream = clientSocket.getOutputStream();
		this.inputStream = clientSocket.getInputStream();
	}
	
	@Override
	public void run() {
		try {
			handle_client(clientSocket);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void handle_client(Socket clientSocket) throws IOException, InterruptedException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		
		String line;

		while ((line = reader.readLine()) != null) {
			if ("quit".equalsIgnoreCase(line)) {
				outputStream.write("Successfully logged off.\n".getBytes());
				break;
			} else if ("help".equalsIgnoreCase(line)) {
				String msg = "\nmsg: Send a message to someone\n"
							+ "list: List all users that you can send a message to\n"
							+ "quit: Exit the chat\n";
				outputStream.write(msg.getBytes());
			} else if ("msg".equalsIgnoreCase(line)) {
				handle_message();
			} else if ("list".equalsIgnoreCase(line)) {
				listUsers();
			} else if ("login".equalsIgnoreCase(line)) {
				handle_login();
			} else {
				outputStream.write("Unrecognized command\n".getBytes());
			}
		}
		clientSocket.close();
	}
		
	private void handle_login() throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		
		String line;
		String username = "";
		String password = "";

		while (true) {
			if (username.equals(password) && !username.isEmpty()) {
				clientName = username;
				outputStream.write("You have successfully logged in!\n".getBytes());
				System.out.printf("%s has logged in!\n", username);
				server.broadcastUsers();
				return;
			} else if (!username.equals(password)) {
				outputStream.write("Incorrect username, password combination! Try again:\n".getBytes());
			} else {
				outputStream.write("Username:\n".getBytes());
			}
			line = reader.readLine();
			username = line;
			
			int i = 0;
			for (; i < server.workers.size(); i++) {
				if (server.workers.get(i).clientName != null) {
					if (server.workers.get(i).clientName.equals(username)) {
						break;
					}
				}
			}
			if (i == server.workers.size()) {
				outputStream.write("Password:\n".getBytes());
				line = reader.readLine();
				password = line;
			} else {
				outputStream.write("Username already in use!\n".getBytes());
			}
		
			
		}
	}

	private void listUsers() throws IOException {
		
		String users = "";
		for (ServerWorker worker: server.workers) {
			users = users + worker.clientName+"\n";
		}
		outputStream.write(users.getBytes());
	}

	private void handle_message() throws IOException {
		String line;
		String userToTalkTo;
		
		outputStream.write("Who do you want to talk to?\n".getBytes());
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		line = reader.readLine();
		userToTalkTo = line;
		
		int i = 0;
		for (; i < server.workers.size(); i++) {
			if (server.workers.get(i).clientName.equalsIgnoreCase(userToTalkTo)) {
				break;
			}
		}
		if (i == server.workers.size()) {
			outputStream.write("No user by that name\n".getBytes());
			return;
		} else {
			outputStream.write("Message\n".getBytes());
			line = reader.readLine();
			String msg = line+"\n";
			server.workers.get(i).sendMessageToClient(msg);
			return;
		}
	}
	
	public void sendMessageToClient(String message) throws IOException {
		outputStream.write(message.getBytes());
	}
}

import java.io.*;
import java.net.*;

public class Worker extends Thread {

	Server server;
	Socket clientSocket;
	OutputStream outStream;
	InputStream inStream;
	String[] commands = {"quit", "help", "login", "bcast", "msg", "list"};
	String username = null;
	int id;

	public Worker(Server server, Socket clientSocket, int id) {
		this.server = server;
		this.clientSocket = clientSocket;
		this.id = id;
	}

	@Override
	public void run() {
		try {
			handle_Socket();
			server.user_logged_off(username);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (username == null) {
				System.out.println("User left the server");
			} else {
				System.out.println(username+" logged off.");
			}
			server.logoff(id);
		}
	}
	
	public void handle_Socket() throws IOException {
		this.outStream = clientSocket.getOutputStream();
		this.inStream = clientSocket.getInputStream();

		outStream.write("Welcome to the server!\n".getBytes());
		BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
		String line;
		boolean loop = true;

		while ((line = reader.readLine()) != null) {
			String[] user_command_tokens = line.split(" ");
			int command_code = -1;
			for (int i = 0; i < commands.length; i++) {
				if (commands[i].equals(user_command_tokens[0])) {
					command_code = i;
					break;
				}
			}
			switch (command_code) {
			case -1:
				outStream.write("Unrecognized command!\n".getBytes());
				break;
			case 0:
				outStream.write("Successfully logged out\n".getBytes());
				loop = false;
				break;
			case 1:
				String msg = "\nCommands ->\n"
						+ "help: All commands\n"
						+ "login <username> <password>: login command\n"
						+ "bcast <message>: Broadcast a message to all users. You need to be logged in\n"
						+ "list: list all online users\n"
						+ "msg <To user> <message>: Sends a direct messge to a user\n"
						+ "quit: leave the chat\n";
				outStream.write(msg.getBytes());
				break;
			case 2:
				if (!handle_login(user_command_tokens)) {
					outStream.write("Login failed!\n".getBytes());
					System.out.println("User failed to log in.");
				} else {
					outStream.write("Login succesfull!\n".getBytes());
					server.user_joined(username);
					System.out.println(username + " has succesfully logged in.");
				}
				break;
			case 3:
				if (handle_broadcast(user_command_tokens)) {
					outStream.write("Success broadcasting message!\n".getBytes());
				} else {
					outStream.write("Failure broadcasting message!\n".getBytes());
				}
				break;
			case 4:
				String res = handle_message(user_command_tokens);
				outStream.write(res.getBytes());
				break;
			case 5:
				handle_list();
				break;
			}
			if (!loop) {
				break;
			}
		}
		clientSocket.close();
	}
	
	public void handle_list() {
		String list = server.listUsers(username) + "\n";
		try {
			outStream.write(list.getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String handle_message(String[] user_command_tokens) {
		if (username == null) {
			return "Failed to send message. You must be logged in.";
		} else if (user_command_tokens.length < 2) {
			return "Failed to send message. Empty mesage.\n";
		} else {
			String msg = "";
			for (int i = 2; i < user_command_tokens.length; i++) {
				msg += user_command_tokens[i]+" ";
			}
			String returnMessage = "";
			try {
				returnMessage = server.sendMessage(username, user_command_tokens[1], msg);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return returnMessage;
		}
	}

	private boolean handle_broadcast(String[] user_command_tokens) {
		System.out.println(username);
		if (username == null) {
			return false;
		} else if (user_command_tokens.length < 2) {
			return false;
		} else {
			try {
				String msg = "";
				for (int i = 1; i < user_command_tokens.length; i++) {
					msg += user_command_tokens[i]+" ";
				}
				server.broadcast(username, msg);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	private boolean handle_login(String[] user_command_token) {
		if (user_command_token.length != 3) {
			return false;
		} else if (!user_command_token[1].equals(user_command_token[2])) {
			return false;
		} else {
			if (server.userLoggedIn(user_command_token[1])) {
				return false;
			}
			username = user_command_token[1];
			return true;
		}
	}
	
	public void send(String message) throws IOException {
		outStream.write(message.getBytes());
	}
}

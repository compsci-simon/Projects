import java.io.*;
import java.net.*;

public class Worker extends Thread {

	Server server;
	Socket clientSocket;
	OutputStream outStream;
	InputStream inStream;
	String[] commands = {"quit", "help", "login"};
	String username = null;

	public Worker(Server server, Socket clientSocket) {
		this.server = server;
		this.clientSocket = clientSocket;
	}

	@Override
	public void run() {
		try {
			handle_Socket();
		} catch (Exception e) {
			e.printStackTrace();
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
			String[] user_command_token = line.split(" ");
			int command_code = -1;
			for (int i = 0; i < commands.length; i++) {
				if (commands[i].equals(user_command_token[0])) {
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
				if (username == null) {
					System.out.println("User left the server");
				} else {
					System.out.println(username+" logged off.");
				}
				loop = false;
				break;
			case 1:
				String msg = "\nCommands ->\n"
						+ "help: All commands\n"
						+ "login <username> <password>: login command\n"
						+ "quit: leave the chat\n";
				outStream.write(msg.getBytes());
				break;
			case 2:
				if (!handle_login(user_command_token)) {
					outStream.write("Login failed!\n".getBytes());
					System.out.println("User failed to log in.");
				} else {
					outStream.write("Login succesfull!\n".getBytes());
					System.out.println(username + " has succesfully logged in.");
				}
				break;
			}
			if (!loop) {
				break;
			}
		}
		clientSocket.close();
	}

	private boolean handle_login(String[] user_command_token) {
		if (user_command_token.length != 3) {
			return false;
		} else if (!user_command_token[1].equals(user_command_token[2])) {
			return false;
		} else {
			username = user_command_token[1];
			return true;
		}
	}
	
	public void send(String message) throws IOException {
		outStream.write(message.getBytes());
	}
}

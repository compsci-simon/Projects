import java.io.*;
import java.net.*;

public class Worker extends Thread {

	Server server;
	Socket clientSocket;
	OutputStream outStream;
	InputStream inStream;

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

		while ((line = reader.readLine()) != null) {
			if (line.equals("quit")) {
				outStream.write("Succesfully logged off.\n".getBytes());
				System.out.println("User logged off.");
				break;
			} else {
				outStream.write("Unrecognized command!\n".getBytes());
			}
		}
		clientSocket.close();
	}
}

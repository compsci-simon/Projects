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
			this.outStream = clientSocket.getOutputStream();
			this.inStream = clientSocket.getInputStream();

			outStream.write("Welcome to the server!\n".getBytes());
			BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
			String line;

			while ((line = reader.readLine()) != null) {
				if (line.equals("quit")) {
					outStream.write("Succesfully logged off.".getBytes());
					clientSocket.close();
				} else {
					outStream.write("Unrecognized command!\n".getBytes());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

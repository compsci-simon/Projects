import java.io.IOException;
import java.net.*;

public class Server {

	int port;
	ServerSocket socket;
	
	public void Server(int port) {
		try {
			this.socket = new ServerSocket(port);
			System.out.println("Server started...");
			
			while (true) {
				
				Socket clientSocket = socket.accept();
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}

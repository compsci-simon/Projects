import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class Client {
	String server;
	int port;
	private Socket socket;
	OutputStream serverOut;
	InputStream serverIn;
	private BufferedReader bufferedIn;
	
	public Client(String server, int port) {
		this.server = server;
		this.port = port;
	}
	
	public boolean connectToServer() {
		try {
			this.socket = new Socket(this.server, this.port);
			this.serverOut = socket.getOutputStream();
			this.serverIn = socket.getInputStream();
			this.bufferedIn = new BufferedReader(new InputStreamReader(serverIn));
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean login(String username, String password) throws IOException{
		String response = bufferedIn.readLine();
		serverOut.write(username.getBytes());
		return true;
	}
}

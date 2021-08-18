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
		BufferedReader reader = new BufferedReader(new InputStreamReader(serverIn));
		String line;
		line = reader.readLine();
		System.out.println(line);
		String login_string = "login " + username + " " + password;
		System.out.println(login_string);
		serverOut.write(login_string.getBytes());
		System.out.println("hey");
		line = reader.readLine();
		System.out.println(line);
		return true;
	}
}

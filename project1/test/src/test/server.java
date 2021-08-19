package test;

import java.io.*;
import java.net.*;

public class server {
	public static void main(String[] args) throws IOException {
		try {
		    ServerSocket sock = new ServerSocket(5000);       // 1st statement
		    Socket client = sock.accept();
		    
		    OutputStream out = (client.getOutputStream());
		    BufferedReader in =                                          // 3rd statement 
		        new BufferedReader(
		            new InputStreamReader(client.getInputStream()));
		    BufferedReader stdIn =                                       // 4th statement 
		        new BufferedReader(
		            new InputStreamReader(System.in));
		    
		    out.write("Hello\n".getBytes());
		    String line;
		    while ((line = in.readLine()) != null) {
		    	System.out.println(line);
		    	out.write(("You said: nothing\n").getBytes());
		    	if (line.equals("Bye.")) 
		    		break;
		    }
//		    String userInput;
//		    while ((userInput = stdIn.readLine()) != null) {
//		        out.println(userInput);
//		        System.out.println("echo: " + in.readLine());
//		    }
	} finally {
		System.out.println("here");
	}
	}
}

package cust;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.ArrayList;

public class Server {
  DatagramSocket udpSock;
  ServerSocket serverSock;
  Socket tcpSock;
  OutputStream tcpOut;
  BufferedReader tcpIn;
  int udpPort;
  int tcpPort;
  int MAXPACKETSIZE = 32000;

  public Server(int udpPort, int tcpPort) throws Exception {
    this.udpPort = udpPort;
    this.tcpPort = tcpPort;
  }

  public static void main (String[] args) throws Exception {
    Server s = new Server(5555, 5556);
    s.accept_tcp_connection();
    String msg = s.tcpReceive();
    s.closeTcp();
    System.out.println(msg);
  }

  public void accept_tcp_connection() {
    try {
      serverSock = new ServerSocket(tcpPort);
      tcpSock = serverSock.accept();
      tcpOut = tcpSock.getOutputStream();
      tcpIn = new BufferedReader(new InputStreamReader(tcpSock.getInputStream()));
    } catch (Exception e) {
      System.exit(0);
    }
  }

  public void closeTcp() throws Exception {
    tcpSock.close();
  }

  public String tcpReceive() {
    String message = null;
    try {
      message = tcpIn.readLine();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return message;
  }

  public void writeBytesToPath(byte[] bytes, String path) throws Exception {
    Path path1 = Paths.get("/Users/simon/Developer/git_repos/Projects/project2/fileTransfer/assets/file2.dmg");
        
    Files.write(path1, bytes);
        
  }

  public void writeFile(byte[] fileBytes, String path) throws Exception {
    Path newPath = Paths.get(path);
    Files.write(newPath, fileBytes);
  }
}
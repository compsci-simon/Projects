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

  public void handle_tcp_connections() {
    
    try {
      serverSock = new ServerSocket(tcpPort);
      tcpSock = serverSock.accept();
      tcpOut = tcpSock.getOutputStream();
      tcpIn = new BufferedReader(new InputStreamReader(tcpSock.getInputStream()));
      String in;
      char[] buff = new char[1024];
      while (true) {
        tcpIn.read(buff);
        String line = buff.toString();
        if (line.equals("quit"))
          break;
        System.out.println(line);
      }
    } catch (Exception e) {
      System.exit(0);
    }
  }

  public void receive() {
    
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
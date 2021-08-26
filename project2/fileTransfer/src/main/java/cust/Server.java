package cust;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.ArrayList;

/*
 * The server or receiver of the file. Maintains a tcp connection with
 * the client in order to reliably send and receive synchronization and
 * meta data.
 */
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
    System.out.println("Waiting for tcp connection");
    s.acceptTcpConnection();
    s.rbudpRecv();
    s.closeTcp();
  }

  public void rbudpRecv () throws Exception {
    if (tcpSock == null) {
      System.err.println("You first need to establish a tcp connection to use this function.");
      return;
    }
    String metadata = tcpIn.readLine();
    int fileSize = Integer.parseInt(metadata.split(" ")[0]);
    int packetSize = Integer.parseInt(metadata.split(" ")[1]);
    int blastlength = Integer.parseInt(metadata.split(" ")[2]);
    System.out.printf("fileSize = %d, packetSize = %d, blastlength = %d\n", fileSize, packetSize, blastlength);
    tcpOut.write("1\n".getBytes());
  }

  /*
   * This method simply accepts a tcp connection which can then be utilized at a later stage by sending
   * and receiving messages over the connection.
   */
  public void acceptTcpConnection() {
    try {
      serverSock = new ServerSocket(tcpPort);
      tcpSock = serverSock.accept();
      tcpOut = tcpSock.getOutputStream();
      tcpIn = new BufferedReader(new InputStreamReader(tcpSock.getInputStream()));
    } catch (Exception e) {
      System.exit(0);
    }
  }

  /*
   * Closes the tcp connection with the client.
   */
  public void closeTcp() throws Exception {
    tcpSock.close();
  }

  /*
   * Receives metadata and synchronization data to ensure reliability of
   * the datagram packets that have been sent.
   */
  public String tcpReceive() {
    String message = null;
    try {
      message = tcpIn.readLine();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return message;
  }

  /*
   * Used when the file has been received by rbudpRecv to write the file to the servers
   * filesystem.
   */
  public void writeFile(byte[] fileBytes, String path) throws Exception {
    Path newPath = Paths.get(path);
    Files.write(newPath, fileBytes);
  }
}
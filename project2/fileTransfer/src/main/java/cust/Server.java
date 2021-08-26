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

  public Server(int udpPort, int tcpPort) throws Exception {
    this.udpPort = udpPort;
    this.tcpPort = tcpPort;
    udpSock = new DatagramSocket(udpPort);
    udpSock.setSoTimeout(200);
  }

  public static void main (String[] args) throws Exception {
    Server s = new Server(5555, 5556);
    System.out.println("Waiting for tcp connection");
    s.acceptTcpConnection();
    System.out.println("Received connection");
    s.rbudpRecv();
    s.closeTcp();
  }

  public void rbudpRecv () throws Exception {
    if (tcpSock == null) {
      System.err.println("You first need to establish a tcp connection to use this function.");
      return;
    }
    String metadata = tcpReceive();
    int fileSize = Integer.parseInt(metadata.split(" ")[0]);
    int packetSize = Integer.parseInt(metadata.split(" ")[1]);
    int dataSize = packetSize - 1;
    int blastlength = Integer.parseInt(metadata.split(" ")[2]);
    System.out.printf("fileSize = %d, packetSize = %d, blastlength = %d\n", fileSize, packetSize, blastlength);

    tcpAck();

    String fromTo = tcpReceive();
    int from = Integer.parseInt(fromTo.split(" ")[0]);
    int to = Integer.parseInt(fromTo.split(" ")[1]);
    System.out.println(from+" "+to);

    tcpAck();

    byte[] output = new byte[dataSize*blastlength];
    byte[] packetbuffer = new byte[packetSize];
    String packetsReceived = "";
    for (int i = 0; i < blastlength; i++) {
      packetbuffer = udpRecv(packetSize);
      if (packetbuffer == null)
        continue;
      int packetNum = packetbuffer[0];
      packetsReceived += packetNum + " ";
      System.out.printf("Received packet %d\n", packetNum);
      System.arraycopy(packetbuffer, 1, output, packetNum*dataSize, dataSize);
    }
    packetsReceived += "\n";
    tcpSend(packetsReceived);
    
    // tcpAck();
    
  }

  /*
   * Used to acknowledge a message was received and communication can continue
   */
  public void tcpAck() {
    tcpSend("1\n");
  }

  public void tcpSend(String message) {
    try {
      tcpOut.write(message.getBytes());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String tcpRecv() {
    try {
      return tcpIn.readLine();
    } catch (Exception e) {
      return null;
    }
  }

  public byte[] udpRecv(int packetsize) {
    byte[] buffer = new byte[packetsize];
    DatagramPacket packet = new DatagramPacket(buffer, packetsize);
    try {
      udpSock.receive(packet);
      return packet.getData();
    } catch (Exception e) {
      return null;
    }
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
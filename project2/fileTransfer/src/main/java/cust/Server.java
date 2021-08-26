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
  int packetsize;
  int payloadsize;
  int blastlength;
  byte[] fileBytes;
  int fileSize;
  static final boolean log = true;

  public Server(int udpPort, int tcpPort) throws Exception {
    this.udpPort = udpPort;
    this.tcpPort = tcpPort;
    udpSock = new DatagramSocket(udpPort);
    udpSock.setSoTimeout(100);
  }

  public static void main (String[] args) throws Exception {
    Server s = new Server(5555, 5556);
    logger("Waiting for tcp connection");
    s.acceptTcpConnection();
    logger("Received connection");
    s.rbudpRecv();
    s.closeTcp();
  }

  /*
   * Used to send files quickly with udp but is also reliable
   */
  public void rbudpRecv () throws Exception {
    if (tcpSock == null) {
      logger(String.format("You first need to establish a tcp connection to use this function."));
      return;
    }
    String metadata = tcpReceive();
    fileSize = Integer.parseInt(metadata.split(" ")[0]);
    fileBytes = new byte[fileSize];
    packetsize = Integer.parseInt(metadata.split(" ")[1]);
    blastlength = Integer.parseInt(metadata.split(" ")[2]);
    logger(String.format("fileSize = %d, packetSize = %d, blastlength = %d%n", fileSize, packetsize, blastlength));

    syn();

    String from;
    int fromPacket;

    from = tcpReceive();
    fromPacket = Integer.parseInt(from);
    receiveBlast(fromPacket);
    logProgress(blastlength, fileSize);

    syn();

    from = tcpReceive();
    fromPacket = Integer.parseInt(from);
    receiveBlast(fromPacket);
    logProgress(blastlength, fileSize);
    
  }

  public void logProgress(int packetNum, int fileSize) {
    logger(String.format("%f%n", (100.0 * packetNum)*payloadsize/fileSize));
  }

  /*
   * Logger to print select statements which can easily be turned on and off.
   */
  public static void logger(String s) {
    if (log) {
      System.out.println(s);
    }
  }

  /*
   * Used to receive a blast of udp packets during the rbudp receive method.
   */
  public void receiveBlast(int fromPacket) {
    
    payloadsize = packetsize - 1;
    byte[] packetbuffer = new byte[packetsize];
    boolean[] packetsReceived = new boolean[blastlength];
    int packetsNotReceived = blastlength;

    
    for (int i = 0; i < blastlength; i++) {
      packetbuffer = udpRecv(packetsize);
      if (packetbuffer == null)
        continue;
      int packetNum = packetbuffer[0];
      logger(String.format("packetNum = %d\n", packetNum));
      packetsReceived[packetNum - fromPacket] = true;
      packetsNotReceived--;
      System.out.printf("Received packet %d\n", packetNum);
      System.arraycopy(packetbuffer, 1, fileBytes, packetNum*payloadsize, payloadsize);
    }

    if (packetsNotReceived > 0) {
      blastRequestPackets(packetsNotReceived, fromPacket, packetsReceived);
    } else {
      tcpSend("\n");
    }
  }

  /*
   * Used when a blast is received but some packets are missing.
   */
  public void blastRequestPackets(int packetsNotReceived, int fromPacket, boolean[] packetsReceived) {
    
    byte[] packetbuffer = new byte[packetsize];
    while (packetsNotReceived > 0) {
      String requestPackets = resendPackets(fromPacket, blastlength, packetsReceived);
      requestPackets += "\n";
      tcpSend(requestPackets);

      for (int i = 0; i < packetsNotReceived; i++) {
        packetbuffer = udpRecv(packetsize);
        if (packetbuffer == null)
          continue;
        int packetNum = packetbuffer[0];
        packetsReceived[packetNum - fromPacket] = true;
        packetsNotReceived--;
        logger(String.format("Received packet %d\n", packetNum));
        System.arraycopy(packetbuffer, 1, fileBytes, packetNum*payloadsize, payloadsize);
      }
    }
    tcpSend("\n");

  }

  /*
   * @param from - Starting range on packets in blast
   * @param to - End range on packets in blast
   * @param packetsReceived - String of all packets that have been received. 
   * Must be space seperated.
   * 
   * 
   * Used to create a string to send to the client for packets to be resent.
   * During rbudp the udp packets may not arrive and the server needs to let 
   * the client know which packets have not arrived. This method creates a 
   * string of all packets not yet received.
   * 
   * Packet numbers to resend. Not number of packet in loop but number of
   * actual packet number i.e. packet #192 not packet 2
   */
  public String resendPackets(int from, int blastLength, boolean[] packetsReceived) {
    String resend = "";
    for (int i = 0; i < blastLength; i++) {
      if (packetsReceived[i] == false) {
        resend += from + i + " ";
      }
    }
    return resend;
  }

  /*
   * Used to acknowledge a message was received and communication can continue
   */
  public void syn() {
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
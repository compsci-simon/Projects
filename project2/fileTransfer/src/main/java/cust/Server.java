package cust;

import java.io.*;
import java.lang.System.Logger;
import java.net.*;
import java.nio.file.*;
import java.util.ArrayList;
import cust.Utils;
import cust.Packet;

/*
 * The server or receiver of the file. Maintains a tcp connection with
 * the client in order to reliably send and receive synchronization and
 * meta data.
 */
public class Server {
  private DatagramSocket udpSock;
  private ServerSocket serverSock;
  private ServerSocket serverFileSock;
  private Socket tcpSock;
  private Socket tcpFileSock;
  private OutputStream tcpOut;
  private BufferedReader tcpIn;
  private DataOutputStream tcpFileOut;
  private DataInputStream tcpFileIn;
  private int tcpPort;
  private int tcpFilePort;
  private int packetsize;
  private int payloadsize;
  private int udpTimeout = 50;
  private int byteRecvcount = 0;

  public Server(int udpPort, int tcpPort, int tcpFilePort) throws Exception {
    this.tcpPort = tcpPort;
    this.tcpFilePort = tcpFilePort;
    udpSock = new DatagramSocket(udpPort);
    udpSock.setSoTimeout(udpTimeout);
  }

  // **************************************************************************
  // ------------------------------ Main method -------------------------------
  // **************************************************************************
  public static void main (String[] args) throws Exception {
    Server s = new Server(5555, 5556, 5557);
    Utils.logger("Waiting for tcp connection");
    s.acceptTcpConnection();
    Utils.logger("Received connection");
    byte[] fileByte = s.rbudpRecv();
    writeFile(fileByte, "/Users/simon/Developer/git_repos/Projects/project2/fileTransfer/assets/book2.pdf");
    s.closeTcp();

    // byte[] packetBytes = s.udpRecv(64000);
    // if (packetBytes == null)
    //   System.err.println("Weird");
    // Packet p = deserializePacket(packetBytes);
    // System.out.println(p.getBlastNum());

  }

  // **************************************************************************
  // -------------------------- Networking methods ----------------------------
  // **************************************************************************

  /*
   * Used to send files quickly with udp but is also reliable
   */
  public byte[] rbudpRecv () throws Exception {
    if (tcpSock == null) {
      Utils.logger(String.format("You first need to establish a tcp connection to use this function."));
      return null;
    }
    int fileSize;
    int blastlength;
    byte[] file;
    int numBlasts;
    int startPacket;
    int endPacket;

    String metadata = tcpReceive();
    fileSize = Integer.parseInt(metadata.split(" ")[0]);
    packetsize = Integer.parseInt(metadata.split(" ")[1]);
    blastlength = Integer.parseInt(metadata.split(" ")[2]);
    payloadsize = packetsize - Packet.packetBaseSize;
    file = new byte[fileSize];
    Utils.logger(String.format("fileSize = %d, packetSize = %d, blastlength = %d%n", fileSize, packetsize, blastlength));

    syn();

    numBlasts = fileSize / (blastlength * payloadsize);
    Utils.logger(numBlasts*blastlength);

    String fromTo;

    // Main blast
    for (int i = 0; i < numBlasts; i++) {
      fromTo = tcpReceive();
      Utils.logger(String.format("FromTo = %s", fromTo));
      startPacket = Integer.parseInt(fromTo.split(" ")[0]);
      endPacket = Integer.parseInt(fromTo.split(" ")[1]);
      receiveBlast(startPacket, endPacket, file);
      Utils.logProgress(byteRecvcount, fileSize);
    }

    // Partial blast
    fromTo = tcpReceive();
    if (fromTo == null) {
      Utils.logger("Nothings else to receive");
    } else if (fromTo.isEmpty()) {
      Utils.logger("Nothings else to receive");
    } else {
      startPacket = Integer.parseInt(fromTo.split(" ")[0]);
      endPacket = Integer.parseInt(fromTo.split(" ")[1]);
      receiveBlast(startPacket, endPacket, file);
      Utils.logProgress(byteRecvcount, fileSize);
    }
    return file;
  }

  /*
   * Used to receive a blast of udp packets during the rbudp receive method.
   */
  private void receiveBlast(int startPacket, int endPacket, byte[] file) {
    
    byte[] packetBytes = new byte[packetsize];
    int totalPackets = endPacket - startPacket + 1;
    PacketReceiver packetReceiver = new PacketReceiver(totalPackets, startPacket, file, payloadsize);

    
    for (int i = startPacket; i <= endPacket; i++) {
      packetBytes = udpRecv(packetsize);
      if (packetBytes == null)
        continue;
      Packet packet = deserializePacket(packetBytes);
      if (packet == null)
        continue;
      packetReceiver.receivePacket(packet);
      Utils.logger(String.format("Received packet %d", packet.getPacketID()));
    }
    

    if (packetReceiver.missingPackets()) {
      blastRequestPackets(packetReceiver);
    } else {
      tcpSend("\n");
    }
    int bytesAddedToFile = packetReceiver.writePayloadsToFile();
    if (bytesAddedToFile == -1) {
      Utils.logger("Write to file failed");
      System.exit(0);
    }
    byteRecvcount += bytesAddedToFile;
  }

  /*
   * Used when a blast is received but some packets are missing.
   */
  public void blastRequestPackets(PacketReceiver packetReceiver) {
    
    String packetsNotReceived = packetReceiver.failedPackets();
    Utils.logger(String.format("packets not received = %s", packetsNotReceived));
    byte[] packetBytes = new byte[packetsize];
    
    while (packetReceiver.missingPackets()) {
      String requestPackets = packetReceiver.failedPackets();
      requestPackets += "\n";
      tcpSend(requestPackets);

      int numMissingPackets = packetReceiver.numMissingPackets();

      for (int i = 0; i < numMissingPackets; i++) {
        packetBytes = udpRecv(packetsize);
        if (packetBytes == null)
          continue;
        Packet packet = deserializePacket(packetBytes);
        if (packet == null)
          continue;
        packetReceiver.receivePacket(packet);
        Utils.logger(String.format("Received packet %d", packet.getPacketID()));
      }
    }
    tcpSend("\n");

  }

  /*
   * @param fromPacket - Starting range on packets in blast
   * @param toPacket - End range on packets in blast
   * @param packetsReceived - boolean array of all packets that 
   * have been received.
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
  public String packetsToRequest(int fromPacket, boolean[] packetsReceived) {
    String resend = "";
    for (int i = 0; i < packetsReceived.length; i++) {
      if (packetsReceived[i] == false) {
        resend += fromPacket + i + " ";
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
    } catch (java.net.SocketException e) {
      if (e.getMessage().equals("Broken pipe (Write failed)")) {
        System.err.println("Client has closed the connection");
      }
      System.exit(0);
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
   * accepts tcp connection used for transferring files with tcp
   */
  public void acceptFileTcpConnection() {
	    try {
	      serverFileSock = new ServerSocket(tcpFilePort);
	      tcpFileSock = serverFileSock.accept();
	      tcpFileOut = new DataOutputStream(tcpFileSock.getOutputStream());
	      tcpFileIn = new DataInputStream(tcpFileSock.getInputStream());
	    } catch (Exception e) {
	      System.exit(0);
	    }
	  }

  /*
   * Closes the tcp connection with the client.
   */
  private void closeTcp() throws Exception {
    tcpSock.close();
  }
  
  /*
   * Closes the tcpFile connection with the client.
   */
  public void closeTcpFile() throws Exception {
    tcpFileSock.close();
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
   * Receives file using tcp.
   */
  public byte[] tcpReceiveFile() throws IOException {
	  try {
		  int file_length = tcpFileIn.readInt();
		  System.out.println(file_length);
		  byte[] file_contents = new byte[file_length];
		  tcpFileIn.read(file_contents, 0, file_length);
		  return file_contents;
	  } catch(Exception e) {
		  return null;
	  }
  }

  // **************************************************************************
  // ------------------------ Object related methods --------------------------
  // **************************************************************************

  /*
   * Used to deserialize a bytestream into a packet. Packets
   * contain metadata that is critical to the success or rbudp
   */
  private static Packet deserializePacket(byte[] stream) {
    try {
      ByteArrayInputStream bis = new ByteArrayInputStream(stream);
      ObjectInputStream in = new ObjectInputStream(bis);
      return (Packet) in.readObject();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /*
   * Used when the file has been received by rbudpRecv to write the file to the servers
   * filesystem.
   */
  private static void writeFile(byte[] fileBytes, String path) throws Exception {
    Path newPath = Paths.get(path);
    Files.write(newPath, fileBytes);
  }
}

/**
 * This packet receiver object helps with the reliability of the RBUDP.
 * It provides an API for recording received packets as well as producing 
 * the necessary output to request packets be resent from the client.
 */

class PacketReceiver {
  int totalPackets;
  int packZeroID;
  Packet[] packets;
  byte[] file;
  int normalPayloadSize;

  public PacketReceiver(int totalPackets, int packZeroID, byte[] file, int normalPayloadSize) {
    this.totalPackets = totalPackets;
    this.packZeroID = packZeroID;
    packets = new Packet[totalPackets];
    this.file = file;
    this.normalPayloadSize = normalPayloadSize;
  }

  public String failedPackets() {
    String res = "";
    for (int i = 0; i < totalPackets; i++) {
      if (packets[i] == null) {
        res += packZeroID + i + " ";
      }
    }
    return res;
  }

  public void receivePacket(Packet p) {
    packets[p.getBlastNum()] = p;
  }

  public boolean missingPackets() {
    for (int i = 0; i < totalPackets; i++) {
      if (packets[i] == null) {
        return true;
      }
    }
    return false;
  }

  public int numMissingPackets() {
    int count = 0;
    for (int i = 0; i < totalPackets; i++) {
      if (packets[i] == null)
        count++;
    }
    return count;
  }

  public int numPacketsReceived() {
    int count = 0;
    for (Packet p: packets) {
      if (p != null)
        count++;
    }
    return count;
  }

  public int lastPacketSize() {
    return packets[totalPackets-1].getPayload().length;
  }

  public int writePayloadsToFile() {
    int byteRecvCount = 0;
    for (Packet p: packets) {
      if (p == null) {
        System.err.println("We should not be getting a null packet when writing to file.");
        return -1;
      } else {
        try {
          System.arraycopy(p.getPayload(), 0, file, p.getPacketID()*normalPayloadSize, p.getPayload().length);
          byteRecvCount += p.getPayload().length;
        } catch (Exception e) {
          e.printStackTrace();
          return -1;
        }
      }
    }
    return byteRecvCount;
  }
}
package cust;

import java.io.*;
import java.lang.System.Logger;
import java.net.*;
import java.nio.file.*;
import java.util.ArrayList;
import cust.Utils.*;

/*
 * The server or receiver of the file. Maintains a tcp connection with
 * the client in order to reliably send and receive synchronization and
 * meta data.
 */
public class Server {
  DatagramSocket udpSock;
  ServerSocket serverSock;
  ServerSocket serverFileSock;
  Socket tcpSock;
  Socket tcpFileSock;
  OutputStream tcpOut;
  BufferedReader tcpIn;
  DataOutputStream tcpFileOut;
  DataInputStream tcpFileIn;
  int udpPort;
  int tcpPort;
  int tcpFilePort;
  int packetsize;
  int payloadsize;
  int packetIDSize;
  int blastlength;
  byte[] fileBytes;
  int fileSize;
  int udpTimeout = 50;

  public Server(int udpPort, int tcpPort, int tcpFilePort) throws Exception {
    this.udpPort = udpPort;
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
    s.rbudpRecv();
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
  public void rbudpRecv () throws Exception {
    if (tcpSock == null) {
      Utils.logger(String.format("You first need to establish a tcp connection to use this function."));
      return;
    }
    String metadata = tcpReceive();
    fileSize = Integer.parseInt(metadata.split(" ")[0]);
    packetsize = Integer.parseInt(metadata.split(" ")[1]);
    blastlength = Integer.parseInt(metadata.split(" ")[2]);
    payloadsize = packetsize - packetIDSize;
    fileBytes = new byte[fileSize];
    Utils.logger(String.format("fileSize = %d, packetSize = %d, blastlength = %d%n", fileSize, packetsize, blastlength));

    syn();

    int numBlasts = fileSize / (blastlength * payloadsize);
    Utils.logger(numBlasts*blastlength);

    String fromTo;
    int startPacket;
    int endPacket;

    for (int i = 0; i < numBlasts; i++) {
      fromTo = tcpReceive();
      startPacket = Integer.parseInt(fromTo.split(" ")[0]);
      endPacket = Integer.parseInt(fromTo.split(" ")[1]);
      receiveBlast(startPacket, endPacket);
      Utils.logProgress(endPacket, fileSize, payloadsize);
    }

    syn();
    
  }

  /*
   * Used to receive a blast of udp packets during the rbudp receive method.
   */
  public void receiveBlast(int startPacket, int endPacket) {

    Utils.logger(String.format("startpack = %d, endpack = %d", startPacket, endPacket));
    
    byte[] packetBytes = new byte[packetsize];
    int totalPackets = endPacket - startPacket + 1;
    PacketReceiver packetReceiver = new PacketReceiver(totalPackets, startPacket);

    
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
    Utils.logger(String.format("Num packets received = %d", packetReceiver.numPacketsReceived()));
    Utils.logger(String.format("packets missing = %b", packetReceiver.missingPackets()));
    

    if (packetReceiver.missingPackets()) {
      blastRequestPackets(packetReceiver);
    } else {
      tcpSend("\n");
    }
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
  public static Packet deserializePacket(byte[] stream) {
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
  public void writeFile(byte[] fileBytes, String path) throws Exception {
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

  public PacketReceiver(int totalPackets, int packZeroID) {
    this.totalPackets = totalPackets;
    this.packZeroID = packZeroID;
    packets = new Packet[totalPackets];
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
}
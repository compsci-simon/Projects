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
  private DataOutputStream tcpDataOut;
  private DataInputStream tcpDataIn;
  private int tcpPort;
  private int tcpFilePort;
  private int udpTimeout = 50;
  private int totalLoops = 0;
  private double Progress = 0;

  public Server(int udpPort, int tcpPort) throws Exception {
    this.tcpPort = tcpPort;
    this.tcpFilePort = tcpFilePort;
    udpSock = new DatagramSocket(udpPort);
    udpSock.setSoTimeout(udpTimeout);
  }

  // **************************************************************************
  // ------------------------------ Main method -------------------------------
  // **************************************************************************
  public static void main (String[] args) throws Exception {
    String filename = "/Users/simon/Developer/git_repos/Projects/project2/fileTransfer/assets/file2.mov";
    Server s = new Server(5555, 5556);
    Utils.logger("Waiting for tcp connection");
    s.acceptTcpConnection();
    Utils.logger("Received connection");
    byte[] fileByte = s.rbudpRecv();
    if (fileByte == null)
      return;
    writeFile(fileByte, filename);
    s.closeTcp();

    // byte[] file = s.tcpReceiveFile();
    // if (file == null)
    //   return;
    // writeFile(file, filename);

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
    PacketReceiver packetsReceived;
    byte[] file;
    int fileSize;
    int packetSize;
    int payloadSize;
    int totalPackets;
    int blastLength;
    String sendMePackets;

    fileSize = tcpDataIn.readInt();
    packetSize = tcpDataIn.readInt();
    blastLength = tcpDataIn.readInt();
    payloadSize = packetSize - Packet.packetBaseSize;
    totalPackets = (int) Math.ceil(fileSize*1.0/payloadSize);
    file = new byte[fileSize];
    packetsReceived = new PacketReceiver(totalPackets, 0, file, payloadSize);

    while (packetsReceived.missingPackets()) {
      sendMePackets = packetsReceived.getPackets(blastLength).trim() + "\n";
      Utils.logger(String.format("Send me packets %s", sendMePackets));
      tcpSend(sendMePackets);
      receiveBlast(packetsReceived, blastLength);
      Progress = packetsReceived.progress();
      tcpDataOut.writeDouble(Progress);
      Utils.logger(String.format("Progress = %f", Progress));
    }

    packetsReceived.writePayloadsToFile();
    Utils.logger(String.format("Packet success rate = %f", totalPackets*1.0/totalLoops));
    Progress = 0;
    return file;
  }

  /*
   * Used to receive a blast of udp packets during the rbudp receive method.
   */
  private void receiveBlast(PacketReceiver mainPacketReceiver, int blastLength) {
    
    int packetSize = mainPacketReceiver.getPayloadSize() + Packet.packetBaseSize;
    byte[] packetBytes = new byte[packetSize];
    for (int i = 0; i < blastLength; i++) {
      totalLoops++;
      packetBytes = udpRecv(packetSize);
      if (packetBytes == null)
        continue;
      Packet packet = deserializePacket(packetBytes);
      if (packet == null)
        continue;
      mainPacketReceiver.receivePacket(packet);
      Utils.logger(String.format("Received packet %d", packet.getPacketID()));
    }
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
      tcpDataOut = new DataOutputStream(tcpSock.getOutputStream());
      tcpDataIn = new DataInputStream(tcpSock.getInputStream());
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
	      tcpDataOut = new DataOutputStream(tcpFileSock.getOutputStream());
	      tcpDataIn = new DataInputStream(tcpFileSock.getInputStream());
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
      int filesize = tcpDataIn.readInt();
      byte [] mybytearray  = new byte [filesize];
      InputStream is = tcpSock.getInputStream();
      int bytesRead = is.read(mybytearray,0,mybytearray.length);
      int current = bytesRead;
      do {
         bytesRead =
            is.read(mybytearray, current, (mybytearray.length-current));
         if(bytesRead >= 0) current += bytesRead;
      } while(bytesRead > 0);
		  return mybytearray;
	  } catch(Exception e) {
      e.printStackTrace();
		  return null;
	  }
  }
  
  public double ProgressRecv() {
	  return Progress;
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
  public static void writeFile(byte[] fileBytes, String path) throws Exception {
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
  private int totalPackets;
  private int packZeroID;
  private Packet[] packets;
  private byte[] file;
  private int normalPayloadSize;

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
    if (p.getPacketID() < totalPackets) {
      packets[p.getPacketID()] = p;
    }
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

  public void writePayloadsToFile() {
    for (Packet p: packets) {
      if (p == null) {
        System.err.println("We should not be getting a null packet when writing to file.");
      } else {
        try {
          System.arraycopy(p.getPayload(), 0, file, p.getPacketID()*normalPayloadSize, p.getPayload().length);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  public int getTotalPackets () {
    return totalPackets;
  }

  public int getPayloadSize() {
    return normalPayloadSize;
  }

  public String getPackets(int n) {
    String packetsString = "";
    int count = 0;
    for (int i = 0; i < totalPackets && count <= n; i++) {
      if (packets[i] == null) {
        packetsString += i + " ";
        count++;
      }
    }
    return packetsString;
  }

  public double progress() {
    int totalBytesReceived = 0;
    for (Packet p: packets) {
      if (p != null) {
        totalBytesReceived += p.getPayload().length;
      }
    }
    return totalBytesReceived*1.0/file.length;
  }
}
package cust;

import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.io.*;
import cust.Utils.*;
import cust.Packet;

/* 
 *
 * Client that is to send a file using rbudpSend. Maintains a tcp connection
 * with the server while sending datagram packets.
 * 
 */
public class Client {
  private DatagramSocket udpSock;
  private Socket tcpSock;
  private Socket tcpFileSock;
  private OutputStream tcpOutClient;
  private BufferedReader tcpInClient;
  private DataOutputStream tcpDataOutClient;
  private DataInputStream tcpDataInClient;
  private InetAddress hostAddress;
  private int udpPort;
  private int tcpPort;
  private int tcpFilePort;
  private static int packetSize = 10000;
  double progress = 0;
  private String filePath;

  public Client(int udpPort, int tcpPort, String hostAddress) throws Exception {
    this.hostAddress = InetAddress.getByName(hostAddress);
    this.udpPort = udpPort;
    this.tcpPort = tcpPort;
    this.tcpFilePort = tcpFilePort;
    udpSock = new DatagramSocket();
  }

  // **************************************************************************
  // ------------------------------ Main method -------------------------------
  // **************************************************************************
  public static void main(String[] args) {
	  String filePath = "/Users/simon/Developer/git_repos/Projects/project2/fileTransfer/assets/file.mov";
    try {
      Client c = new Client(5555, 5556, "localhost");
      if (!c.tcpConnect()) {
        Utils.logger("Failed to connect");
        return;
      }
      
      Utils.logger("Successfully connected");
      byte[] file;
      file = c.readFileToBytes(filePath);
      final long startTime = System.currentTimeMillis();
      c.rbudpSend(file, packetSize, 30);
      final long endTime = System.currentTimeMillis();
      System.out.println("Total execution time: " + (endTime - startTime)/1000.0 + " seconds");

      // byte[] fileBytes = c.readFileToBytes(filePath);
      // c.tcpFileSend(fileBytes);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  // **************************************************************************
  // -------------------------- Networking methods ----------------------------
  // **************************************************************************

  /*
   * For implementing the entire RBUDP protocol to send the file.
   */
  public void rbudpSend (byte[] message, int packetSize, int blastLength) throws Exception {
    if (!tcpSock.isConnected() || tcpSock.isClosed()) {
      Utils.logger("You must first connect the tcp socket.");
      return;
    }
    int payloadSize = packetSize - Packet.packetBaseSize;
    int totalPackets = (int) Math.ceil(message.length*1.0/payloadSize);
    String packetsToSend;

    SentPackets allPackets = new SentPackets(totalPackets);
    String fileName = getFileName();
    if (fileName == null) {
    	Utils.logger("Filename not yet set.");
    	return;
    }

    // Populating all packet objects
    for (int i = 0; i < totalPackets; i++) {
      Packet p = new Packet(i);
      byte[] payload;
      if (i < totalPackets - 1) {
        payload = new byte[payloadSize];
        System.arraycopy(message, i*payloadSize, payload, 0, payloadSize);
      } else {
        // Different packet size for last packet
        int lastPayloadSize = message.length - i*payloadSize;
        payload = new byte[lastPayloadSize];
        System.arraycopy(message, i*payloadSize, payload, 0, lastPayloadSize);
      }
      p.setPayload(payload);
      allPackets.addPacket(p);
    }

    tcpDataOutClient.writeInt(message.length);
    tcpDataOutClient.writeInt(packetSize);
    tcpDataOutClient.writeInt(blastLength);
    //tcpDataOutClient.writeBytes(fileName);
    tcpDataOutClient.writeUTF(fileName);
    Packet p = new Packet(0);
    p.setPayload(new byte[10]);
    System.out.println(serializePacket(p).length);
    
    while ((packetsToSend = tcpRecv())!= null && !packetsToSend.isEmpty() && !(packetsToSend.compareTo("Done") == 0)) {
      blast(packetsToSend, allPackets);
      progress = tcpDataInClient.readDouble();
      Utils.logger(String.format("Progress = %f", progress));
    }

    Utils.logger("Done");

  }

  public void blast(String packetsToSendString, SentPackets allPackets) {
    Packet[] packetsToSend = allPackets.getPackets(packetsToSendString);
    for (Packet p: packetsToSend) {
      byte[] packetBytes = serializePacket(p);
      try {
        udpSend(packetBytes);
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(0);
      }
    }
  }

  /*
   * For sending a single udp packet. Will be used by rbudpSend when sending the actual 
   * udp packets.
   */
  private void udpSend(byte[] message) throws IOException {
    udpSock = new DatagramSocket();
    DatagramPacket packet = new DatagramPacket(message, message.length, hostAddress, udpPort);
    udpSock.send(packet);
  }

  /*
   * Is used for establishing a tcp connection with the server. The server needs the tcp 
   * connection to send metadata such as the total file size and all synchronization data.
   */
  public boolean tcpConnect() {
    try {
      tcpSock = new Socket(hostAddress, tcpPort);
      tcpSock.setSoTimeout(5000);
      tcpOutClient = tcpSock.getOutputStream();
      tcpInClient = new BufferedReader(new InputStreamReader(tcpSock.getInputStream()));
      tcpDataOutClient = new DataOutputStream(tcpSock.getOutputStream());
      tcpDataInClient = new DataInputStream(tcpSock.getInputStream());
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /*
   * Used to send all metadata and synchronization data to the server because UDP
   * is not realiable or in order of when it was sent.
   */
  public void tcpSend(byte[] message) throws IOException {
    tcpOutClient.write(message);
  }

  public void tcpSend(String message) throws IOException {
    tcpOutClient.write(message.getBytes());
  }
  
  /* Used to send length of file and file with tcp */
  public void tcpFileSend(byte[] file) throws IOException {
    tcpDataOutClient.writeInt(file.length);
    
    OutputStream os = tcpSock.getOutputStream();
    os.write(file,0,file.length);
    os.flush();

  }

  private String tcpRecv() {
    try {
      return tcpInClient.readLine();
    } catch (Exception e) {
    	Utils.logger("Nothing left to read");
    	return null;
    }
  }

  // **************************************************************************
  // ------------------------ Object related methods --------------------------
  // **************************************************************************

  public void setFileName(String filename) {
	  this.filePath = filename;
  }
  
  private String getFileName() {
	  if (filePath == null) {
		  return null;
	  } else {
		  String[] parts = filePath.split("/");
		  return parts[parts.length - 1];
	  }
  }
  /*
   * Used to create and serialize a packet object so that it can be 
   * send in a datagram packet. This is to more easily communicate 
   * metadata in the datagram packet. byte array is payload.length + 121
   */
  private static byte[] serializePacket(Packet packet) {
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(bos);
      oos.writeObject(packet);
      oos.flush();
      return bos.toByteArray();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /*
   * Convert a file to bytes because the rbudpSend needs a message
   * in the bytes format in order to send it.
   */
  public byte[] readFileToBytes(String filePath) throws Exception {
    return Files.readAllBytes(Paths.get(filePath));
  }

}

/**
 * This class is useful to handle the packets that have been sent and also
 * for resending of packets that failed to get to the server.
 */
class SentPackets {
  private Packet[] packets;

  public SentPackets(int totalpackets) {
    packets = new Packet[totalpackets];
  }

  public void addPacket(Packet p) {
    packets[p.getPacketID()] = p;
  }

  public Packet getPacket(int packetID) {
    for (Packet p: packets) {
      if (p != null) {
        if (p.getPacketID() == packetID) {
          return p;
        }
      }
    }
    return null;
  }

  public int sentBytes() {
    int count = 0;
    for (Packet p: packets) {
      if (p != null) {
        count += p.getPayload().length;
      }
    }
    return count;
  }

  public Packet[] getPackets(String packetString) {
    String[] sPackets = packetString.trim().split(" ");
    Packet[] packetsToSend = new Packet[sPackets.length];
    for (int i = 0; i < sPackets.length; i++) {
      packetsToSend[i] = packets[Integer.parseInt(sPackets[i])];
    }
    return packetsToSend;
  }

}
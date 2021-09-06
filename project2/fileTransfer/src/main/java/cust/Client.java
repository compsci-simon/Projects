package cust;

import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
  private Lock lock;
  private DatagramSocket udpSock;
  private Socket tcpSock;
  private OutputStream tcpOutClient;
  private BufferedReader tcpInClient;
  private DataOutputStream tcpDataOutClient;
  private DataInputStream tcpDataInClient;
  private InetAddress hostAddress;
  private String stringHostAddress;
  private int udpPort, tcpPort;
  private double progress;

  public Client(int udpPort, int tcpPort, String hostAddress) throws Exception {
    this.hostAddress = InetAddress.getByName(hostAddress);
    stringHostAddress = hostAddress;
    this.udpPort = udpPort;
    this.tcpPort = tcpPort;
    udpSock = new DatagramSocket();
    this.lock = new ReentrantLock();
  }

  // **************************************************************************
  // ------------------------------ Main method -------------------------------
  // **************************************************************************
  public static void main(String[] args) throws UnknownHostException, IOException {
    String filePath = "/Users/simon/Developer/git_repos/Projects/project2/fileTransfer/assets/book.pdf";
    try {
      Client c = new Client(9004, 9005, "localhost");
      if (!c.tcpConnect()) {
        Utils.logger("Failed to connect");
        return;
      }
      byte[] fileBytes = readFileToBytes(filePath);
      c.tcpFileSendv2(fileBytes, Utils.highest_common_denom(fileBytes.length));
      System.out.println("Done");

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
	  progress = 0;
	  if (!tcpSock.isConnected() || tcpSock.isClosed()) {
      Utils.logger("You must first connect the tcp socket.");
      return;
    }
    int payloadSize = packetSize - Packet.packetBaseSize;
    int totalPackets = (int) Math.ceil(message.length*1.0/payloadSize);
    String packetsToSend;

    SentPackets allPackets = new SentPackets(totalPackets);

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
    
    while ((packetsToSend = tcpRecv())!= null && !packetsToSend.isEmpty()) {
      Utils.logger(String.format("Resending packets %s", packetsToSend));
      blast(packetsToSend, allPackets);
      lock.lock();
      progress = tcpDataInClient.readDouble();
      lock.unlock();
      Utils.logger(String.format("Progress = %f", progress));
    }

    Utils.logger("Done");
    progress = 0;
    
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
      tcpSock = new Socket(stringHostAddress, tcpPort);
      tcpSock.setSoTimeout(10000);
      tcpOutClient = tcpSock.getOutputStream();
      tcpInClient = new BufferedReader(new InputStreamReader(tcpSock.getInputStream()));
      tcpDataOutClient = new DataOutputStream(tcpSock.getOutputStream());
      tcpDataInClient = new DataInputStream(tcpSock.getInputStream());
      return true;
    } catch (Exception e) {
    	e.printStackTrace();
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
    
    tcpOutClient.write(file,0,file.length);
    tcpOutClient.flush();

  }
  
  public void tcpFileSendv2(byte[] file, int blasts) throws IOException {
	  	progress = 0;
	    tcpDataOutClient.writeInt(file.length);
	    tcpDataOutClient.writeInt(blasts);
	    
	    byte[][] fragments = fragmentMessage(file, blasts);
	    for (int i = 0; i < blasts; i++) {
	    	tcpDataOutClient.writeInt(fragments[i].length);
	        tcpOutClient.write(fragments[i], 0, fragments[i].length);
	        tcpOutClient.flush();
	        tcpRecv();
	        progress += 1.0/blasts;
	    }
  }
  
  public byte[][] fragmentMessage(byte[] message, int blasts) {
	  int fragmentSize = (int) Math.ceil(message.length*1.0/blasts);
	  byte[][] fragments = new byte[blasts][];
	  
	  for (int i = 0; i < blasts; i++) {
		  if (message.length - i*fragmentSize < fragmentSize) {
			  fragmentSize = message.length - i*fragmentSize;
		  }
		  fragments[i] = new byte[fragmentSize];
		  Utils.logger(String.format("i = %d, fs = %d", i, fragmentSize));
		  System.arraycopy(message, i*fragmentSize, fragments[i], 0, fragmentSize);
	  }
	  return fragments;
  }

  public String tcpRecv() {
    try {
      return tcpInClient.readLine();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
  

  // **************************************************************************
  // ------------------------ Object related methods --------------------------
  // **************************************************************************

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
  public static byte[] readFileToBytes(String filePath) throws Exception {
    return Files.readAllBytes(Paths.get(filePath));
  }
  
  public double getProgress() {
	  try {
		  lock.lock();
		  return progress;
	  } finally {
		  lock.unlock();
	  }
  }
  
  public void setProgress() {
	  try {
		  lock.lock();
		  progress = 0;
	  } finally {
		  lock.unlock();
	  }
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
    	try {
//    		sPackets[i] = sPackets[i].in
    		packetsToSend[i] = packets[Integer.parseInt(sPackets[i])];
    	} catch (Exception e) {
    		e.printStackTrace();
    		packetsToSend[i] = packets[packets.length - 1];
    	}
    }
    return packetsToSend;
  }

}
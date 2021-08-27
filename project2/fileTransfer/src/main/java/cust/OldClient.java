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
public class OldClient {
  private DatagramSocket udpSock;
  private Socket tcpSock;
  private Socket tcpFileSock;
  private OutputStream tcpOutClient;
  private BufferedReader tcpInClient;
  private DataOutputStream tcpFileOutClient;
  private DataInputStream tcpFileInClient;
  private InetAddress hostAddress;
  private int udpPort;
  private int tcpPort;
  private int tcpFilePort;
  private int packetsize = 10000; // Must be bigger than 121
  private int payloadsize = packetsize - Packet.packetBaseSize;
  private int blastlength = 10;
  private int byteSendCount;
  private final int tcpMaxSize = 500000;
  private static final boolean log = true;

  public OldClient(int udpPort, int tcpPort, String hostAddress) throws Exception {
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
  }
  
  // **************************************************************************
  // -------------------------- Networking methods ----------------------------
  // **************************************************************************

  /*
   * For implementing the entire RBUDP protocol to send the file.
   */
  public void rbudpSend (byte[] message) throws Exception {
    if (!tcpSock.isConnected() || tcpSock.isClosed()) {
      Utils.logger("You must first connect the tcp socket.");
      return;
    }

    byte[] parameters = (message.length + " " + packetsize + " " + blastlength + "\n").getBytes();
    tcpSend(parameters);
    
    recvSyn();

    int numBlasts = message.length / (blastlength * payloadsize);
    int totalPackets = (int) Math.ceil(message.length*1.0/payloadsize);
    Utils.logger(numBlasts*blastlength);
    SentPackets sentPackets = new SentPackets(totalPackets);

    for (int i = 0; i < numBlasts; i++) {
      parameters = (i*blastlength + " " + ((i+1)*blastlength - 1) + "\n").getBytes();
      tcpSend(parameters);
      blast(i*blastlength, (i+1)*blastlength - 1, message, sentPackets);
      
      Utils.logProgress(byteSendCount, message.length);
    }

    // Partial blast
    if (numBlasts*blastlength*payloadsize < message.length) {
      double doublePacketsLeft = (message.length - numBlasts*blastlength*payloadsize)*1.0/payloadsize;
      int numPacketsLeft = (int) Math.ceil(doublePacketsLeft);
      int startPacket = (numBlasts)*blastlength;
      int endPacket = (numBlasts)*blastlength + numPacketsLeft - 1;
      parameters = (startPacket + " " + endPacket + "\n").getBytes();
      tcpSend(parameters);
      int initialPayloadSize = payloadsize;
      blast(startPacket, endPacket, message, sentPackets);
      int bytesSent = (numBlasts + numPacketsLeft - 1)*blastlength*initialPayloadSize + payloadsize;
      Utils.logProgress(byteSendCount, message.length);
    }
    System.out.println("Number of packets sent = "+totalPackets);
    Utils.logger("Done");

  }

  /*
   * Completes one blast which is used in rbudpSend. This
   * method will reliable complete the blast.
   * 
   * @param startPacket - The packet to start the blast at.
   * 
   */
  private void blast(int startPacket, int endPacket, byte[] message, SentPackets sentPackets) throws Exception {
    int frombyte = startPacket*payloadsize;
  
    for (int packetID = startPacket; packetID <= endPacket; packetID++) {
      int blastNum = packetID - startPacket;
      Packet packetObj = new Packet(packetID, blastNum, frombyte);
      if ((packetID+1)*payloadsize > message.length) {
        payloadsize = message.length - packetID*payloadsize;
      }
      byte[] payload = new byte[payloadsize];
      System.arraycopy(message, packetID*payloadsize, payload, 0, payloadsize);
      packetObj.setPayload(payload);
      byte[] packetBytes = serializePacket(packetObj);
      if (packetBytes == null)
      {
        System.err.println("PacketBytes was null");
        continue;
      }
      if (packetBytes.length > packetsize) {
        System.err.printf("packetBytes length = %d which is > packetsize %n", packetBytes.length);
        continue;
      }
      udpSend(packetBytes);
      sentPackets.addPacket(packetObj, packetID);
    }

    String resendPackets;

    while ((resendPackets = tcpRecv().trim()) != null && !resendPackets.isEmpty()) {
      System.out.println("Resend packets "+resendPackets);
      udpResendPackets(resendPackets, message, sentPackets);
      System.out.println("Packets were resent");
    }
    byteSendCount = sentPackets.sentBytes();
  }

  /*
   * Used to resend packets that were not received by the server
   * during an rbudp blast
   */
  private void udpResendPackets(String packets, byte[] message, SentPackets sentPackets) throws Exception {

    String[] resendPackets = packets.split(" ");
    for (int i = 0; i < resendPackets.length; i++) {
      if (resendPackets[i].isEmpty())
        continue;
      int packetID = Integer.parseInt(resendPackets[i]);
      byte[] packetBytes = serializePacket(sentPackets.getPacket(packetID));
      udpSend(packetBytes);
    }
  }

  /*
   * Used to receive an Ack message from the server
   */
  private void recvSyn() {
    String res = null;
    try {
      res = tcpInClient.readLine();
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (Integer.parseInt(res) != 1) {
      System.err.println("Ack did not equal 1");
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
      tcpFileOutClient = new DataOutputStream(tcpSock.getOutputStream());
      tcpFileInClient = new DataInputStream(tcpSock.getInputStream());
      return true;
    } catch (Exception e) {
      return false;
    }
  }
  
  /*
   * Is used for establishing a tcp connection for the purposes of transferring a file with 
   * tcp.
   */
  public boolean tcpFileConnect() {
	    try {
	      tcpFileSock = new Socket(hostAddress, tcpFilePort);
	      tcpFileOutClient = new DataOutputStream(tcpFileSock.getOutputStream());
	      tcpFileInClient = new DataInputStream(tcpFileSock.getInputStream());
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
  
  /* Used to send length of file and file with tcp */
  public void tcpFileSend(byte[] file) throws IOException {
    tcpFileOutClient.writeInt(file.length);
    
    OutputStream os = tcpSock.getOutputStream();
    os.write(file,0,file.length);
    os.flush();

  }

  private String tcpRecv() {
    String res = null;
    try {
      res = tcpInClient.readLine();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return res;
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
  public byte[] readFileToBytes(String filePath) throws Exception {
    return Files.readAllBytes(Paths.get(filePath));
  }

}

/**
 * This class is useful to handle the packets that have been sent and also
 * for resending of packets that failed to get to the server.
 */
// class SentPackets {
//   private Packet[] packets;

//   public SentPackets(int totalpackets) {
//     packets = new Packet[totalpackets];
//   }

//   public void addPacket(Packet p, int position) {
//     if (position >= packets.length || position < 0) {
//       System.err.println("Invalid packet position given");
//       return;
//     } else {
//       packets[position] = p;
//     }
//   }

//   public Packet getPacket(int packetID) {
//     for (Packet p: packets) {
//       if (p != null) {
//         if (p.getPacketID() == packetID) {
//           return p;
//         }
//       }
//     }
//     return null;
//   }

//   public int sentBytes() {
//     int count = 0;
//     for (Packet p: packets) {
//       if (p != null) {
//         count += p.getPayload().length;
//       }
//     }
//     return count;
//   }

// }
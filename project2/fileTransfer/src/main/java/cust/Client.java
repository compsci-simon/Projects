package cust;

import java.net.*;
import java.nio.file.*;
import java.util.Arrays;
import java.io.*;

/* 
 *
 * Client that is to send a file using rbudpSend. Maintains a tcp connection
 * with the server while sending datagram packets.
 * 
 */
public class Client {
  DatagramSocket udpSock;
  Socket tcpSock;
  OutputStream tcpOutClient;
  BufferedReader tcpInClient;
  InetAddress hostAddress;
  int udpPort;
  int tcpPort;
  int packetsize = 55001;
  int payloadsize = packetsize - 1;
  int blastLength = 10;
  static final boolean log = true;

  public Client(int udpPort, int tcpPort, String hostAddress) throws Exception {
    this.hostAddress = InetAddress.getByName(hostAddress);
    this.udpPort = udpPort;
    this.tcpPort = tcpPort;
    udpSock = new DatagramSocket();
  }

  /*
   * Just your average main method.
   */
  public static void main(String[] args) {
    try {
      Client c = new Client(5555, 5556, "localhost");
      if (!c.tcpConnect()) {
        logger("Failed to connect");
        return;
      }
      System.out.println("Successfully connected");
      logger("Successfully connected");
      byte[] file = c.readFileToBytes("/Users/simon/Developer/git_repos/Projects/project2/fileTransfer/assets/file.mov");
      c.rbudpSend(file);
    } catch (Exception e) {
      e.printStackTrace();
    }
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
   * For implementing the entire RBUDP protocol to send the file.
   */
  public void rbudpSend (byte[] message) throws Exception {
    if (!tcpSock.isConnected() || tcpSock.isClosed()) {
      logger("You must first connect the tcp socket.");
      return;
    }

    byte[] parameters = (message.length + " " + packetsize + " " + blastLength + "\n").getBytes();
    tcpSend(parameters);
    
    recvSyn();

    parameters = ("0\n").getBytes();
    tcpSend(parameters);
    blast(0, message);

    recvSyn();

    parameters = (blastLength+"\n").getBytes();
    tcpSend(parameters);
    blast(blastLength, message);

    logger("Done");

  }

  /*
   * Completes one blast which is used in rbudpSend. This
   * method will reliable complete the blast.
   * 
   * @param startPacket - The packet to start the blast at.
   * 
   */
  public void blast(int startPacket, byte[] message) throws Exception {
    int frombyte = startPacket*payloadsize;
  
    for (int i = 0; i < blastLength; i++) {
      byte[] packetBuff = new byte[packetsize];
      packetBuff[0] = (byte) (i + startPacket);
      System.arraycopy(message, frombyte + i*(payloadsize), packetBuff, 1, payloadsize);
      udpSend(packetBuff);
    }

    String resendPackets;

    while ((resendPackets = tcpRecv().trim()) != null && !resendPackets.isEmpty()) {
      System.out.println("Resend packets "+resendPackets);
      udpResendPackets(resendPackets, message);
      System.out.println("Packets were resent");
    }
  }

  /*
   * Used to resend packets that were not received by the server
   * during an rbudp blast
   */
  public void udpResendPackets(String packets, byte[] message) throws Exception {

    String[] resendPackets = packets.split(" ");
    for (int i = 0; i < resendPackets.length; i++) {
      if (resendPackets[i].isEmpty())
        continue;
      byte[] packetBuff = new byte[packetsize];
      int packetNum = Integer.parseInt(resendPackets[i]);
      packetBuff[0] = (byte) packetNum;
      System.arraycopy(message, packetNum*payloadsize, packetBuff, 1, payloadsize);
      udpSend(packetBuff);
    }
  }

  /*
   * Used to receive an Ack message from the server
   */
  public void recvSyn() {
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
  public void udpSend(byte[] message) throws IOException {
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

  public String tcpRecv() {
    String res = null;
    try {
      res = tcpInClient.readLine();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return res;
  }

  /*
   * Convert a file to bytes because the rbudpSend needs a message
   * in the bytes format in order to send it.
   */
  public byte[] readFileToBytes(String filePath) throws Exception{
    return Files.readAllBytes(Paths.get(filePath));
  }
  
}

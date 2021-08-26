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
  int packetsize = 10001;
  int dataSize = packetsize - 1;
  int blastLength = 10;

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
        System.out.println("Failed to connect");
        return;
      }
      System.out.println("Successfully connected");
      byte[] file = c.readFileToBytes("/Users/simon/Developer/git_repos/Projects/project2/fileTransfer/assets/file.mov");
      c.rbudpSend(file);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /*
   * For implementing the entire RBUDP protocol to send the file.
   */
  public void rbudpSend (byte[] message) throws Exception {
    if (!tcpSock.isConnected() || tcpSock.isClosed()) {
      System.out.println("You must first connect the tcp socket.");
      return;
    }

    byte[] parameters = (message.length + " " + packetsize + " " + blastLength + "\n").getBytes();
    tcpSend(parameters);
    
    recvAck();

    parameters = ("0 " + (blastLength - 1) + "\n").getBytes();
    tcpSend(parameters);

    recvAck();
    
    int i = 0;
    for (; i < blastLength; i++) {
      byte[] packetBuff = new byte[packetsize];
      packetBuff[0] = (byte) i;
      System.arraycopy(message, i*(dataSize), packetBuff, 1, dataSize);
      udpSend(packetBuff);
    }
    String resendPackets = tcpRecv();
    while (resendPackets != null) {
      udpResendPackets(resendPackets, message, i);
      System.out.println("Packets were resent");
      resendPackets = tcpRecv();
    }
    System.out.println("Done");

    // recvAck();
  }

  /*
   * Used to resend packets that were not received by the server
   * during an rbudp blast
   */
  public void udpResendPackets(String packets, byte[] message, int from) throws Exception {
    String[] resendPackets = packets.split(" ");
    for (int i = 0; i < resendPackets.length; i++) {
      byte[] packetBuff = new byte[packetsize];
      int packetNum = Integer.parseInt(resendPackets[i]);
      packetBuff[0] = (byte) packetNum;
      System.arraycopy(message, from + i*(dataSize), packetBuff, 1, dataSize);
      udpSend(packetBuff);
    }
  }

  /*
   * Used to receive an Ack message from the server
   */
  public void recvAck() {
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

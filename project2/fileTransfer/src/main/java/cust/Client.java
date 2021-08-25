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
  int MAXPACKETSIZE = 32000;

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
      byte[] message = "151000000 30000".getBytes();
      c.tcpSend(message);
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

    byte[] parameters = (message.length + " " + MAXPACKETSIZE).getBytes();
    tcpOutClient.write(parameters);
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

  /*
   * Convert a file to bytes because the rbudpSend needs a message
   * in the bytes format in order to send it.
   */
  public byte[] readFileToBytes(String filePath) throws Exception{
    return Files.readAllBytes(Paths.get(filePath));
  }
  
}

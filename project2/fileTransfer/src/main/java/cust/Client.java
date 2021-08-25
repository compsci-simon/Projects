package cust;

import java.net.*;
import java.nio.file.*;
import java.util.Arrays;
import java.io.*;

public class Client {
  DatagramSocket udpSock;
  Socket tcpSock;
  OutputStream tcpOutClient;
  BufferedReader tcpInClient;
  InetAddress hostAddress;
  int udpPort;
  int tcpPort;
  int MAXPACKETSIZE = 32000;

  public Client(int udpPort, int tcpPort, InetAddress hostAddress) throws Exception {
    this.hostAddress = hostAddress;
    this.udpPort = udpPort;
    udpSock = new DatagramSocket();
  }

  public void udpSend(byte[] message) throws IOException {
    udpSock = new DatagramSocket();
    DatagramPacket packet = new DatagramPacket(message, message.length, hostAddress, udpPort);
    udpSock.send(packet);
  }

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

  public void tcpSend(byte[] message) throws IOException {
    tcpOutClient.write(message);
  }

  public byte[] readFileToBytes(String filePath) throws Exception{
    return Files.readAllBytes(Paths.get(filePath));
  }
  
}

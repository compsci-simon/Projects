package cust;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.*;
import java.util.Arrays;
import java.io.*;

public class UDPClient {
  DatagramSocket udpSock;
  InetAddress hostAddress;
  int port;
  int MAXPACKETSIZE = 32000;

  public UDPClient(int port, InetAddress hostAddress) throws Exception {
    udpSock = new DatagramSocket();
    this.hostAddress = hostAddress;
    this.port = port;

  }

  public void altSend(byte[] byteMessage) throws Exception {
    DatagramPacket packet = new DatagramPacket(byteMessage, byteMessage.length, hostAddress, port);
    udpSock.send(packet);
  }

  public byte[] readFileToBytes(String filePath) throws Exception{
    return Files.readAllBytes(Paths.get(filePath));
  }

  public void send(byte[] byteMessage) throws Exception {

    if (byteMessage.length > MAXPACKETSIZE) {
      int numIterations = byteMessage.length / MAXPACKETSIZE;
      numIterations += byteMessage.length % MAXPACKETSIZE == 0 ? 0 : 1;
      System.out.println("Client num iter = "+numIterations);
      for (int i = 0; i < numIterations; i++) {
        int from = i * MAXPACKETSIZE;
        int to = from + MAXPACKETSIZE < byteMessage.length ? (i + 1) * MAXPACKETSIZE: byteMessage.length;
        byte[] packetMessage = Arrays.copyOfRange(byteMessage, from, to);
        DatagramPacket packet = new DatagramPacket(packetMessage, packetMessage.length, hostAddress, port);
        udpSock.send(packet);
      }
    } else {
      DatagramPacket packet = new DatagramPacket(byteMessage, byteMessage.length, hostAddress, port);
      udpSock.send(packet);
    }
  }

  public void recv() throws Exception {

    byte[] buffer = new byte[1024];
    DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
    udpSock.receive(dp);
    String message = new String(dp.getData());
    System.out.println("Client" + message);
  }
  
}

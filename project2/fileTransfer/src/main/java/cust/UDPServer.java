package cust;

import java.io.IOException;
import java.net.*;
import java.nio.file.*;
import java.util.ArrayList;

public class UDPServer {
  DatagramSocket udpSock;
  int port;
  int MAXPACKETSIZE = 32000;

  public UDPServer(int port) throws Exception {
      udpSock = new DatagramSocket(port);
      this.port = port;
  }

  public byte[] recv(int messageSize) throws Exception {
    int numIterations = 0;
    byte[] byteStream = null;
    if (messageSize > MAXPACKETSIZE) {
      byteStream = new byte[messageSize];
      numIterations = messageSize / MAXPACKETSIZE;
      numIterations += messageSize % MAXPACKETSIZE == 0 ? 0 : 1;
      numIterations -= 5;
      numIterations = 50;
      System.out.println("Server num iter = "+numIterations);
      for (int i = 0; i < numIterations; i++) {
        int size = (i+1) * MAXPACKETSIZE < messageSize ? MAXPACKETSIZE: (i+1)*MAXPACKETSIZE-messageSize;
        byte[] tempBuffer = new byte[size];
        DatagramPacket dp = new DatagramPacket(tempBuffer, tempBuffer.length);
        udpSock.receive(dp);
        byte[] tdata = dp.getData();
        System.out.println(i * MAXPACKETSIZE + tdata.length);
        for (int j = 0; j < tdata.length; j++) {
          byteStream[i * MAXPACKETSIZE + j] = tdata[j];
        }
      }
    } else {
      byte[] buffer = new byte[messageSize];
      DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
      udpSock.receive(dp);
      byteStream = dp.getData();
    }
    return byteStream;
  }

  public String recv() throws Exception {
    byte[] buffer = new byte[1024];
    DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
    udpSock.receive(dp);
    return new String(dp.getData());
  }

  public byte[] altRecv(int size) throws Exception {
    byte[] buffer = new byte[size];
    DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
    udpSock.receive(dp);
    return dp.getData();
  }

  public void writeBytesToPath(byte[] bytes, String path) throws Exception {
    Path path1 = Paths.get("/Users/simon/Developer/git_repos/Projects/project2/fileTransfer/assets/file2.dmg");
        
    Files.write(path1, bytes);
        
  }

  public void send(byte[] byteMessage, InetAddress hostAddress) throws Exception {

    DatagramPacket packet = new DatagramPacket(byteMessage, byteMessage.length, hostAddress, port);
    udpSock.send(packet);
  }

  public void writeFile(byte[] fileBytes, String path) throws Exception {
    Path newPath = Paths.get(path);
    Files.write(newPath, fileBytes);
  }
}
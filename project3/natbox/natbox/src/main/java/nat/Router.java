package nat;

import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Router {
  private DatagramSocket serverSock;
  private DatagramPacket packet;
  private ArrayList<Integer> connectedHosts;
  private DHCPServer dhcpServer;
  private byte[] addressMAC;
  private byte[] addressIP = {(byte) 0xC0, (byte) 0xA8, 0, 1};

  public Router (int portNum) {
    connectedHosts = new ArrayList<Integer>();
    this.addressMAC = generateRandomMAC();
    try {
      this.serverSock = new DatagramSocket(portNum);
      packet = new DatagramPacket(new byte[1500], 1500);
      // dhcpServer = new DHCPServer();
      // dhcpServer.start();
      // addPortToArrayList(67);
      handleConnections();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    Router router = new Router(5000);
  }
  
  private static byte[] generateRandomMAC() {
    byte[] mac = new byte[6];
    for (int i = 0; i < 6; i++) {
      mac[i] = (byte) (Math.random()*0xff);
    }
    return mac;
  }

  private void handleConnections() {
    try {

      while (true) {
        serverSock.receive(packet);
        addPortToArrayList(packet.getPort());
        if (!handleFrame(packet.getData()))
          break;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private boolean handleFrame(byte[] frame) {
    byte[] destAddr = new byte[6];
    System.arraycopy(frame, 0, destAddr, 0, 6);
    for (int i = 0; i < 6; i++) {
      System.out.println(String.format("0x%08x", destAddr[i]&0xff));
    }
    byte[] broadcastMAC = {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
    if (Arrays.equals(addressMAC, destAddr) || Arrays.equals(broadcastMAC, destAddr)) {
      byte[] packet = new byte[frame.length - 14];
      System.arraycopy(frame, 14, packet, 0, packet.length);
      handleIPPacket(packet);
    }
    return true;
  }

  private boolean handleIPPacket(byte[] packet) {
    int protocol = packet[9];
    byte[] destIP = new byte[4];
    byte[] sourceIP = new byte[4];
    System.arraycopy(packet, 12, destIP, 0, 4);
    System.arraycopy(packet, 16, sourceIP, 0, 4);
    System.out.println("Dest IP");
    printIP(destIP);
    System.out.println("Source IP");
    printIP(sourceIP);
    return false;
  }

  public static void printIP(byte[] ipaddr) {
    if (ipaddr.length != 4)
      return;
    String ip = String.format("%d.%d.%d.%d", (int) (ipaddr[0]&0xff), (int) (ipaddr[1]&0xff), (int) (ipaddr[2]&0xff), (int) (ipaddr[3]&0xff));
    System.out.println(ip);
  }

  private void broadcast(byte[] frame) {
    for (int i = 0; i < connectedHosts.size(); i++) {
      this.packet.setData(frame);
      this.packet.setPort(connectedHosts.get(i));
      try {
        this.serverSock.send(this.packet);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void addPortToArrayList(int port) {
    for (int i = 0; i < connectedHosts.size(); i++) {
      if (connectedHosts.get(i) == port) {
        return;
      }
    }
    connectedHosts.add(port);
  }
}

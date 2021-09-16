package nat;

import java.net.*;
import java.util.ArrayList;

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
    // if (addressMAC == destAddr || Long.decode("0xffffffffffff") == destAddr) {
    //   byte[] packet = new byte[frame.length - 14];
    //   System.arraycopy(frame, 14, packet, 0, packet.length);
    //   handlePacket(packet);
    // }
    return true;
  }

  private boolean handlePacket(byte[] packet) {
    int protocol = packet[9];
    int destAddr = 0;
    for (int i = 0; i < 4; i++) {
      destAddr = destAddr<<8;
      destAddr = destAddr | 0xFF&packet[12+i];
    }
    return false;
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

package nat;

import java.net.*;
import java.util.ArrayList;

public class Router {
  private DatagramSocket serverSock;
  private DatagramPacket packet;
  private ArrayList<Integer> connectedHosts;
  private DHCPServer dhcpServer;
  private long addressMAC;

  public Router (int portNum) {
    connectedHosts = new ArrayList<Integer>();
    this.addressMAC = generateRandomMAC();
    try {
      this.serverSock = new DatagramSocket(portNum);
      packet = new DatagramPacket(new byte[1500], 1500);
      dhcpServer = new DHCPServer();
      dhcpServer.start();
      addPortToArrayList(67);
      handleConnections();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    Router router = new Router(5000);
  }

  private static long generateRandomMAC() {
    long mac = 0;
    for (int i = 0; i < 6; i++) {
      mac = mac<<8;
      mac = mac | (int) (Math.random()*0xFF);
    }
    return mac;
  }

  private void handleConnections() {
    try {

      while (true) {
        serverSock.receive(packet);
        addPortToArrayList(packet.getPort());
        if (!handlePacket(packet.getData()))
          break;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void handleFrame(byte[] frame) {
    broadcast(frame);
    long destAddr = 0;
    System.arraycopy(frame, 0, destAddr, 0, 6);
    if ()
  }

  private boolean handlePacket(byte[] packet) {
    if (packet[0] == 1) {
      System.out.println("DHCP request");
      return true;
    } else {
      return false;
    }
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

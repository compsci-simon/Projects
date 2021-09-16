package nat;

import java.net.*;
import java.util.ArrayList;

public class Router {
  private DatagramSocket serverSock;
  private DatagramPacket packet;
  ArrayList<Integer> connectedHosts;
  DHCPServer dhcpServer;

  public Router (int portNum) {
    connectedHosts = new ArrayList<Integer>();
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

  private boolean handlePacket(byte[] packet) {
    if (packet[0] == 1) {
      System.out.println("DHCP request");
      return true;
    } else {
      return false;
    }
  }

  private void broadcast(byte[] packet) {
    for (int i = 0; i < connectedHosts.size(); i++) {
      this.packet.setData(packet);
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

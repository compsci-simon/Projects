package nat;

import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import nat.Constants;

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
      dhcpServer = new DHCPServer();
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
      System.out.println("Router started...");
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
    if (frame.length < 14)
      return true;
    byte[] destAddr = new byte[6];
    System.arraycopy(frame, 0, destAddr, 0, 6);
    byte[] broadcastMAC = {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
    // Router MAC address of broadcast addressed frame are accepted
    if (Arrays.equals(addressMAC, destAddr) || Arrays.equals(Constants.broadcastMAC, destAddr)) {
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
    
    if (Arrays.equals(Constants.broadcastIP, destIP)) {
      broadcastFrame(packet);
      if (protocol == 17) {
        // pass to UDP on router... Dont know what packets the router would receive yet...
        byte[] payload = new byte[packet.length - 20];
        System.arraycopy(packet, 20, payload, 0, payload.length);
        handleUDPPacket(payload);
      }
    } else if (Arrays.equals(addressIP, destIP)) {
      System.out.println("Received packet destined for router");
    }
    return false;
  }

  public void handleUDPPacket(byte[] packet) {
    UDP udpPacket = new UDP(packet);
    if (udpPacket.demuxPort() == Constants.demuxPortDHCP) {
      dhcpServer.processDHCPPacket(udpPacket.payload());
    }
  }

  public static void printIP(byte[] ipaddr) {
    if (ipaddr.length != 4)
      return;
    String ip = String.format("%d.%d.%d.%d", (int) (ipaddr[0]&0xff), (int) (ipaddr[1]&0xff), (int) (ipaddr[2]&0xff), (int) (ipaddr[3]&0xff));
    System.out.println(ip);
  }

  private void broadcastFrame(byte[] frame) {
    for (int i = 0; i < connectedHosts.size(); i++) {
      this.packet.setData(frame);
      this.packet.setPort(connectedHosts.get(i));
      System.out.println("Broadcasting to host on logical port "+this.packet.getPort());
      try {
        this.serverSock.send(this.packet);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void broadcastIP(byte[] packet) {
    System.arraycopy(packet, 12, Constants.broadcastIP, 0, 4);
    System.arraycopy(packet, 16, addressIP, 0, 4);
    byte[] frame = encapsulateEthernet(Constants.broadcastMAC, addressMAC, packet);
    broadcastFrame(frame);
  }

  public byte[] encapsulateEthernet(byte[] destAddr, byte[] sourceAddr, byte[] payload) {
    byte[] header = new byte[14];
    System.arraycopy(sourceAddr, 0, header, 0, 6);
    System.arraycopy(destAddr, 0, header, 6, 6);
    header[12] = (byte) 0x80;
    header[13] = 0x00;
    
    byte[] frame = new byte[14 + payload.length];
    System.arraycopy(header, 0, frame, 0, header.length);
    System.arraycopy(payload, 0, frame, header.length, payload.length);

    return frame;
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

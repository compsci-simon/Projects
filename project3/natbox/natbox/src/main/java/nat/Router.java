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
  private ARP arp;
  private byte[] addressMAC;
  private byte[] addressIP = {(byte) 0xC0, (byte) 0xA8, 0, 1};
  private int packetID;

  public Router (int portNum) {
    packetID = 0;
    connectedHosts = new ArrayList<Integer>();
    this.addressMAC = generateRandomMAC();
    try {
      this.serverSock = new DatagramSocket(portNum);
      packet = new DatagramPacket(new byte[1500], 1500);
      dhcpServer = new DHCPServer(addressIP);
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
      return false;
    Ethernet ethernetFrame = new Ethernet(frame);
    // Router MAC address of broadcast addressed frame are accepted
    System.out.println(ethernetFrame.toString());
    if (Arrays.equals(addressMAC, ethernetFrame.destination())
      || ethernetFrame.isBroadcast()) {
      handleIPPacket(ethernetFrame.payload());
    }
    return true;
  }

  private boolean handleIPPacket(byte[] packet) {
    IP ipPacket = new IP(packet);
    System.out.println(ipPacket.toString());

    if (ipPacket.isBroadcast()) {
      // This is broadcast IP packets
      System.out.println("Broadcast frame");
      Ethernet frame = new Ethernet(Ethernet.broadcastMAC, addressMAC, Ethernet.demuxIP, ipPacket);
      sendFrame(frame);
      if (ipPacket.demuxPort() == 17) {
        // pass to UDP on router... Dont know what packets the router would receive yet...
        handleUDPPacket(ipPacket.payload());
      }
    } else if (Arrays.equals(addressIP, ipPacket.destination())) {
      // Packets destined for the router
      System.out.println("Received packet destined for router");
    } else {
      // Packets that need to be routed
      // Here we forward packets... Externally as well as internally
      // Get MAC address of IP from ARP table
      ipPacket.destination();
    }
    return false;
  }

  public void handleUDPPacket(byte[] packet) {
    UDP udpPacket = new UDP(packet);
    if (udpPacket.demuxPort() == DHCP.serverPort) {
      DHCP dhcpReq = new DHCP(udpPacket.payload());
      byte[] response = dhcpServer.generateResponse(dhcpReq);
      DHCP dhcpResp = new DHCP(response);
      UDP udpPack = new UDP(68, 67, dhcpResp.getBytes());
      IP ipPack = new IP(dhcpReq.getCiaddr(), addressIP, UDP.demuxPort, udpPack.getBytes());
      Ethernet frame = new Ethernet(dhcpReq.getChaddr(), addressMAC, IP.demuxID, ipPack.getBytes(packetID++));
      sendFrame(frame);
    }
  }

  public static void printIP(byte[] ipaddr) {
    if (ipaddr.length != 4)
      return;
    String ip = String.format("%d.%d.%d.%d", (int) (ipaddr[0]&0xff), (int) (ipaddr[1]&0xff), (int) (ipaddr[2]&0xff), (int) (ipaddr[3]&0xff));
    System.out.println(ip);
  }

  private void sendFrame(Ethernet frame) {
    this.packet.setData(frame.getBytes());
    for (int i = 0; i < connectedHosts.size(); i++) {
      this.packet.setPort(connectedHosts.get(i));
      System.out.println("Broadcasting to host on logical port "+this.packet.getPort());
      try {
        this.serverSock.send(this.packet);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
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

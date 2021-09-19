package nat;

import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class Client {
  private boolean internalClient = true;
  private byte[] addressIP = {0x7F, 0, 0, 1};
  private byte[] addressMAC;
  private DatagramSocket socket;
  private DatagramPacket packet;
  private int transactionIdentifier;
  private DHCPClient dhcpClient;
  private int packetCount;
  private static byte[] broadcastMAC = {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
  private static byte[] routerIdentifier;
  private static byte[] subnetMask;
  private static byte[] routerBCAddr;

  public Client(boolean internalClient, String address) {
    try {
      this.socket = new DatagramSocket();
      this.packet = new DatagramPacket(new byte[1500], 1500, InetAddress.getByName(address), 5000);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }
    if (!internalClient) {
      this.addressIP = generateRandomIP();
    }
    this.internalClient = internalClient;
    this.addressMAC = generateRandomMAC();
    this.transactionIdentifier = 0;
    this.dhcpClient = new DHCPClient(addressMAC);
    new Thread() {
      @Override
      public void run() {
        while (true) {
          try {
            socket.receive(packet);
            handleFrame(packet.getData());
            System.out.println("Received a frame");
          } catch (Exception e) {
            //TODO: handle exception
            e.printStackTrace();
          }
        }
      }
    }.start();;
  }

  public static void main(String[] args) {
    Client c = new Client(true, "localhost");
    c.sendDHCPDiscover();
  }

  private static byte[] generateRandomMAC() {
    byte[] mac = new byte[6];
    for (int i = 0; i < 6; i++) {
      mac[i] = (byte) (Math.random()*0xff);
    }
    return mac;
  }

  private static byte[] generateRandomIP() {
    byte[] addressIP = new byte[4];
    for (int i = 0; i < 4; i++) {
      addressIP[i] = (byte) (Math.random()*0xFF);
    }
    return addressIP;
  }

  public void sendFrame(byte[] frame) {
    this.packet.setData(frame);
    try {
      socket.send(this.packet);
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

  private void handleIPPacket(byte[] packet) {
    IP ipPacket = new IP(packet);
    System.out.println(ipPacket.toString());

    if (ipPacket.isBroadcast()) {
      // This is broadcast IP packets
      System.out.println("Broadcast frame");
      Ethernet frame = new Ethernet(Ethernet.broadcastMAC, addressMAC, Ethernet.demuxIP, ipPacket);
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
  }

  public void handleUDPPacket(byte[] packet) {
    UDP udpPacket = new UDP(packet);
    if (udpPacket.demuxPort() == DHCP.clientPort) {
      DHCP dhcpPacket = new DHCP(udpPacket.payload());
      if (dhcpPacket.getMessageType() == DHCP.bootReply) {
        addressIP = dhcpPacket.getCiaddr();
        System.out.println("Here");
      }
    }
  }

  public void sendDHCPDiscover() {
    byte[] packetDHCP = generateDHCPDiscoverPacket();
    byte[] packetUDP = encapsulateUDP(68, 67, packetDHCP);
    byte[] packetIP = encapsulateIP(17, IP.broadcastIP, IP.relayIP, packetUDP);
    byte[] frame = encapsulateEthernet(broadcastMAC, addressMAC, packetIP);
    sendFrame(frame);
  }
  
  public byte[] generateDHCPDiscoverPacket() {
    return DHCP.bootRequest(transactionIdentifier++, addressMAC);
  }

  public byte[] encapsulateEthernet(byte[] destAddr, byte[] sourceAddr, byte[] payload) {
    return new Ethernet(destAddr, sourceAddr, Ethernet.demuxIP, payload).getBytes();
  }

  public byte[] encapsulateIP(int protocol, byte[] destAddr, byte[] sourceAddr, byte[] payload) {
    return new IP(destAddr, sourceAddr, protocol, payload).getBytes(packetCount);
  }

  private byte[] encapsulateUDP(int destPort, int sourcePort, byte[] payload) {
    return new UDP(destPort, sourcePort, payload).getBytes();
  }

}

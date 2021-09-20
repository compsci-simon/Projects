package nat;

import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class Client {
  private byte[] addressIP = {0, 0, 0, 0};
  private byte[] addressMAC;
  private DatagramSocket socket;
  private DatagramPacket packet;
  private DatagramPacket packetRec;
  private ARPTable arpTable;
  private int transactionIdentifier;
  private int packetCount;
  private static byte[] routerIP;
  private static byte[] subnetMask;

  public Client(boolean internalClient, String address) {
    try {
      this.socket = new DatagramSocket();
      this.packet = new DatagramPacket(new byte[1500], 1500, InetAddress.getByName(address), 5000);
      this.packetRec = new DatagramPacket(new byte[1500], 1500, InetAddress.getByName(address), 5000);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }
    if (!internalClient) {
      this.addressIP = generateRandomIP();
    }
    this.addressMAC = generateRandomMAC();
    this.transactionIdentifier = 0;
    arpTable = new ARPTable();
    new Thread() {
      @Override
      public void run() {
        while (true) {
          try {
            socket.receive(packetRec);
            handleFrame(packetRec.getData());
          } catch (Exception e) {
            //TODO: handle exception
            e.printStackTrace();
          }
        }
      }
    }.start();
    if (internalClient) {
      sendDHCPDiscover();
    }
  }

  public static void main(String[] args) {
    Client c = new Client(true, "localhost");
    byte[] testIP = {0, 0, 0, 0};
    boolean ipNotObtained = Arrays.equals(c.addressIP, testIP);
    // this is to make sure the client has received an IP
    while (ipNotObtained) {
    	ipNotObtained = Arrays.equals(c.addressIP, testIP);
    	try {
            Thread.sleep(100);
          } catch (Exception e) {
            //TODO: handle exception
          }
    }
    byte[] ip = {(byte) 0xC0, (byte) 0xA8, 0, 1};
    c.udpSend(ip, "Hello there my friendo");
    c.udpSend(ip, "Another message to my friend");
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
    System.out.println();
    if (ethernetFrame.protocol() == ARP.DEMUX_PORT) {
      handleARPPacket(ethernetFrame.payload());
      return true;
    }
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
      Ethernet frame = new Ethernet(Ethernet.BROADCASTMAC, addressMAC, Ethernet.DEMUXIP, ipPacket);
      if (ipPacket.getDemuxPort() == UDP.DEMUXPORT) {
        // pass to UDP on router... Dont know what packets the router would receive yet...
        handleUDPPacket(ipPacket.payload());
      }
    } else if (Arrays.equals(addressIP, ipPacket.destination()) || Arrays.equals(addressIP, IP.nilIP)) {
      // Packets destined for the router
      if (ipPacket.getDemuxPort() == UDP.DEMUXPORT) {
        handleUDPPacket(ipPacket.payload());
      }
    } else {
      // Packets that need to be routed
      // Here we forward packets... Externally as well as internally
      // Get MAC address of IP from ARP table
      boolean hasIP = arpTable.containsMAC(ipPacket.destination());
    	if (hasIP) {
        byte[] destinationMAC = arpTable.getMAC(ipPacket.destination());
    		/* forward packet to destination */
    	} else {
    		sendRequestARP(ipPacket.destination());
    	}
    }
  }

  public void handleUDPPacket(byte[] packet) {
    UDP udpPacket = new UDP(packet);
    if (udpPacket.destinationPort() == DHCP.CLIENTPORT) {
      handleDHCPPacket(udpPacket.payload());
    }
  }

  public void handleDHCPPacket(byte[] packet) {
    DHCP dhcpPacket = DHCP.deserialize(packet);
    if (dhcpPacket.getMessageType() == DHCP.BOOT_REPLY) {
      addressIP = dhcpPacket.getCiaddr();
      routerIP = dhcpPacket.getGateway();
      System.out.println(toString());
    }
  }

  private void handleARPPacket(byte[] packet) {
      ARP arpPacket = new ARP(packet);
      System.out.println(arpPacket.toString());
      System.out.println();
      
      if (arpPacket.opCode() == 1) {
        if (Arrays.equals(arpPacket.destIP(), addressIP)) {
        	System.out.println("ARP request received");
        	sendResponseARP(arpPacket.srcMAC(), arpPacket.srcIP());
        } else {
        	return;
        }
      } else if (arpPacket.opCode() == 2) {
    	  System.out.println("ARP response received");
    	  arpTable.addPair(arpPacket.srcIP(), arpPacket.srcMAC());
      } else {
    	  System.out.println("Invalid opCode");
      }
  }

  private void sendRequestARP(byte[] destIP) {
    byte[] packet = ARP.createPacketARP(1, addressMAC, addressIP, ARP.zeroMAC, destIP);
    byte[] frame = encapsulateEthernetARP(addressMAC, ARP.broadcastMAC, packet);
    sendFrame(frame);
  }

  private void sendResponseARP(byte[] destMAC, byte[] destIP) {
    byte[] packet = ARP.createPacketARP(2, addressMAC, addressIP, destMAC, destIP);
    byte[] frame = encapsulateEthernetARP(addressMAC, destMAC, packet);
    sendFrame(frame);
  }

  public void sendDHCPDiscover() {
    byte[] packetDHCP = generateDHCPDiscoverPacket();
    byte[] packetUDP = encapsulateUDP(DHCP.SERVERPORT, DHCP.CLIENTPORT, packetDHCP);
    byte[] packetIP = encapsulateIP(IP.UDP_PORT, IP.broadcastIP, IP.relayIP, packetUDP);
    byte[] frame = encapsulateEthernet(Ethernet.BROADCASTMAC, addressMAC, Ethernet.DEMUXIP, packetIP);
    sendFrame(frame);
  }
  
  
  public void udpSend(byte[] ip, String message) {
    UDP udpPacket = new UDP(9000, 9000, message.getBytes());
    IP ipPacket = new IP(ip, addressIP, UDP.DEMUXPORT, udpPacket.getBytes());
    boolean hasIP = arpTable.containsMAC(ipPacket.destination());
    while (!hasIP) {
      System.out.println("Sent ARP request");
      sendRequestARP(ipPacket.destination());
      try {
        Thread.sleep(100);
      } catch (Exception e) {
        //TODO: handle exception
      }
      hasIP = arpTable.containsMAC(ipPacket.destination());
    }
    
    System.out.println(ipPacket.toString());
    byte[] destinationMAC = arpTable.getMAC(ipPacket.destination());
    
    Ethernet ethernetPacket = new Ethernet(destinationMAC, addressMAC, IP.DEMUXPORT, ipPacket.getBytes(packetCount));
    sendFrame(ethernetPacket.getBytes());
    System.out.println("Sent UDP Packet");
  }

  public byte[] generateDHCPDiscoverPacket() {
    return DHCP.bootRequest(transactionIdentifier++, addressMAC).getBytes();
  }

  public byte[] encapsulateEthernet(byte[] destAddr, byte[] sourceAddr, int demuxPort, byte[] payload) {
    return new Ethernet(destAddr, sourceAddr, demuxPort, payload).getBytes();
  }

  public byte[] encapsulateIP(int protocol, byte[] destAddr, byte[] sourceAddr, byte[] payload) {
    return new IP(destAddr, sourceAddr, protocol, payload).getBytes(packetCount);
  }

  private byte[] encapsulateUDP(int destPort, int sourcePort, byte[] payload) {
    return new UDP(destPort, sourcePort, payload).getBytes();
  }

  public byte[] encapsulateEthernetARP(byte[] destAddr, byte[] sourceAddr, byte[] payload) {
    byte[] header = new byte[14];
    System.arraycopy(sourceAddr, 0, header, 0, 6);
    System.arraycopy(destAddr, 0, header, 6, 6);
    header[12] = (byte) 0x08;
    header[13] = 0x06;
    
    byte[] frame = new byte[14 + payload.length];
    System.arraycopy(header, 0, frame, 0, header.length);
    System.arraycopy(payload, 0, frame, header.length, payload.length);

    return frame;
  }

  public String toString() {
    String routerString = "Not set";
    String subnetString = "Not set";
    if (routerIP != null) {
      routerString = IP.ipString(routerIP);
    }
    if (subnetMask != null) {
      subnetString = IP.ipString(subnetMask);
    }
    String s = String.format("Client toString\nMAC = %s\nIP = %s\nGateway = %s" +
                            "\nSubnet Mask = %s", 
      Ethernet.macString(addressMAC), IP.ipString(addressIP), routerString, subnetString);
    return s;
  }

}

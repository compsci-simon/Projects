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
  private int ipIdentifier;
  private int packetCount;
  private static byte[] routerIP;
  private static byte[] subnetMask;
  private byte icmpID;

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
      this.addressIP = IP.generateRandomIP();
    }
    this.addressMAC = Ethernet.generateRandomMAC();
    this.ipIdentifier = 0;
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
    this.icmpID = 0;
    if (internalClient) {
      sendDHCPDiscover();
    }
  }

  public static void main(String[] args) {
    Client c = new Client(true, "localhost");
    try {
      Thread.sleep(200);
    } catch (Exception e) {
      //TODO: handle exception
    }
    byte[] ip = {(byte) 0xC0, (byte) 0xA8, 0, 2};
    if (args.length > 0) {
      try {
        Thread.sleep(500);
        System.out.println("Sending ping");
        c.udpSend(ip, "Howzit my broski");
      } catch (Exception e) {
        //TODO: handle exception
      }
    }
    // c.udpSend(ip, "Another message to my friend");
  }

  public void sendFrame(Ethernet frame) {
    this.packet.setData(frame.getBytes());
    try {
      socket.send(this.packet);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  

  /***************************************************************************/
  /**************************** Handling methods *****************************/
  /***************************************************************************/

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

      switch (ipPacket.getDemuxPort()) {
      case IP.UDP_PORT:
        handleUDPPacket(ipPacket.payload());
        break;
      case IP.ICMP_PORT:
        handleICMPPacket(ipPacket);
        break;
      default:
        System.err.println("Unknown demux port for IP packet");
        break;
      }
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

  public void handleICMPPacket(IP ipPacket) {
    ICMP icmpPacket = new ICMP(ipPacket.payload());
    if (icmpPacket.getType() == ICMP.PING_REQ) {
      ICMP response = ICMP.pingResponse(icmpPacket);
      IP ipPack = new IP(ipPacket.source(), addressIP, ipPacket.getIdentifier(), IP.ICMP_PORT, response.getBytes());
      byte[] destMAC = getMAC(ipPack.destination());
      Ethernet frame = new Ethernet(destMAC, addressMAC, Ethernet.IP_PORT, ipPack.getBytes());
      sendFrame(frame);
    }
  }

  public void handleUDPPacket(byte[] packet) {
    UDP udpPacket = new UDP(packet);
    if (udpPacket.destinationPort() == DHCP.CLIENTPORT) {
      handleDHCPPacket(udpPacket.payload());
    } else {
    	String payload = new String(udpPacket.payload());
    	System.out.println(payload);
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
        	sendResponseARP(arpPacket.srcMAC(), arpPacket.srcIP());
        } else {
        	return;
        }
      } else if (arpPacket.opCode() == 2) {
    	  arpTable.addPair(arpPacket.srcIP(), arpPacket.srcMAC());
      } else {
    	  System.out.println("Invalid opCode");
      }
  }

  private void sendRequestARP(byte[] destIP) {
	  ARP arpPacket = new ARP(1, addressMAC, addressIP, ARP.zeroMAC, destIP);
	  Ethernet frame = new Ethernet(ARP.broadcastMAC, addressMAC, ARP.DEMUX_PORT, arpPacket.getBytes());
	  sendFrame(frame);
  }

  private void sendResponseARP(byte[] destMAC, byte[] destIP) {
	  ARP arpPacket = new ARP(2, addressMAC, addressIP, destMAC, destIP);
    Ethernet frame = new Ethernet(destMAC, addressMAC, Ethernet.ARP_PORT, arpPacket.getBytes());
	  sendFrame(frame);
  }
  
  /***************************************************************************/
  /**************************** Sending methods ******************************/
  /***************************************************************************/

  public void sendDHCPDiscover() {
    byte[] packetDHCP = generateDHCPDiscoverPacket();
    byte[] packetUDP = encapsulateUDP(DHCP.SERVERPORT, DHCP.CLIENTPORT, packetDHCP);
    byte[] packetIP = encapsulateIP(IP.UDP_PORT, IP.broadcastIP, IP.relayIP, packetUDP);
    Ethernet frame = new Ethernet(Ethernet.BROADCASTMAC, addressMAC, Ethernet.IP_PORT, packetIP);
    sendFrame(frame);
  }
  
  public void udpSend(byte[] ip, String message) {
    UDP udpPacket = new UDP(9000, 9000, message.getBytes());
    IP ipPacket = new IP(ip, addressIP, ipIdentifier++, UDP.DEMUXPORT, udpPacket.getBytes());
    System.out.println(arpTable.toString());
    if (IP.sameNetwork(addressIP, ip)) {
      // Do not send frame to router
    
      byte[] destinationMAC = getMAC(ipPacket.destination());
      Ethernet frame = new Ethernet(destinationMAC, addressMAC, Ethernet.IP_PORT, ipPacket.getBytes());
      sendFrame(frame);
    } else {
      // Send frame to router
    }
  }

  public void ping(byte[] ip) {
    ICMP ping = new ICMP(ICMP.PING_REQ, icmpID++, new byte[1]);
    IP ipPacket = new IP(ip, addressIP, ipIdentifier++, IP.ICMP_PORT, ping.getBytes());
    Ethernet frame = new Ethernet(getMAC(ip), addressMAC, Ethernet.IP_PORT, ipPacket.getBytes());
    System.out.println("in ping");
    System.out.println(ipPacket.toString());
    sendFrame(frame);
  }

  public byte[] getMAC(byte[] ip) {

    boolean hasIP = arpTable.containsMAC(ip);
    int i = 0;
    for (; i < 2; i++) {
      if (hasIP)
        break;
      System.out.println("Sent ARP request");
      sendRequestARP(ip);
      try {
        Thread.sleep(100);
      } catch (Exception e) {
        //TODO: handle exception
      }
      hasIP = arpTable.containsMAC(ip);
    }
    if (i == 2) {
      System.out.println(String.format("Could not resolve IP: %s to physical address", IP.ipString(ip)));
      return null;
    }
    return arpTable.getMAC(ip);
  }

  public byte[] generateDHCPDiscoverPacket() {
    return DHCP.bootRequest(ipIdentifier++, addressMAC).getBytes();
  }

  public byte[] encapsulateEthernet(byte[] destAddr, byte[] sourceAddr, int demuxPort, byte[] payload) {
    return new Ethernet(destAddr, sourceAddr, demuxPort, payload).getBytes();
  }

  public byte[] encapsulateIP(int protocol, byte[] destAddr, byte[] sourceAddr, byte[] payload) {
    return new IP(destAddr, sourceAddr, ipIdentifier++, protocol, payload).getBytes();
  }

  private byte[] encapsulateUDP(int destPort, int sourcePort, byte[] payload) {
    return new UDP(destPort, sourcePort, payload).getBytes();
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
    String s = String.format("\nCLIENT toString\n----------------------" + 
      "\nMAC = %s\nIP = %s\nGateway = %s\nSubnet Mask = %s\n", 
      Ethernet.macString(addressMAC), IP.ipString(addressIP), routerString, subnetString);
    return s;
  }

}

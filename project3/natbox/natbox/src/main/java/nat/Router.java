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
  private ARPTable arpTable;
  private byte[] addressMAC;
  private byte[] addressIP = {(byte) 0xC0, (byte) 0xA8, 0, 1};
  private byte[] externalIP;
  private int packetID;
  public static final byte[] subnetMask = {(byte)0xff, (byte)0xff, (byte)0xff, 0};
  public static final byte[] broadcastIP = {(byte) 0xC0, (byte) 0xA8, 0, (byte)0xff};

  public Router (int portNum) {
    packetID = 0;
    connectedHosts = new ArrayList<Integer>();
    this.addressMAC = generateRandomMAC();
    try {
      this.serverSock = new DatagramSocket(portNum);
      packet = new DatagramPacket(new byte[1500], 1500);
      dhcpServer = new DHCPServer(addressIP);
      arpTable = new ARPTable();
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
      if (ethernetFrame.protocol() == Ethernet.DEMUXARP) {
        handleARPPacket(ethernetFrame.payload());
        return true;
      } else if (ethernetFrame.protocol() == Ethernet.DEMUXIP) {
        handleIPPacket(ethernetFrame.payload());
      }
    }
    return true;
  }

  private void handleIPPacket(byte[] packet) {
    IP ipPacket = new IP(packet);
    System.out.println(ipPacket.toString());

    if (ipPacket.isBroadcast()) {
      // This is broadcast IP packets
      Ethernet frame = new Ethernet(Ethernet.BROADCASTMAC, addressMAC, Ethernet.DEMUXIP, ipPacket);
      sendFrame(frame);
      if (ipPacket.getDemuxPort() == 17) {
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
      boolean hasIP = arpTable.containsMAC(ipPacket.destination());
      byte[] destinationMAC;
    	if (hasIP) {
        destinationMAC = arpTable.getMAC(ipPacket.destination());
    		/* forward packet to destination */
    	} else {
    		sendRequestARP(ipPacket.destination());
    	}
      ipPacket.destination();
    }
  }

  public void handleUDPPacket(byte[] packet) {
    UDP udpPacket = new UDP(packet);
    if (udpPacket.demuxPort() == DHCP.SERVERPORT) {
      DHCP dhcpReq = new DHCP(udpPacket.payload());
      DHCP bootReply = dhcpServer.generateBootResponse(dhcpReq);
      try {
        UDP udpPack = new UDP(DHCP.CLIENTPORT, DHCP.SERVERPORT, bootReply.serialize());
        IP ipPack = new IP(dhcpReq.getCiaddr(), addressIP, UDP.DEMUXPORT, udpPack.getBytes());
        Ethernet frame = new Ethernet(bootReply.getChaddr(), addressMAC, IP.DEMUXPORT, ipPack.getBytes(packetID++));
        sendFrame(frame);        
      } catch (Exception e) {
        //TODO: handle exception
        e.printStackTrace();
      }
    }
  }

  private void handleARPPacket(byte[] packet) {
      ARP arpPacket = new ARP(packet);
      System.out.println(arpPacket.toString());
      
      if (arpPacket.opCode() == 1) {
        if (Arrays.equals(arpPacket.destIP(), addressIP)) {
        	System.out.println("ARP request received");
        	sendResponseARP(arpPacket.srcMAC(), arpPacket.srcIP());
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
    sendFrameARP(frame);
  }

  private void sendResponseARP(byte[] destMAC, byte[] destIP) {
    byte[] packet = ARP.createPacketARP(2, addressMAC, addressIP, destMAC, destIP);
    byte[] frame = encapsulateEthernetARP(addressMAC, ARP.broadcastMAC, packet);
    sendFrameARP(frame);
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

  private void sendFrameARP(byte[] frame) {
    this.packet.setData(frame);
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

  private void addPortToArrayList(int port) {
    for (int i = 0; i < connectedHosts.size(); i++) {
      if (connectedHosts.get(i) == port) {
        return;
      }
    }
    connectedHosts.add(port);
  }
}

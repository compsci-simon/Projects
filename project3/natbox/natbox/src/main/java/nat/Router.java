package nat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Router {
  private DatagramSocket[] internalInterfaces;
  private DatagramSocket[] externalInterfaces;
  private DatagramPacket packet;
  private ArrayList<Integer> linkIDs;
  private DHCPServer dhcpServer;
  private ARPTable arpTable;
  private byte[] addressMAC;
  private byte[] addressIP = {(byte) 0xC0, (byte) 0xA8, 0, 1};
  private byte[] externalIP;
  private int packetID;
  public static final byte[] subnetMask = {(byte)0xff, (byte)0xff, (byte)0xff, 0};
  public static final byte[] broadcastIP = {(byte) 0xC0, (byte) 0xA8, 0, (byte)0xff};

  public Router (int[] internalPortNumbers, int[] externalPortNumbers) {
    if (internalPortNumbers.length != 4 && externalPortNumbers.length != 2) {
      return;
    }
    packetID = 0;
    this.internalInterfaces = new DatagramSocket[4];
    linkIDs = new ArrayList<Integer>();
    this.addressMAC = Ethernet.generateRandomMAC();
    this.externalIP = IP.generateRandomIP();
    try {
      for (int i = 0; i < 4; i++) {
        this.internalInterfaces[i] = new DatagramSocket(internalPortNumbers[i]);
      }
      for (int i = 0; i < 2; i++) {
        this.externalInterfaces[i] = new DatagramSocket(externalPortNumbers[i]);
      }
      packet = new DatagramPacket(new byte[1500], 1500);
      dhcpServer = new DHCPServer(addressIP);
      arpTable = new ARPTable();
      new Thread() {
        @Override
        public void run() {
          handleUserInputs();
        }
      }.start();
      handleConnections();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    int[] internalPorts = {1234, 1235, 1236, 1237};
    int[] externalPorts = {1238, 1239};
    new Router(internalPorts, externalPorts);
  }

  private void handleConnections() {
    try {
      System.out.println("Router started...");
      while (true) {
    	  packet = new DatagramPacket(new byte[1500], 1500);
        internalInterface.receive(packet);
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
    if (Arrays.equals(addressMAC, ethernetFrame.destination()) || ethernetFrame.isBroadcast()) {
      if (ethernetFrame.protocol() == Ethernet.ARP_PORT) {
        handleARPPacket(ethernetFrame);
      } else if (ethernetFrame.protocol() == Ethernet.IP_PORT) {
        handleIPPacket(ethernetFrame.payload());
      }
    } else {
      sendFrame(ethernetFrame);
    }
    return true;
  }

  private void handleIPPacket(byte[] packet) {
    IP ipPacket = new IP(packet);
    System.out.println(ipPacket.toString());

    if (ipPacket.isBroadcast()) {
      // This is broadcast IP packets
      Ethernet frame = new Ethernet(Ethernet.BROADCASTMAC, addressMAC, Ethernet.IP_PORT, ipPacket);
      sendFrame(frame);
      if (ipPacket.getDemuxPort() == 17) {
        // pass to UDP on router... Dont know what packets the router would receive yet...
        handleUDPPacket(ipPacket);
      }
    } else if (Arrays.equals(addressIP, ipPacket.destination())) {
      // Packets destined for the router
      System.out.println("Received packet destined for router");
      if (ipPacket.getDemuxPort() == 17) {
          handleUDPPacket(ipPacket);
      }
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
    }
  }

  public void handleUDPPacket(IP ipPacket) {
    UDP udpPacket = new UDP(ipPacket.payload());
    System.out.println(udpPacket.toString());
    if (udpPacket.demuxPort() == DHCP.SERVERPORT) {
      DHCP dhcpReq = new DHCP(udpPacket.payload());
      DHCP bootReply = dhcpServer.generateBootResponse(dhcpReq);
      System.out.println(String.format("Assigned IP %s to %s", 
        IP.ipString(bootReply.getCiaddr()), 
        Ethernet.macString(bootReply.getChaddr())));
      try {
        UDP udpPack = new UDP(DHCP.CLIENTPORT, DHCP.SERVERPORT, bootReply.serialize());
        IP ipPack = new IP(dhcpReq.getCiaddr(), addressIP, ipPacket.getIdentifier(), UDP.DEMUXPORT, udpPack.getBytes());
        Ethernet frame = new Ethernet(bootReply.getChaddr(), addressMAC, Ethernet.IP_PORT, ipPack.getBytes());
        sendFrame(frame);        
      } catch (Exception e) {
        //TODO: handle exception
        e.printStackTrace();
      }
    } else {
    	String payload = new String(udpPacket.payload());
    	System.out.println(payload);
    }
  }

  private void handleARPPacket(Ethernet ethernetFrame) {
      byte[] packet = ethernetFrame.payload();
      byte[] sourceMAC = ethernetFrame.source();
      ARP arpPacket = new ARP(packet);
      System.out.println(arpPacket.toString());
      
      if (arpPacket.opCode() == 1) {
        if (Arrays.equals(arpPacket.destIP(), addressIP)) {
        	System.out.println("ARP request received");
        	sendResponseARP(arpPacket.srcMAC(), arpPacket.srcIP());
        } else {
          Ethernet ethernetFrameToForward = new Ethernet(Ethernet.BROADCASTMAC, sourceMAC, Ethernet.ARP_PORT, packet);
          sendFrame(ethernetFrameToForward);
        }
      } else if (arpPacket.opCode() == 2) {
    	  System.out.println("ARP response received");
    	  arpTable.addPair(arpPacket.srcIP(), arpPacket.srcMAC());
      } else {
    	  System.out.println("Invalid opCode");
      }
  }

  private void sendRequestARP(byte[] destIP) {
	    ARP arpPacket = new ARP(1, addressMAC, addressIP, ARP.zeroMAC, destIP);
	    Ethernet frame = encapsulateEthernet(ARP.broadcastMAC, addressMAC, ARP.DEMUX_PORT, arpPacket.getBytes());
	    sendFrame(frame);
  }

  private void sendResponseARP(byte[] destMAC, byte[] destIP) {
		  ARP arpPacket = new ARP(2, addressMAC, addressIP, destMAC, destIP);
		  Ethernet frame = encapsulateEthernet(destMAC, addressMAC, ARP.DEMUX_PORT, arpPacket.getBytes());
		  System.out.println(Constants.bytesToString(arpPacket.getBytes()));
		  sendFrame(frame);
  }

  private void sendFrame(Ethernet frame) {
    this.packet.setData(frame.getBytes());
    for (int i = 0; i < linkIDs.size(); i++) {
      this.packet.setPort(linkIDs.get(i));
      // System.out.println("Broadcasting to host on logical port "+this.packet.getPort());
      try {
        this.internalInterface.send(this.packet);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
  
  public Ethernet encapsulateEthernet(byte[] destAddr, byte[] sourceAddr, int demuxPort, byte[] payload) {
	    return new Ethernet(destAddr, sourceAddr, demuxPort, payload);
  }

  private void addPortToArrayList(int port) {
    for (int i = 0; i < linkIDs.size(); i++) {
      if (linkIDs.get(i) == port) {
        return;
      }
    }
    linkIDs.add(port);
  }

  
  /***************************************************************************/
  /************************* Interactive methods *****************************/
  /***************************************************************************/  

  public static void handleUserInputs() {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
      String line = "";
      while ((line = reader.readLine()) != null) {
        if (line.equals("shut down")) {
          System.out.println("Shutting down router...");
          System.exit(0);
        } else if (line.split(" ")[0].equals("plug") && line.split(" ")[1].equals("into")) {
          System.out.println("Connect to another router");
        } else {
          System.out.println("Unknown command");
        }
      }      
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(0);
    }
  }

}

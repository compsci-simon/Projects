package nat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Router {
  private DatagramSocket internalInterface;
  private DatagramSocket externalInterface;
  private DatagramPacket packet;
  private ArrayList<Integer> internalLinks;
  private ArrayList<Integer> externalLinks;
  private DHCPServer dhcpServer;
  private ARPTable arpTable;
  private byte[] addressMAC;
  private byte[] addressIP = {(byte) 0xC0, (byte) 0xA8, 0, 1};
  private byte[] externalIP;
  private byte[] externalMAC;
  public static final byte[] subnetMask = {(byte)0xff, (byte)0xff, (byte)0xff, 0};
  public static final byte[] broadcastIP = {(byte) 0xC0, (byte) 0xA8, 0, (byte)0xff};
  private boolean handleInternal = false;
  private boolean handleExternal = false;
  private int internalPort;
  private int externalPort;
  private String address;
  private int icmpID;

  public Router (String address) {
    this.address = address;
    this.icmpID = 0;
    this.internalLinks = new ArrayList<Integer>();
    this.externalLinks = new ArrayList<Integer>();
    this.addressMAC = Ethernet.generateRandomMAC();
    this.externalIP = IP.generateRandomIP();
    this.externalMAC = Ethernet.generateRandomMAC();
    try {
      
      this.packet = new DatagramPacket(new byte[1500], 1500);
      this.dhcpServer = new DHCPServer(addressIP);
      this.arpTable = new ARPTable();
      System.out.println("Router started...");
      handleUserInputs();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    new Router("localhost");
  }

  private void handleInternalConnections(int port) {
    try {
      this.internalInterface = new DatagramSocket(port);
      while (true) {
    	  packet = new DatagramPacket(new byte[1500], 1500);
        internalInterface.receive(packet);
        addPortToInternalLinks(packet.getPort());
        if (!handleFrame(packet.getData()))
          break;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void handleExternalConnections(int port) {
    try {
      this.externalInterface = new DatagramSocket(port);
      while (true) {
    	  packet = new DatagramPacket(new byte[1500], 1500);
        externalInterface.receive(packet);
        addPortToExternalLinks(packet.getPort());
        if (!handleFrame(packet.getData()))
          break;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private boolean handleFrame(byte[] frame) {
    String message = new String(frame).strip();
    System.out.println(message);
    if (message.equals("hello")) {
      System.out.println("External router connected via ethernet");
      return true;
    }
    Ethernet ethernetFrame = new Ethernet(frame);
    // Router MAC address of broadcast addressed frame are accepted
    System.out.println(ethernetFrame.toString());
    sendFrame(ethernetFrame, true);
    // Perhaps check for destination later...
    if (ethernetFrame.protocol() == Ethernet.ARP_PORT) {
      handleARPPacket(ethernetFrame);
    } else if (ethernetFrame.protocol() == Ethernet.IP_PORT) {
      handleIPPacket(ethernetFrame.payload());
    }
    return true;
  }

  private void handleIPPacket(byte[] packet) {
    IP ipPacket = new IP(packet);
    System.out.println(ipPacket.toString());

    if (ipPacket.isBroadcast()) {
      // This is broadcast IP packets
      Ethernet frame = new Ethernet(Ethernet.BROADCASTMAC, addressMAC, Ethernet.IP_PORT, ipPacket);
      sendFrame(frame, true);
      if (ipPacket.getDemuxPort() == 17) {
        // pass to UDP on router... Dont know what packets the router would receive yet...
        handleUDPPacket(ipPacket);
      }
    } else if (Arrays.equals(addressIP, ipPacket.destination())) {
      // Packets destined for the router from private network
      switch (ipPacket.getDemuxPort()) {
        case IP.UDP_PORT:
          handleUDPPacket(ipPacket);
          break;
        case IP.ICMP_PORT:
          handleICMPPacket(ipPacket);
          break;
        default:
          System.err.println("Unknown demux port for IP packet");
          break;
        }
    } else if (Arrays.equals(externalIP, ipPacket.destination())) {
      // Packet from external router
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

  public void handleICMPPacket(IP ipPacket) {
    ICMP icmpPacket = new ICMP(ipPacket.payload());
    System.out.println(icmpPacket.toString());
    if (icmpPacket.getType() == ICMP.PING_REQ) {
      ICMP response = ICMP.pingResponse(icmpPacket);
      IP ipPack = new IP(ipPacket.source(), addressIP, ipPacket.getIdentifier(), IP.ICMP_PORT, response.getBytes());
      byte[] destMAC = getMAC(ipPack.destination());
      if (destMAC == null) {
        System.err.println(String.format("Could not resolve host MAC for %s", 
          IP.ipString(ipPack.destination())));
        return;
      }
      Ethernet frame = new Ethernet(destMAC, addressMAC, Ethernet.IP_PORT, ipPack.getBytes());
      sendFrame(frame, true);
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
        sendFrame(frame, true);        
      } catch (Exception e) {
        //TODO: handle exception
        e.printStackTrace();
      }
    } else {
    	String payload = new String(udpPacket.payload());
    	System.out.println(payload);
    }
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
        e.printStackTrace();
      }
      hasIP = arpTable.containsMAC(ip);
    }
    if (i == 2) {
      System.out.println(String.format("Could not resolve IP: %s to physical address", IP.ipString(ip)));
      return null;
    }
    return arpTable.getMAC(ip);
  }
  
  private void handleARPPacket(Ethernet ethernetFrame) {
      byte[] packet = ethernetFrame.payload();
      byte[] sourceMAC = ethernetFrame.source();
      ARP arpPacket = new ARP(packet);
      System.out.println(arpPacket.toString());
      System.out.println();
      
      if (arpPacket.opCode() == ARP.ARP_REQUEST) {
        if (Arrays.equals(arpPacket.destIP(), addressIP)) {
          sendResponseARP(arpPacket.srcMAC(), arpPacket.srcIP());
          arpTable.addPair(arpPacket.srcIP(), arpPacket.srcMAC());
        }
      } else if (arpPacket.opCode() == ARP.ARP_REPLY) {
        arpTable.addPair(arpPacket.srcIP(), arpPacket.srcMAC());
      } else {
        System.out.println("Invalid opCode");
      }
  }

  private void sendRequestARP(byte[] destIP) {
	  ARP arpPacket = new ARP(ARP.ARP_REQUEST, addressMAC, addressIP, ARP.zeroMAC, destIP);
	  Ethernet frame = new Ethernet(ARP.broadcastMAC, addressMAC, ARP.DEMUX_PORT, arpPacket.getBytes());
	  sendFrame(frame, true);
  }

  private void sendResponseARP(byte[] destMAC, byte[] destIP) {
		  ARP arpPacket = new ARP(2, addressMAC, addressIP, destMAC, destIP);
		  Ethernet frame = encapsulateEthernet(destMAC, addressMAC, ARP.DEMUX_PORT, arpPacket.getBytes());
		  sendFrame(frame, true);
  }

  private void sendFrame(Ethernet frame, boolean internalInterface) {
    this.packet.setData(frame.getBytes());
    if (internalInterface) {
      for (int i = 0; i < internalLinks.size(); i++) {
        this.packet.setPort(internalLinks.get(i));
        try {
          this.internalInterface.send(this.packet);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    } else {
      for (int i = 0; i < externalLinks.size(); i++) {
        this.packet.setPort(externalLinks.get(i));
        try {
          this.externalInterface.send(this.packet);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }
  
  public Ethernet encapsulateEthernet(byte[] destAddr, byte[] sourceAddr, int demuxPort, byte[] payload) {
	    return new Ethernet(destAddr, sourceAddr, demuxPort, payload);
  }

  private void addPortToInternalLinks(int port) {
    for (int i = 0; i < internalLinks.size(); i++) {
      if (internalLinks.get(i) == port) {
        return;
      }
    }
    internalLinks.add(port);
  }

  private void addPortToExternalLinks(int port) {
    for (int i = 0; i < externalLinks.size(); i++) {
      if (externalLinks.get(i) == port) {
        return;
      }
    }
    externalLinks.add(port);
  }

  
  /***************************************************************************/
  /************************* Interactive methods *****************************/
  /***************************************************************************/  

  public void handleUserInputs() {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
      String line = "";
      while ((line = reader.readLine()) != null) {
        System.out.println();
        if (line.equals("shutdown")) {
          System.out.println("Shutting down router...");
          System.exit(0);
        } else if (line.split(" ")[0].equals("plug") && line.split(" ")[1].equals("into")) {
          System.out.println("Connecting to other router");
          if (line.split(" ").length != 3) {
            System.err.println("Incorrect number of arguments");
            continue;
          }
          int port = Integer.parseInt(line.split(" ")[2]);
          connectToRouter(port);
        } else if (line.equals("status")) {
          System.out.println(toString());
        } else if (line.equals("open in port") && !handleInternal) {

          System.out.print("Enter a port number: ");
          line = reader.readLine();
          internalPort = 0;
          try {
            internalPort = Integer.parseInt(line);
          } catch (Exception e) {
            e.printStackTrace();
            return;
          }
          handleInternal = true;
          new Thread() {
            @Override
            public void run() {
              handleInternalConnections(internalPort);
            }
          }.start();
          System.out.println("Internal interface is open on port " + internalPort);

        } else if (line.equals("open ex port") && !handleExternal) {

          System.out.print("Enter a port number: ");
          line = reader.readLine();
          externalPort = 0;
          try {
            externalPort = Integer.parseInt(line);
          } catch (Exception e) {
            e.printStackTrace();
            return;
          }
          handleExternal = true;
          new Thread() {
            @Override
            public void run() {
              handleExternalConnections(externalPort);
            }
          }.start();
          System.out.println("External interface is open on port " + externalPort);

        } else {
          System.out.println("Unknown command");
        }
      }      
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(0);
    }
  }

  public void connectToRouter(int port) {
    byte[] buf = "hello".getBytes();
    ICMP routerAd = new ICMP(ICMP.ROUTER_ADVERTISEMENT, (byte)icmpID++, new byte[1]);
    try {      
      packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(address), port);
      externalInterface.send(packet);
      addPortToExternalLinks(port);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String toString() {
    String internalInterfacePort = "Closed";
    String externalInterfacePort = "Closed";
    if (internalInterface != null) {
      internalInterfacePort = String.format("%d", internalInterface.getLocalPort());
    }
    if (externalInterface != null) {
      externalInterfacePort = String.format("%d", externalInterface.getLocalPort());
    }
    String s = String.format("ROUTER toString\n----------------------" + 
      "\nInternal MAC Address = %s\nExternal MAC Address = %s\nInternal IP = %s\nExternal IP = %s\nInternal" + 
      " interface port = %s\nExternal interface port = %s\n", 
      Ethernet.macString(addressMAC), Ethernet.macString(externalMAC), 
      IP.ipString(addressIP), IP.ipString(externalIP), internalInterfacePort, externalInterfacePort);
    return s;
  }

}

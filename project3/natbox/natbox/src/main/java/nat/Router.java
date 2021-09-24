package nat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Router {
  private DatagramSocket internalInterface;
  private DatagramSocket externalInterface;
  private ArrayList<Integer> internalLinks;
  private ArrayList<Integer> externalLinks;
  private DHCPServer dhcpServer;
  private ARPTable arpTable;
  private NAPT naptTable;
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
  private int ipID;
  private int icmpID;
  private int skipLinkPortNum = -1;

  public Router () {
    this.ipID = 0;
    this.icmpID = 0;
    this.internalLinks = new ArrayList<Integer>();
    this.externalLinks = new ArrayList<Integer>();
    this.addressMAC = Ethernet.generateRandomMAC();
    this.externalIP = IP.generateRandomIP();
    this.externalMAC = Ethernet.generateRandomMAC();
    try {
      this.dhcpServer = new DHCPServer(addressIP);
      this.arpTable = new ARPTable();
      this.naptTable = new NAPT(externalIP);
      System.out.println("Router started...");
      //internalInterface = new DatagramSocket(5000);
      new Thread() {
        @Override
        public void run() {
          //handleInternalConnections(5000);
        }
      }.start();
    } catch (Exception e) {
      e.printStackTrace();
    }
    handleUserInputs();
  }

  public static void main(String[] args) {
    Router r = new Router();
    //r.handleUserInputs();
  }

  /***************************************************************************/
  /**************************** Handling methods *****************************/
  /***************************************************************************/

  public void handleInternalConnections(int port) {
    try {
    this.internalInterface = new DatagramSocket(port);
      DatagramPacket packet;
      while (true) {
    	  packet = new DatagramPacket(new byte[1500], 1500);
        internalInterface.receive(packet);
        addPortToInternalLinks(packet.getPort());
        skipLinkPortNum = packet.getPort();
        sendFrame(new Ethernet(packet.getData()), true);
        skipLinkPortNum = -1;
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
      DatagramPacket packet;
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
    Ethernet ethernetFrame = new Ethernet(frame);
    if (Arrays.equals(ethernetFrame.source(), addressMAC))
      return true;
    System.out.println(ethernetFrame.toString());
    
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

    if (ipPacket.isBroadcast() || Arrays.equals(addressIP, ipPacket.destination())) {
      // This is broadcast IP packets
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
      // Packets from external interface
      ipPacket = naptTable.translate(ipPacket);
      if (!Arrays.equals(ipPacket.destination(), addressIP)) {
        byte[] lanMAC = getMAC(ipPacket.destination());
        Ethernet frame = new Ethernet(lanMAC, addressMAC, ipPacket.getDemuxPort(), ipPacket.getBytes());
        sendFrame(frame, true);
      }
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
    } else {
      // Packets that need to be routed

      ipPacket = naptTable.translate(ipPacket);
      //System.out.println(naptTable.containsSession(packet));
      if (ipPacket == null) {
        System.out.println("Could not translate IP packet");
        return;
      }
      System.out.println(IP.ipString(ipPacket.source()));
      boolean LAN = IP.sameNetwork(ipPacket.destination(), addressIP);
      byte[] destMAC = getMAC(ipPacket.destination());
      byte[] sourceMAC = LAN ? addressMAC : externalMAC;
      Ethernet frame = new Ethernet(destMAC, sourceMAC, Ethernet.IP_PORT, ipPacket.getBytes());
      
      sendFrame(frame, LAN);
    }
  }

  public void handleICMPPacket(IP ipPacket) {
    ICMP icmpPacket = new ICMP(ipPacket.payload());
    System.out.println(icmpPacket.toString());
    boolean internal = true;
    if (!IP.sameNetwork(ipPacket.destination(), addressIP)) {
      internal = false;
    }
    byte[] sourceIP = internal ? addressIP : externalIP;
    byte[] sourceMAC = internal ? addressMAC : externalMAC;

    if (icmpPacket.getType() == ICMP.PING_REQ) {

      ICMP response = ICMP.pingResponse(icmpPacket);
      IP ipPack = new IP(ipPacket.source(), sourceIP, ipPacket.getIdentifier(), IP.ICMP_PORT, response.getBytes());
      byte[] destMAC = getMAC(ipPack.destination());
      if (destMAC == null) {
        System.err.println(String.format("Could not resolve host MAC for %s", 
          IP.ipString(ipPack.destination())));
        return;
      }
      Ethernet frame = new Ethernet(destMAC, sourceMAC, Ethernet.IP_PORT, ipPack.getBytes());
      sendFrame(frame, internal);

    } else if (icmpPacket.getType() == ICMP.ROUTER_SOLICITATION) {

    	ICMP routerAd = new ICMP(ICMP.ROUTER_ADVERTISEMENT, (byte)icmpID++, new byte[1]);
      IP ipPacketSend = new IP(IP.broadcastIP, externalIP, ipID++, IP.ICMP_PORT, routerAd.getBytes());
      Ethernet frame = new Ethernet(Ethernet.BROADCASTMAC, externalMAC, Ethernet.IP_PORT, ipPacketSend.getBytes());
      sendFrame(frame, false);
    
    }

  }

  public void handleUDPPacket(IP ipPacket) {
    UDP udpPacket = new UDP(ipPacket.payload());
    System.out.println(udpPacket.toString());
    if (udpPacket.demuxPort() == DHCP.SERVER_PORT) {
      DHCP dhcpReq = new DHCP(udpPacket.payload());
      System.out.println(dhcpReq.toString());
      DHCP bootReply = dhcpServer.generateBootResponse(dhcpReq);
      System.out.println(String.format("Assigned IP %s to %s\n", 
                        IP.ipString(bootReply.getCiaddr()), 
                        Ethernet.macString(bootReply.getChaddr())));
      UDP udpPack = new UDP(DHCP.CLIENT_PORT, DHCP.SERVER_PORT, bootReply.getBytes());
      IP ipPack = new IP(dhcpReq.getCiaddr(), addressIP, ipPacket.getIdentifier(), IP.UDP_PORT, udpPack.getBytes());
      Ethernet frame = new Ethernet(bootReply.getChaddr(), addressMAC, Ethernet.IP_PORT, ipPack.getBytes());
      sendFrame(frame, true);
      
    } else if (udpPacket.demuxPort() == UDP.MESSAGE_PORT) {
      System.out.println(new String(udpPacket.payload()));
    } else {
      System.out.println("Unknown udp port " + udpPacket.demuxPort());
    }
  }

  public byte[] getMAC(byte[] ip) {

    boolean hasIP = arpTable.containsMAC(ip);
    int i = 0;
    for (; i < 2; i++) {
      if (hasIP)
        break;
      System.out.println("Sent ARP request\n");
      sendRequestARP(ip);
      try {
        Thread.sleep(100);
      } catch (Exception e) {
        e.printStackTrace();
      }
      hasIP = arpTable.containsMAC(ip);
    }
    if (i == 2) {
      System.out.println(String.format("Could not resolve IP: %s to physical address\n", IP.ipString(ip)));
      return null;
    }
    return arpTable.getMAC(ip);
  }
  
  private void handleARPPacket(Ethernet ethernetFrame) {
      byte[] packet = ethernetFrame.payload();
      ARP arpPacket = new ARP(packet);
      System.out.println(arpPacket.toString());
      
      if (arpPacket.opCode() == ARP.ARP_REQUEST) {
        if (Arrays.equals(arpPacket.destIP(), addressIP) || 
            Arrays.equals(arpPacket.destIP(), externalIP)) {

          sendResponseARP(arpPacket.srcMAC(), arpPacket.srcIP());
          
          arpTable.addPair(arpPacket.srcIP(), arpPacket.srcMAC());
          System.out.println(arpTable.toString());
        }
      } else if (arpPacket.opCode() == ARP.ARP_REPLY) {
        if (Arrays.equals(arpPacket.destIP(), addressIP) || 
            Arrays.equals(arpPacket.destIP(), externalIP)) {

          arpTable.addPair(arpPacket.srcIP(), arpPacket.srcMAC());
          System.out.println(arpTable.toString());
        }
      } else {
        System.out.println("Invalid opCode");
      }
  }

  /***************************************************************************/
  /**************************** Sending methods ******************************/
  /***************************************************************************/
  
  private void sendFrame(Ethernet frame, boolean LAN) {
	  DatagramPacket packet = new DatagramPacket(new byte[1500], 1500);
    try {
      packet.setAddress(InetAddress.getLocalHost());      
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }
    packet.setData(frame.getBytes());

    if (LAN) {
      for (int i = 0; i < internalLinks.size(); i++) {
        if (internalLinks.get(i) == skipLinkPortNum)
          continue;
        packet.setPort(internalLinks.get(i));
        try {
          internalInterface.send(packet);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    } else {
      for (int i = 0; i < externalLinks.size(); i++) {
        packet.setPort(externalLinks.get(i));
        try {
          externalInterface.send(packet);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }
  
  private void sendRequestARP(byte[] destIP) {
    boolean internal = true;
    if (!IP.sameNetwork(destIP, addressIP)) {
      internal = false;
    }
    byte[] srcIP = internal ? addressIP : externalIP;
    byte[] srcMAC = internal ? addressMAC : externalMAC;

	  ARP arpPacket = new ARP(ARP.ARP_REQUEST, srcMAC, srcIP, ARP.zeroMAC, destIP);
	  Ethernet frame = new Ethernet(ARP.broadcastMAC, srcMAC, ARP.DEMUX_PORT, arpPacket.getBytes());
	  sendFrame(frame, internal);
  }

  private void sendResponseARP(byte[] destMAC, byte[] destIP) {
    boolean internal = true;
    if (!IP.sameNetwork(destIP, addressIP)) {
      internal = false;
    }
    byte[] srcIP = internal ? addressIP : externalIP;
    byte[] srcMAC = internal ? addressMAC : externalMAC;
    ARP arpPacket = new ARP(ARP.ARP_REPLY, srcMAC, srcIP, destMAC, destIP);
    Ethernet frame = new Ethernet(destMAC, addressMAC, ARP.DEMUX_PORT, arpPacket.getBytes());
    sendFrame(frame, internal);
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

  public void ping(byte[] ip) {
    boolean internal = true;
    if (!IP.sameNetwork(ip, addressIP)) {
      internal = false;
    }
    byte[] sourceIP = internal ? addressIP : externalIP;
    byte[] sourceMAC = internal ? addressMAC : externalMAC;

    ICMP ping = new ICMP(ICMP.PING_REQ, (byte) (icmpID++), new byte[1]);
    IP ipPacket = new IP(ip, sourceIP, ipID++, IP.ICMP_PORT, ping.getBytes());
    byte[] destMAC = getMAC(ip);
    if (destMAC == null) {
      return;
    }
    Ethernet frame = new Ethernet(destMAC, sourceMAC, Ethernet.IP_PORT, ipPacket.getBytes());
    sendFrame(frame, internal);
  }

  public void ping(String ipString) {
    ipString = ipString.strip();
    byte[] ip = new byte[4];
    String[] ipStringArray = ipString.split("[.]");
    if (ipStringArray.length != 4) {
      System.err.println("Incorrect IP format");
      return;
    }
    for (int i = 0; i < 4; i++) {
      try {
        ip[i] = (byte) Integer.parseInt(ipStringArray[i]);
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }
    }
    ping(ip);
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

        } else if (line.equals("port forward")) {
          System.out.print("Enter a port: ");
          String port = reader.readLine();
          System.out.print("Enter a LAN IP: ");
          String ip = reader.readLine();
          portForward(port, ip);
        } else if (line.split(" ")[0].equals("ping")) {
          ping(line.split(" ")[1]);
          
        } else if (line.equals("napt table")) {
        	naptTable.toString();
        	
        } else if (line.equals("arp table")) { 
        	arpTable.toString(); 
        	
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
    ICMP routerAd = new ICMP(ICMP.ROUTER_SOLICITATION, (byte)icmpID++, new byte[1]);
    IP ipPacket = new IP(IP.broadcastIP, externalIP, ipID++, IP.ICMP_PORT, routerAd.getBytes());
    Ethernet frame = new Ethernet(Ethernet.BROADCASTMAC, externalMAC, Ethernet.IP_PORT, ipPacket.getBytes());
    addPortToExternalLinks(port);
    sendFrame(frame, false);
  }

  public String connectedLinks() {
    String s = String.format("----------------------\nEXTERNAL LINKS\n");
    if (externalLinks.size() == 0) {
      s += "None";
    } else {
      for (int i = 0; i < externalLinks.size(); i++) {
        s = String.format("%s%d ", s, externalLinks.get(i));
      }
    }
    s = String.format("%s\nINTERNAL LINKS\n", s);
    if (internalLinks.size() == 0) {
      s += "None";
    } else {
      for (int i = 0; i < internalLinks.size(); i++) {
        s = String.format("%s%d ", s, internalLinks.get(i));
      }
    }
    return s + "\n";
  }

  public void portForward(String port, String IP) {
    int portNum;
    byte[] ip = new byte[4];
    try {
      portNum = Integer.parseInt(port);
      if (portNum < 1) {
        System.err.println("Invalid port number " + port);
        return;
      }
      String[] ipBytes = IP.split("[.]");
      if (ipBytes.length != 4) {
        System.err.println("IP must be 4 octets, not " + ipBytes.length);
        return;
      }
      for (int i = 0; i < 4; i++) {
        ip[i] = (byte) Integer.parseInt(ipBytes[i]);
      }
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }
    naptTable.portForward(portNum, ip);
    System.out.println(naptTable.toString());
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

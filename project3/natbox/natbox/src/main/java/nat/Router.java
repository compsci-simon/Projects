package nat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * The router that contains a DHCP server, a natbox and allows
 * clients to connect to it. Simulates some of the basic funcitonality
 * of an actual router.
 */
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
  private boolean handleInternal = false;
  private boolean handleExternal = false;
  private int internalPort;
  private int externalPort;
  private int ipID;
  private int icmpID;
  private int skipLinkPortNum = -1;
  private static int minToRefresh = 1;
  private int secondsToRefresh = 45;
  private InetAddress iaddress;
  private static final int TIMEOUT = 400;

  /**
   * Creates a router from scratch
   * @param portIn The port on which internal connections will be accepted
   * @param portEx The port on which external connections will be accepted
   * @param min The interval at which to check the napt table for refreshing
   * @param seconds How long an entry should last in the napt table
   */
  public Router (int portIn, int portEx, int min, int seconds) {
    this.internalPort = portIn;
    this.externalPort = portEx;
    this.ipID = 0;
    this.icmpID = 0;
    this.minToRefresh = min;
    this.secondsToRefresh = seconds;
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
      if (portIn != -1 && portEx != -1) {
        new Thread() {
          @Override
          public void run() {
            handleInternalConnections();
          }
        }.start();
        new Thread() {
          @Override
          public void run() {
            handleExternalConnections();
          }
        }.start();
        //expireDHCP();
    
        new Thread() {
            @Override
            public void run() {
              naptTable.refreshNAPTTable(minToRefresh, secondsToRefresh);
            }
        }.start();
      }
      handleUserInputs();
    } catch (Exception e) {
      e.printStackTrace();
    }
    handleUserInputs();
  }

  public static void main(String[] args) {
    System.out.println(args.length);
    if (args.length == 4) {
      try {
        int portIn = Integer.parseInt(args[0]);
        int portEx = Integer.parseInt(args[1]);
        int min = Integer.parseInt(args[2]);
        int seconds = Integer.parseInt(args[3]);
        new Router(portIn, portEx, min, seconds);        
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      new Router(-1, -1, 60, 45);
    }
  }

  /***************************************************************************/
  /**************************** Handling methods *****************************/
  /***************************************************************************/

  /**
   * This method handles all UDP packets received on the internal interface
   * that need to be turned into Ethernet frames.
   */
  public void handleInternalConnections() {
    if (internalPort == -1) {
      System.err.println("You need to set the internal portnum first");
      return;
    }
    try {
      internalInterface = new DatagramSocket(internalPort);
      DatagramPacket packet;
      while (true) {
    	  packet = new DatagramPacket(new byte[1500], 1500);
        internalInterface.receive(packet);
        addPortToInternalLinks(packet.getPort());
        iaddress = packet.getAddress();
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

  /**
   * This method handles all UDP packets received on the external interface
   * that need to be turned into Ethernet frames.
   */
  private void handleExternalConnections() {
    if (externalPort == -1) {
      System.err.println("Need to set port number first");
      return;
    }
    try {
      externalInterface = new DatagramSocket(externalPort);
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
  
  /**
   * This method takes the bytes receieved over the simulated physical link
   * and turns them into an Ethernet frame which has a header and a payload.
   * @param frame The bytes received over the simulated physical link
   * @return True for whether or not the router must continue running.
   */
  private boolean handleFrame(byte[] frame) {
    Ethernet ethernetFrame = new Ethernet(frame);
    if (Arrays.equals(ethernetFrame.source(), addressMAC))
      return true;
    System.out.println(ethernetFrame.toString());
    
    if (Arrays.equals(ethernetFrame.destination(), addressMAC) || ethernetFrame.isBroadcast() || 
        Arrays.equals(ethernetFrame.destination(), externalMAC)) {
      if (ethernetFrame.protocol() == Ethernet.ARP_PORT) {
        handleARPPacket(ethernetFrame);
      } else if (ethernetFrame.protocol() == Ethernet.IP_PORT) {
        handleIPPacket(ethernetFrame);
      }
    }
    return true;
  }

  /**
   * This method takes the payload from the ethernet frame and transforms it 
   * into an IP packet that can be processed further.
   * @param packet The byte array payload from the ethernet frame.
   */
  private void handleIPPacket(Ethernet receivedFrame) {

    IP ipPacket = new IP(receivedFrame.payload());
    System.out.println(ipPacket.toString());

    if (ipPacket.isBroadcast() || Arrays.equals(addressIP, ipPacket.destination())) {
      // This is broadcast IP packets
      switch (ipPacket.getDemuxPort()) {
        case IP.UDP_PORT:
          handleUDPPacket(ipPacket);
          break;
        case IP.TCP_PORT:
            handleTCPPacket(ipPacket.payload());
            break;
        case IP.ICMP_PORT:
          handleICMPPacket(ipPacket);
          break;
        default:
          System.err.println("Unknown demux port for IP packet");
          break;
        }
    } else if (Arrays.equals(externalIP, ipPacket.destination())) {
      // Packets received from external interface
    	
      ipPacket = naptTable.translate(ipPacket);
   
      if (IP.sameNetwork(ipPacket.destination(), addressIP)) {
        byte[] lanMAC = getMAC(ipPacket.destination());
        if (lanMAC == null) {
          return;
        }
        System.out.println("Received packet here!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println(ipPacket.toString());
        System.out.println(iaddress.toString());
        System.out.println("--------------------------------------------");
        Ethernet frame = new Ethernet(lanMAC, addressMAC, Ethernet.IP_PORT, ipPacket.getBytes());
        sendFrame(frame, true);
      } else {
        switch (ipPacket.getDemuxPort()) {
          case IP.UDP_PORT:
            handleUDPPacket(ipPacket);
            break;
          case IP.TCP_PORT:
              handleTCPPacket(ipPacket.payload());
              break;
          case IP.ICMP_PORT:
            handleICMPPacket(ipPacket);
            break;
          default:
            System.err.println("Unknown demux port for IP packet");
            break;
          }
      }
    } else {
      // Packets that need to be routed from the router and are not destined 
      // for the router

      ipPacket = naptTable.translate(ipPacket);
      //System.out.println(naptTable.containsSession(packet));
      if (ipPacket == null) {
        System.out.println("Could not translate IP packet");
        return;
      }
      boolean LAN = IP.sameNetwork(ipPacket.destination(), addressIP);
      if (!LAN) {
        byte[] destMAC = getMAC(ipPacket.destination());
        if (destMAC == null) {
          // Create ICMP packet to return to host because client was unreachable
          ICMP icmp = new ICMP(ICMP.ERROR_UNREACHABLE, (byte) (icmpID++), new byte[1]);
          IP receivedPacket = new IP(receivedFrame.payload());
          IP packetIP = new IP(receivedPacket.source(), addressIP, ipID, IP.ICMP_PORT, icmp.getBytes());
          Ethernet frame = new Ethernet(receivedFrame.source(), addressMAC, Ethernet.IP_PORT, packetIP.getBytes());
          sendFrame(frame, true);
          return;
        }
        Ethernet frame = new Ethernet(destMAC, externalMAC, Ethernet.IP_PORT, ipPacket.getBytes());
        
        sendFrame(frame, LAN);
      }
    }
  }

  /**
   * This method handles ICMP packets
   * @param ipPacket The IP packet that contains an ICMP packet
   */
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

  /**
   * This method handles UDP packets
   * @param packet The payload of the IP packet which is a UDP packet
   */
  public void handleUDPPacket(IP ipPacket) {
    UDP udpPacket = new UDP(ipPacket.payload());
    System.out.println(udpPacket.toString());
    if (udpPacket.destinationPort() == DHCP.SERVER_PORT) {
      DHCP dhcpReq = new DHCP(udpPacket.payload());
      System.out.println(dhcpReq.toString());
      DHCP bootReply = dhcpServer.generateBootResponse(dhcpReq);
      System.out.println(dhcpServer.toString());
      UDP udpPack = new UDP(DHCP.CLIENT_PORT, DHCP.SERVER_PORT, bootReply.getBytes());
      IP ipPack = new IP(dhcpReq.getCiaddr(), addressIP, ipPacket.getIdentifier(), IP.UDP_PORT, udpPack.getBytes());
      Ethernet frame = new Ethernet(bootReply.getChaddr(), addressMAC, Ethernet.IP_PORT, ipPack.getBytes());
      sendFrame(frame, true);
      
    } else if (udpPacket.destinationPort() == UDP.MESSAGE_PORT) {
      System.out.println(new String(udpPacket.payload()));
    
    } else if (udpPacket.destinationPort() == UDP.RELEASE_PORT) {
    	// don't think this is how it actually should work but it does what we want it do to
    	System.out.println("Releasing address");
    	byte[] ipToRemove = ipPacket.source();
    	dhcpServer.removeIP(ipToRemove);
    } else {
      System.out.println("Unknown udp port " + udpPacket.destinationPort());
    }
  }
    
  /**
   * This method handles TCP packets
   * @param ipPacket The IP packet that contains the TCP packet
   */
  public void handleTCPPacket(byte[] packet) {
    TCP tcpPacket = new TCP(packet);
    System.out.println(tcpPacket.toString());
    if (tcpPacket.destinationPort() == TCP.MESSAGE_PORT) {
      System.out.println(new String(tcpPacket.payload()));
    } else {
      System.out.println("TCP packet sent to unknown port");
    }
}

  public byte[] getMAC(byte[] ip) {

    boolean hasIP = arpTable.containsMAC(ip);
    int i = 0;
    int tries = 5;
    for (; i < tries; i++) {
      if (hasIP)
        break;
      System.out.println("Sent ARP request\n");
      sendRequestARP(ip);
      try {
        Thread.sleep(TIMEOUT);
      } catch (Exception e) {
        e.printStackTrace();
      }
      hasIP = arpTable.containsMAC(ip);
    }
    if (i == tries) {
      System.out.println(String.format("Could not resolve IP: %s to physical address\n", IP.ipString(ip)));
      return null;
    }
    return arpTable.getMAC(ip);
  }
  
  /**
   * This method handles ARP packets
   * @param packet The byte array that represents the ARP packet
   */
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
  
  /**
   * This method sents an simulated ethernet frame over the simulated network.
   * @param frame The ethernet frame to send
   */  
  private void sendFrame(Ethernet frame, boolean LAN) {
	  DatagramPacket packet = new DatagramPacket(new byte[1500], 1500);
    try {
      packet.setAddress(iaddress);      
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
      try {
        packet.setAddress(InetAddress.getLocalHost());
      } catch (Exception e) {
        e.printStackTrace();
      }
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
  
  /**
   * Send an ARP request to the specified IP address to resolved its
   * physical hardware address
   * @param destIP The IP address to be resolved to a MAC address
   */
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

  /**
   * Used to send an ARP response when an ARP request is received
   * @param destMAC The MAC of the host that sent the ARP request
   * @param destIP The IP of the host that sent the ARP request
   */
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

  /**
   * Used to add a udp socket port to the list of external links for use 
   * when communicating over the external links
   * @param port The port to be added
   */
  private void addPortToInternalLinks(int port) {
    for (int i = 0; i < internalLinks.size(); i++) {
      if (internalLinks.get(i) == port) {
        return;
      }
    }
    internalLinks.add(port);
  }

  /**
   * Used to add a udp socket port to the list of internal links for use 
   * when communicating over the internal links
   * @param port The port to be added
   */
  private void addPortToExternalLinks(int port) {
    for (int i = 0; i < externalLinks.size(); i++) {
      if (externalLinks.get(i) == port) {
        return;
      }
    }
    externalLinks.add(port);
  }

  /**
   * Used to ping a host on the LAN
   * @param ip The IP of the host to ping
   */
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
  
  /**
   * Used to ping a host on the LAN
   * @param ipString The IP of the host to be converted to a byte[]
   */
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

  /**
   * Handles user inputs from the console and performs various actions based 
   * on the inputs
   */
  private void handleUserInputs() {
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
              handleInternalConnections();
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
              handleExternalConnections();
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
        	System.out.println(naptTable.toString());
        } else if (line.equals("arp table")) { 
          System.out.println(arpTable.toString());
        	
        } else if (line.equals("time table")) { 
            System.out.println();
        	
        } else if (line.equals("dhcp table")) { 
            System.out.println(dhcpServer.toString()); 
        
        } else {
          System.out.println("Unknown command");
        }
      }      
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(0);
    }
  }

  /**
   * Connect the router to another router over the simulated WAN
   * @param port The port of the udp socket on the other router
   */
  private void connectToRouter(int port) {
    ICMP routerAd = new ICMP(ICMP.ROUTER_SOLICITATION, (byte)icmpID++, new byte[1]);
    IP ipPacket = new IP(IP.broadcastIP, externalIP, ipID++, IP.ICMP_PORT, routerAd.getBytes());
    Ethernet frame = new Ethernet(Ethernet.BROADCASTMAC, externalMAC, Ethernet.IP_PORT, ipPacket.getBytes());
    addPortToExternalLinks(port);
    sendFrame(frame, false);
  }
  
  /**
   * Uses the natbox to port forward traffic receieved on the external
   * interface that is destined for the specified port
   * @param port The port to forward
   * @param IP The internal IP address
   */
  private void portForward(String port, String IP) {
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

  /**
   * @return The String that represents the status of the router
   */
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

package nat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * The client can be seen as a simulation of a host such as your personal
 * computer at home. This personal computer can connect to a router and
 * and then communicated with other clients on the LAN or on the WAN.
 */
public class Client {
  private byte[] addressIP = {0, 0, 0, 0};
  private byte[] addressMAC;
  private DatagramSocket socket;
  private ARPTable arpTable;
  private int ipIdentifier;
  private static byte[] routerIP;
  private static byte[] subnetMask;
  private byte icmpID;
  private int portNum = -1;
  private String address;
  private boolean ack = false;
  private static final int TIMEOUT = 400;

  /**
   * This constructs a client that will connect to a router at a specified
   * address and on a specific UDP port
   * 
   * @param address The IP address of the machine running the router simulator
   * @param port The UDP port on which the router is accepting incoming 
   *            connections on the host machine running the router.
   */
  public Client(String address, int port) {
    this.address = address;
    this.addressMAC = Ethernet.generateRandomMAC();
    this.ipIdentifier = 0;
    arpTable = new ARPTable();
    System.out.println("Client started...");
    if (port != -1) {
      try {
        this.portNum = port;
        new Thread() {
          @Override
          public void run() {
            handleInterface();
          }
        }.start();
        Thread.sleep(100);      
      } catch (Exception e) {
        e.printStackTrace();
      }
      sendDHCPDiscover();
      handleUserInputs();
    }
    handleUserInputs();
  }

  public static void main(String[] args) {
    if (args.length != 2) {
      new Client("localhost", -1);
    } else {
      new Client(args[0], Integer.parseInt(args[1]));
    }
  }
  
  /***************************************************************************/
  /**************************** Handling methods *****************************/
  /***************************************************************************/

  /**
   * This method handles all UDP packets received that need to be turned into 
   * Ethernet frames.
   */
  private void handleInterface() {
    try {
      DatagramPacket packet;
      socket = new DatagramSocket();
      packet = new DatagramPacket(new byte[1500], 1500, InetAddress.getByName(address), portNum);
      while (true) {
        packet = new DatagramPacket(new byte[1500], 1500);
        socket.receive(packet);
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
    System.out.println(ethernetFrame.toString());
    if (ethernetFrame.protocol() == Ethernet.ARP_PORT) {
      if (Arrays.equals(addressMAC, ethernetFrame.destination())
                      || ethernetFrame.isBroadcast()) {

        handleARPPacket(ethernetFrame.payload());
      }
      return true;
    } else if (ethernetFrame.protocol() == Ethernet.IP_PORT) {
      if (Arrays.equals(addressMAC, ethernetFrame.destination())
                      || ethernetFrame.isBroadcast()) {
        handleIPPacket(ethernetFrame.payload());
      }
    }
    return true;
  }

  /**
   * This method takes the payload from the ethernet frame and transforms it 
   * into an IP packet that can be processed further.
   * @param packet The byte array payload from the ethernet frame.
   */
  private void handleIPPacket(byte[] packet) {
    IP ipPacket = new IP(packet);
    System.out.println(ipPacket.toString());

    if (ipPacket.isBroadcast() || Arrays.equals(addressIP, ipPacket.destination()) || 
      Arrays.equals(addressIP, IP.nilIP)) {

      switch (ipPacket.getDemuxPort()) {
      case IP.UDP_PORT:
        handleUDPPacket(ipPacket.payload());
        break;
      case IP.TCP_PORT:
        handleTCPPacket(ipPacket);
        break;
      case IP.ICMP_PORT:
        handleICMPPacket(ipPacket);
        break;
      default:
        System.err.println("Unknown demux port for IP packet");
        break;
      }
    } else {
      System.out.println("Ignoring packet\n");
    }
  }

  /**
   * This method handles ICMP packets
   * @param ipPacket The IP packet that contains an ICMP packet
   */
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
      sendFrame(frame);
    }
  }

  /**
   * This method handles UDP packets
   * @param packet The payload of the IP packet which is a UDP packet
   */
  public void handleUDPPacket(byte[] packet) {
    UDP udpPacket = new UDP(packet);
    System.out.println(udpPacket.toString());
    if (udpPacket.destinationPort() == DHCP.CLIENT_PORT) {
      handleDHCPPacket(udpPacket.payload());
    } else if (udpPacket.destinationPort() == UDP.MESSAGE_PORT) {
      System.out.println(new String(udpPacket.payload()));
      System.out.println();
    } else {
      System.out.println("UDP packet sent to unknown port");
    }
  }
  
  /**
   * This method handles TCP packets
   * @param ipPacket The IP packet that contains the TCP packet
   */
  public void handleTCPPacket(IP ipPacket) {
    byte[] packet = ipPacket.payload();
    TCP tcpPacket = new TCP(packet);
    System.out.println(tcpPacket.toString());
    if (tcpPacket.destinationPort() == TCP.MESSAGE_PORT) {
      switch (tcpPacket.getType()) {
        case TCP.ACK:
          ack = true;
          break;
        case TCP.SYN:
          sendACK(ipPacket.source(), tcpPacket);
          break;
        case TCP.PUSH:
          if (tcpPacket.destinationPort() == TCP.MESSAGE_PORT) {
            System.out.println(new String(tcpPacket.payload()));
            System.out.println();
          } else {
            System.out.println("Unknown destination port " + tcpPacket.destinationPort());
          }
          sendACK(ipPacket.source(), tcpPacket);
          break;
        default:
          System.out.println("Unknown TCP packet type");
          break;
      }
    } else {
      System.out.println("TCP packet sent to unknown port");
    }
  }

  /**
   * This method handles DHCP packets
   * @param packet The byte array that represents the DHCP packet
   */
  public void handleDHCPPacket(byte[] packet) {
    DHCP dhcpPacket =  new DHCP(packet);
    System.out.println(dhcpPacket.toString());
    if (dhcpPacket.getMessageType() == DHCP.BOOT_REPLY) {
      addressIP = dhcpPacket.getCiaddr();
      routerIP = dhcpPacket.getGateway();
      System.out.println(toString());
    } else if (dhcpPacket.getMessageType() == DHCP.ADDRESS_RELEASE) {
      for (int i = 0; i < 4; i++) {
        addressIP[i] = 0;
      }
      System.out.println("IP address released.");
    } else {
      System.out.println("Unknown address type " + dhcpPacket.getMessageType());
    }
  }

  /**
   * This method handles ARP packets
   * @param packet The byte array that represents the ARP packet
   */
  private void handleARPPacket(byte[] packet) {
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
      System.out.println(arpTable.toString());
  }
  
  /***************************************************************************/
  /**************************** Sending methods ******************************/
  /***************************************************************************/

  /**
   * This method sents an simulated ethernet frame over the simulated network.
   * @param frame The ethernet frame to send
   */
  public void sendFrame(Ethernet frame) {
    if (portNum == -1) {
      return;
    }
    DatagramPacket packet;
    try {
      packet = new DatagramPacket(new byte[1500], 1500, InetAddress.getByName(address), portNum);      
      packet.setData(frame.getBytes());
      socket.send(packet);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }
  }

  /**
   * Send a dhcp discover to the router to determine what my IP address is as 
   * well as my default gateway
   */
  public void sendDHCPDiscover() {
    byte[] packetDHCP = generateDHCPDiscoverPacket();
    UDP packetUDP = new UDP(DHCP.SERVER_PORT, DHCP.CLIENT_PORT, packetDHCP);
    IP packetIP = new IP(IP.broadcastIP, addressIP, ipIdentifier++, IP.UDP_PORT, packetUDP.getBytes());
    Ethernet frame = new Ethernet(Ethernet.BROADCASTMAC, addressMAC, Ethernet.IP_PORT, packetIP.getBytes());
    sendFrame(frame);
  }
  
  /**
   * Send a udp message to an IP address
   * @param ip The IP address of the intended receiver
   * @param message The message to be sent
   */
  public void udpSend(byte[] ip, String message) {
    UDP udpPacket = new UDP(UDP.MESSAGE_PORT, UDP.MESSAGE_PORT, message.getBytes());
    IP ipPacket = new IP(ip, addressIP, ipIdentifier++, IP.UDP_PORT, udpPacket.getBytes());
    if (IP.sameNetwork(addressIP, ip)) {
      // Do not send frame to router
      byte[] destinationMAC = getMAC(ipPacket.destination());
      if (destinationMAC == null) {
        return;
      }
      Ethernet frame = new Ethernet(destinationMAC, addressMAC, Ethernet.IP_PORT, ipPacket.getBytes());
      sendFrame(frame);
    } else {
      // Send frame to router
      byte[] routerMAC = getMAC(routerIP);
      if (routerMAC == null) {
        return;
      }
      Ethernet frame = new Ethernet(routerMAC, addressMAC, Ethernet.IP_PORT, ipPacket.getBytes());
      sendFrame(frame);
    }
  }
  
  /**
   * Send a UDP message to a specified IP address
   * @param ipString The String that needs to be converted into a byte[]
   * @param message The message to be sent
   */
  public void udpSend(String ipString, String message) {
	  ipString = ipString.strip();
	  if (ipString.equals("router")) {
		  if (routerIP != null) {
			  ping(routerIP);
		  } else {
			  System.err.println("Router IP not yet set");
			  return;
		  }
	  } else {
		  String[] ipStringArray = ipString.split("[.]");
		  if (ipStringArray.length != 4) {
			  System.err.println("Incorrect IP format");
			  return;
		  }
		  byte[] ip = new byte[4];
  
		  for (int i = 0; i < 4; i++) {
			  try {
				  ip[i] = (byte) Integer.parseInt(ipStringArray[i]);
			  } catch (Exception e) {
				  e.printStackTrace();
				  return;
			  }
		  }
		  udpSend(ip, message);
	  }
  }
  
  /**
   * Send a TCP message to an IP address
   * @param ip The IP address to send the message to
   * @param message The message to send
   */
  public void tcpSend(byte[] ip, String message) {

    if (!sendSyn(ip)) {
      System.err.println("Failed to receive ACK");
      return;
    }
    boolean LAN = IP.sameNetwork(ip, addressIP);
    byte[] destMAC = LAN ? getMAC(ip) : getMAC(routerIP);
    TCP tcpPacket = new TCP(TCP.MESSAGE_PORT, TCP.MESSAGE_PORT, TCP.PUSH, message.getBytes());
    IP ipPacket = new IP(ip, addressIP, ipIdentifier++, IP.TCP_PORT, tcpPacket.getBytes());
    Ethernet frame = new Ethernet(destMAC, addressMAC, Ethernet.IP_PORT, ipPacket.getBytes());
    ack = false;
    for (int i = 0; i < 2; i++) {
      try {
        sendFrame(frame);
        Thread.sleep(TIMEOUT);
        if (ack)
          break;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
  
  /**
   * Send a TCP message to an IP address
   * @param ipString The IP address that needs to be converted to a byte[]
   * @param message The message to send
   */
  public void tcpSend(String ipString, String message) {
	  ipString = ipString.strip();
	  if (ipString.equals("router")) {
		  if (routerIP != null) {
			  ping(routerIP);
		  } else {
			  System.err.println("Router IP not yet set");
			  return;
		  }
	  } else {
		  String[] ipStringArray = ipString.split("[.]");
		  if (ipStringArray.length != 4) {
			  System.err.println("Incorrect IP format");
			  return;
		  }
		  byte[] ip = new byte[4];
  
		  for (int i = 0; i < 4; i++) {
			  try {
				  ip[i] = (byte) Integer.parseInt(ipStringArray[i]);
			  } catch (Exception e) {
				  e.printStackTrace();
				  return;
			  }
		  }
		  tcpSend(ip, message);
	  }
  }

  /**
   * Sends a syn message to a TCP receiver to check whether or not a connection
   * can be established
   * @param ip The IP address to send the SYN to
   * @return Whether or not an ACK was received
   */
  private boolean sendSyn(byte[] ip) {
    boolean LAN = IP.sameNetwork(ip, addressIP);
    byte[] destMAC;
    if (!LAN) {
      destMAC = getMAC(routerIP);
    } else {
      destMAC = getMAC(ip);
    }
    if (destMAC == null) {
      return false;
    }
    
    TCP syn = new TCP(TCP.MESSAGE_PORT, TCP.MESSAGE_PORT, TCP.SYN, new byte[0]);
    IP ipPacket = new IP(ip, addressIP, ipIdentifier++, IP.TCP_PORT, syn.getBytes());
    Ethernet frame = new Ethernet(destMAC, addressMAC, Ethernet.IP_PORT, ipPacket.getBytes());
    ack = false;
    int i = 0;
    for (; i < 2; i++) {
      try {
        sendFrame(frame);
        Thread.sleep(TIMEOUT*5);
        if (ack)
          break;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (i == 2) {
      return false;
    } else {
      return true;
    }
  }

  /**
   * The TCP ack that is sent to the source after a TCP message has been 
   * successfully received
   * @param ip The IP address to send the ACK to
   * @param packet The TCP packet that was received
   */
  private void sendACK(byte[] ip, TCP packet) {
    TCP tcpPacket = new TCP(packet.destinationPort(), packet.sourcePort(), TCP.ACK, packet.payload());
    IP ipPacket = new IP(ip, addressIP, ipIdentifier++, IP.TCP_PORT, tcpPacket.getBytes());
    boolean LAN = IP.sameNetwork(ip, addressIP);
    byte[] destMAC = LAN ? getMAC(ip) : getMAC(routerIP);
    Ethernet frame = new Ethernet(destMAC, addressMAC, Ethernet.IP_PORT, ipPacket.getBytes());
    sendFrame(frame);
  }

  /**
   * Send an ARP request to the specified IP address to resolved its
   * physical hardware address
   * @param destIP The IP address to be resolved to a MAC address
   */
  private void sendRequestARP(byte[] destIP) {
	  ARP arpPacket = new ARP(ARP.ARP_REQUEST, addressMAC, addressIP, ARP.zeroMAC, destIP);
	  Ethernet frame = new Ethernet(ARP.broadcastMAC, addressMAC, ARP.DEMUX_PORT, arpPacket.getBytes());
	  sendFrame(frame);
  }

  /**
   * Used to send an ARP response when an ARP request is received
   * @param destMAC The MAC of the host that sent the ARP request
   * @param destIP The IP of the host that sent the ARP request
   */
  private void sendResponseARP(byte[] destMAC, byte[] destIP) {
	  ARP arpPacket = new ARP(2, addressMAC, addressIP, destMAC, destIP);
    Ethernet frame = new Ethernet(destMAC, addressMAC, Ethernet.ARP_PORT, arpPacket.getBytes());
	  sendFrame(frame);
  }

  /**
   * Used to ping a host on the LAN
   * @param ip The IP of the host to ping
   */
  public void ping(byte[] ip) {
    ICMP ping = new ICMP(ICMP.PING_REQ, icmpID++, new byte[1]);
    IP ipPacket = new IP(ip, addressIP, ipIdentifier++, IP.ICMP_PORT, ping.getBytes());
    byte[] mac;
    if (!IP.sameNetwork(ip, addressIP)) {
      mac = getMAC(routerIP);
    } else {
      mac = getMAC(ip);
      if (mac == null) {
        return;
      }      
    }
    Ethernet frame = new Ethernet(mac, addressMAC, Ethernet.IP_PORT, ipPacket.getBytes());
    sendFrame(frame);
  }

  /**
   * Used to ping a host on the LAN
   * @param ipString The IP of the host to be converted to a byte[]
   */
  public void ping(String ipString) {
    ipString = ipString.strip();
    if (ipString.equals("router")) {
      if (routerIP != null) {
        ping(routerIP);
      } else {
        System.err.println("Router IP not yet set");
        return;
      }
    } else {
      String[] ipStringArray = ipString.split("[.]");
      if (ipStringArray.length != 4) {
        System.err.println("Incorrect IP format");
        return;
      }
      byte[] ip = new byte[4];
      
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
  }

  /**
   * Used to resolved an  IP address into a MAC address
   * @param ip The IP address that needs to be resolved
   * @return The MAC address of the corresponding IP address or null if it
   * cannot be resolved
   */
  public byte[] getMAC(byte[] ip) {

    boolean hasIP = arpTable.containsMAC(ip);
    int i = 0;
    int tries = 5;
    for (; i < tries; i++) {
      if (hasIP)
        break;
      System.out.println("Sent ARP request");
      sendRequestARP(ip);
      try {
        Thread.sleep(TIMEOUT);
      } catch (Exception e) {
        e.printStackTrace();
      }
      hasIP = arpTable.containsMAC(ip);
    }
    if (i == tries) {
      System.out.println(String.format("Could not resolve IP: %s to physical address", IP.ipString(ip)));
      return null;
    }
    return arpTable.getMAC(ip);
  }

  /**
   * This method is used to in DHCPDiscover method to help generate the
   * DHCP request and then turn it into a byte array
   * @return The byte array that represents the DHCP request
   */
  public byte[] generateDHCPDiscoverPacket() {
    return DHCP.bootRequest(ipIdentifier++, addressMAC).getBytes();
  }

  /***************************************************************************/
  /****************************** Other methods ******************************/
  /***************************************************************************/

  /**
   * Used to take all user inputs from the console and to execute various 
   * commands accordingly
   */
  public void handleUserInputs() {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
      String line = "";
      while ( (line = reader.readLine()) != null) {
        System.out.println();
        if (line.split(" ")[0].equals("ping")) {
          ping(line.split(" ")[1]);
        } else if (line.equals("shutdown")) {
          System.out.println("Shutting down client...");
          System.exit(0);
        } else if (line.equals("status")) {
          System.out.println(toString());
        } else if (line.equals("connect to router")) {
          if (portNum != -1) {
            System.out.println("Already connected to router");
            continue;
          }
          System.out.print("Enter internal interface number: ");
          line = reader.readLine();
          try {
            portNum = Integer.parseInt(line);
            new Thread() {
              @Override
              public void run() {
                handleInterface();
              }
            }.start();
            Thread.sleep(100);
            sendDHCPDiscover();
          } catch (Exception e) {
            e.printStackTrace();
          }
        } else if (line.equals("udp send")) {
          System.out.print("IP: ");
          String ipString = reader.readLine();
          System.out.print("message: ");
        	String message = reader.readLine();
        	udpSend(ipString, message);
          System.out.println();
        } else if (line.equals("tcp send")) {
            System.out.print("IP: ");
            String ipString = reader.readLine();
            System.out.print("message: ");
          	String message = reader.readLine();
          	tcpSend(ipString, message);
            System.out.println();
        } else if (line.equals("disconnect")) {
        	removeIP();
        	setIPNil();
        	setGatewayNil();
          socket = null;
        	portNum = -1;
        } else if (line.equals("arp table")) { 
        	System.out.println(arpTable.toString());
        } else {
          System.out.println("Unknown command");
        }
      }      
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  /**
   * Sets the clients IP to the nil IP
   */
  public void setIPNil() {
	  for (int i = 0; i < 4; i++) {
		  addressIP[i] = (byte) 0;
	  }
  }
  
  /**
   * Sets the clients gateway to nil
   */
  public void setGatewayNil() {
	  for (int i = 0; i < 4; i++) {
		  routerIP[i] = (byte) 0;
	  }
  }
  
  /**
   * Used to disconnect from the router so that the IP is back in the DHCP pool
   */
  public void removeIP() {
	  DHCP dhcp = new DHCP(DHCP.ADDRESS_RELEASE, 1, new byte[6]);
	  UDP udpPacket = new UDP(UDP.RELEASE_PORT, UDP.DHCP_SERVER, dhcp.getBytes());
      IP ipPacket = new IP(routerIP, addressIP, ipIdentifier++, IP.UDP_PORT, udpPacket.getBytes());
      byte[] destMAC = getMAC(ipPacket.destination());
      if (destMAC == null) {
    	  return;
      }
      Ethernet frame = new Ethernet(destMAC, addressMAC, Ethernet.IP_PORT, ipPacket.getBytes());
      sendFrame(frame);
  }

  /**
   * @return The String representation of the client object
   */
  public String toString() {
    String routerString = "Not set";
    String subnetString = "Not set";
    String interfaceNum = "Not connected";
    if (routerIP != null) {
      routerString = IP.ipString(routerIP);
    }
    if (subnetMask != null) {
      subnetString = IP.ipString(subnetMask);
    }
    if (socket != null) {
      interfaceNum = String.format("%d", socket.getLocalPort());
    }
    String s = String.format("\nCLIENT toString\n----------------------" + 
      "\nMAC = %s\nIP = %s\nGateway = %s\nSubnet Mask = %s\nConnected to router on interface %s\n", 
      Ethernet.macString(addressMAC), IP.ipString(addressIP), routerString, subnetString, interfaceNum);
    return s;
  }

  /**
   * Sets the port number of the UDP packet over which all communication is 
   * sent
   * @param num
   */
  public void setPortNum(int num) {
    portNum = num;
  }

}

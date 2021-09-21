package nat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class Client {
  private byte[] addressIP = {0, 0, 0, 0};
  private byte[] addressMAC;
  private DatagramSocket socket;
  private DatagramPacket packet;
  private ARPTable arpTable;
  private int ipIdentifier;
  private static byte[] routerIP;
  private static byte[] subnetMask;
  private byte icmpID;
  private int portNum = -1;
  private String address;

  public Client(String address) {
    this.address = address;
    this.addressMAC = Ethernet.generateRandomMAC();
    this.ipIdentifier = 0;
    arpTable = new ARPTable();
    System.out.println("Client started...");
    userInputs();
  }

  public static void main(String[] args) {
    new Client("localhost");
  }
  
  /***************************************************************************/
  /**************************** Handling methods *****************************/
  /***************************************************************************/

  private void handleInterface() {
    try {
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

    if (ipPacket.isBroadcast() || Arrays.equals(addressIP, ipPacket.destination()) || 
      Arrays.equals(addressIP, IP.nilIP)) {

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

  public void sendFrame(Ethernet frame) {
    if (portNum == -1) {
      return;
    }
    try {
      this.packet = new DatagramPacket(new byte[1500], 1500, InetAddress.getByName(address), portNum);      
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }
    this.packet.setData(frame.getBytes());
    try {
      socket.send(this.packet);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

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
    byte[] mac = getMAC(ip);
    if (mac == null) {
      return;
    }
    Ethernet frame = new Ethernet(mac, addressMAC, Ethernet.IP_PORT, ipPacket.getBytes());
    sendFrame(frame);
  }

  public void ping(String ipString) {
    ipString = ipString.strip();
    if (ipString.equals("router")) {
      if (routerIP != null) {
        ping(routerIP);
      } else {
        System.err.println("Router IP not yet set");
        return;
      }
    }
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

  /***************************************************************************/
  /****************************** Other methods ******************************/
  /***************************************************************************/

  public void userInputs() {
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
            this.packet = new DatagramPacket(new byte[1500], 1500, InetAddress.getByName(address), portNum);
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
        } else {
          System.out.println("Unknown command");
        }
      }      
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

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

}

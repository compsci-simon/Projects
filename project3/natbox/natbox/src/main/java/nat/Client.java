package nat;

import java.net.*;
import java.nio.ByteBuffer;

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
  
  public void sendDHCPDiscover() {
    byte[] packetDHCP = generateDHCPDiscoverPacket();
    byte[] packetUDP = encapsulateUDP(68, 67, packetDHCP);
    byte[] packetIP = encapsulateIP(17, IP.broadcastIP, IP.nilIP, packetUDP);
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

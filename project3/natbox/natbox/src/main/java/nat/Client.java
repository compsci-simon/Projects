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
    byte[] packetDHCP = dhcpClient.encapDHCPRequest();
    byte[] packetUDP = encapsulateUDP(68, 67, packetDHCP);
    byte[] ipsrc = {0, 0, 0, 0};
    byte[] ipdest = {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
    byte[] packetIP = encapsulateIP(17, ipsrc, ipdest, packetUDP);
    byte[] frame = encapsulateEthernet(addressMAC, broadcastMAC, packetIP);
    sendFrame(frame);
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

  public byte[] encapsulateIP(int protocol, byte[] sourceAddr, byte[] destAddr, byte[] payload) {
    byte[] header = new byte[20];
    header[0] = 0x45;
    ByteBuffer bb = ByteBuffer.allocate(2);
    System.out.println(payload.length + 20);
    int paylodSize = payload.length + 20;
    // 2 bytes for the paylodSize of the payload
    header[2] = (byte) (paylodSize>>4&0xff);
    header[3] = (byte) (paylodSize&0xff);
    header[4] = (byte) (packetCount>>4&0xff);
    header[5] = (byte) (packetCount&0xff);
    // Flags
    header[6] = 0x00;
    // Fragment Offset
    header[7] = 0x00;
    // TTL
    header[8] = (byte) 0xff;
    // Protocol
    if (protocol == 11) {
      header[9] = 0x11;
    }
    header[9] = 0x11;
    // Header checksum (disabled)
    header[10] = 0x4c;
    header[11] = 0x0d;
    // Source address
    System.arraycopy(sourceAddr, 0, header, 12, 4);
    // Destination address
    System.arraycopy(destAddr, 0, header, 16, 4);

    byte[] ipPacket = new byte[payload.length + 20];
    System.arraycopy(header, 0, ipPacket, 0, 20);
    System.arraycopy(payload, 0, ipPacket, 20, payload.length);

    return ipPacket;
  }

  private byte[] encapsulateUDP(int sourcePort, int destPort, byte[] payload) {
    byte[] udpPacket = new byte[payload.length + 4];
    
    udpPacket[0] = (byte) (sourcePort>>8);
    udpPacket[1] = (byte) (sourcePort&0xff);
    udpPacket[2] = (byte) (destPort>>8);
    udpPacket[3] = (byte) (destPort&0xff);
    System.arraycopy(payload, 0, udpPacket, 4, payload.length);

    return udpPacket;
  }

}

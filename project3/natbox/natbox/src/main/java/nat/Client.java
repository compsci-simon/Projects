package nat;

import java.math.BigInteger;
import java.net.*;
import java.nio.ByteBuffer;

public class Client {
  private boolean internalClient = true;
  private int addressIP = 0x7F000001;
  private long addressMAC;
  private DatagramSocket socket;
  private DatagramPacket packet;
  private int transactionIdentifier;
  private DHCPClient dhcpClient;
  private int packetCount;

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

  private static long generateRandomMAC() {
    long mac = 0;
    for (int i = 0; i < 6; i++) {
      mac = mac<<8;
      mac = mac | (int) (Math.random()*0xFF);
    }
    return mac;
  }

  private static int generateRandomIP() {
    int addressIP = 0;
    for (int i = 0; i < 4; i++) {
      addressIP = addressIP<<8;
      addressIP = addressIP | (int) (Math.random()*0xFF);
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
    byte[] packetUDP = encapsulateEthernet(68, 67, packetDHCP);
    byte[] packetIP = encapsulateIP(17, 0, 0xffffffff, packetUDP);
    byte[] frame = encapsulateEthernet(addressMAC, Long.decode("0xffffffffffff"), packetIP);
    sendFrame(frame);
  }

  public byte[] encapsulateEthernet(long sourceAddr, long destAddr, byte[] payload) {
    byte[] header = new byte[14];
    ByteBuffer bb = ByteBuffer.allocate(8);
    bb.putLong(sourceAddr);
    System.arraycopy(bb.array(), 2, header, 0, 6);
    bb.putLong(destAddr);
    System.arraycopy(bb.array(), 2, header, 6, 6);
    header[12] = (byte) 0x80;
    header[13] = 0x00;
    
    byte[] frame = new byte[14 + payload.length];
    System.arraycopy(header, 0, frame, 0, header.length);
    System.arraycopy(payload, 0, frame, header.length, payload.length);

    return frame;
  }

  public byte[] encapsulateIP(int protocol, int sourceAddr, int destAddr, byte[] payload) {
    byte[] header = new byte[20];
    header[0] = 0x45;
    ByteBuffer bb = ByteBuffer.allocate(2);
    bb.putInt(328);
    System.arraycopy(bb.array(), 0, header, 2, 2);
    bb.putInt(packetCount);
    System.arraycopy(bb.array(), 0, header, 4, 2);
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
    bb = ByteBuffer.allocate(4);
    bb.putInt(sourceAddr);
    System.arraycopy(bb.array(), 0, header, 12, 4);
    // Destination address
    bb.putInt(destAddr);
    System.arraycopy(bb.array(), 0, header, 16, 4);

    byte[] ipPacket = new byte[payload.length + 20];
    System.arraycopy(header, 0, ipPacket, 0, 20);
    System.arraycopy(payload, 0, ipPacket, 20, payload.length);

    return ipPacket;
  }

  private byte[] encapsulateUDP(int sourcePort, int destPort, byte[] payload) {
    byte[] packet = new byte[payload.length + 4];
    
    ByteBuffer bb = ByteBuffer.allocate(2);
    bb.putInt(sourcePort);
    System.arraycopy(bb.array(), 0, packet, 0, 2);
    bb.putInt(destPort);
    System.arraycopy(bb.array(), 0, packet, 2, 2);
    System.arraycopy(payload, 0, packet, 4, payload.length);
    return packet;
  }

}

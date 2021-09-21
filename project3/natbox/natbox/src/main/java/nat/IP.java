package nat;

import java.util.Arrays;

public class IP {
  public static byte[] broadcastIP = {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
  public static byte[] relayIP = {0, 0, 0, 0};
  public static byte[] nilIP = {0, 0, 0, 0};
  public static final byte UDP_PORT = 17;
  public static final byte ICMP_PORT = 1;
  private byte[] destIP;
  private byte[] sourceIP;
  private int identifier;
  private int demuxPort;
  private byte[] payload;
  private int totalLength;

  public IP (byte[] destIP, byte[] sourceIP, int identifier, int demuxPort, byte[] payload) {
    if (destIP.length != 4 || sourceIP.length != 4) {
      System.err.println("Incorrect IP format!");
      return;
    }
    this.destIP = destIP;
    this.sourceIP = sourceIP;
    this.identifier = identifier;
    this.demuxPort = demuxPort;
    this.payload = payload;
  }

  public IP (byte[] packet) {
    this.totalLength = (packet[2]<<8) | (packet[3]&0xff);
    this.identifier = ((packet[4]&0xff)<<8) | (packet[5]&0xff);
    this.demuxPort = packet[9];
    this.destIP = new byte[4];
    this.sourceIP = new byte[4];
    this.payload = new byte[this.totalLength - 20];
    System.arraycopy(packet, 16, destIP, 0, 4);
    System.arraycopy(packet, 12, sourceIP, 0, 4);
    System.arraycopy(packet, 20, payload, 0, this.totalLength - 20);
  }

  public byte[] getBytes() {
    byte[] header = new byte[20];
    header[0] = 0x45;
    int payloadSize = payload.length + 20;
    // 2 bytes for the payloadSize of the payload
    header[2] = (byte) ((payloadSize>>8)&0xff);
    header[3] = (byte) (payloadSize&0xff);
    // 2 bytes for the packet identifier
    header[4] = (byte) ((identifier>>8)&0xff);
    header[5] = (byte) (identifier&0xff);
    // Flags
    header[6] = 0x00;
    // Fragment Offset
    header[7] = 0x00;
    // TTL
    header[8] = (byte) 0xff;
    // Protocol
    header[9] = (byte) (demuxPort&0xff);
    // Header checksum (disabled)
    header[10] = 0x4c;
    header[11] = 0x0d;
    // Source address
    System.arraycopy(destIP, 0, header, 16, 4);
    // Destination address
    System.arraycopy(sourceIP, 0, header, 12, 4);

    byte[] ipPacket = new byte[payload.length + 20];
    System.arraycopy(header, 0, ipPacket, 0, 20);
    System.arraycopy(payload, 0, ipPacket, 20, payload.length);

    return ipPacket;
  }
  
  public byte[] source() {
    return sourceIP;
  }

  public byte[] destination() {
    return destIP;
  }

  public int getIdentifier() {
    return identifier;
  }

  public int getDemuxPort() {
    return demuxPort;
  }

  public boolean isBroadcast() {
    return Arrays.equals(destIP, broadcastIP);
  }

  public byte[] payload() {
    return payload;
  }

  public String toString() {
    String s = "IP toString\n----------------------" + 
    "\nDestination IP = ";
    for (int i = 0; i < 4; i++) {
      s = String.format("%s%d.", s, destIP[i]&0xff);
    }
    s = s.substring(0, s.length() - 1);
    s = String.format("%s\nSource IP = ", s);
    for (int i = 0; i < 4; i++) {
      s = String.format("%s%d.", s, sourceIP[i]&0xff);
    }
    s = s.substring(0, s.length() - 1);
    s = String.format("%s\nPacket identifier = %d\nDemux port = %d\n", s, identifier, demuxPort);
    return s;
  }

  public static String ipString(byte[] ip) {
    if (ip.length != 4) {
      System.err.println("Invalid IP format");
      return null;
    }
    String i = String.format("%d.%d.%d.%d", ip[0]&0xff, ip[1]&0xff, ip[2]&0xff, ip[3]&0xff);
    return i;
  }

  public static String ipString(int ipInt) {
    byte[] ip = toBytes(ipInt);
    return String.format("%d.%d.%d.%d", ip[0]&0xff, ip[1]&0xff, ip[2]&0xff, ip[3]&0xff);
  }

  public static int toInt(byte[] ip) {
    if (ip.length != 4) {
      System.err.println("Invalid IP format");
      return -1;
    }
    int temp = 0;
    temp = (ip[0]<<24)&0xff | (ip[1]<<16)&0xff | (ip[2]<<8)&0xff | ip[3]&0xff;
    return temp;
  }

  public static byte[] generateRandomIP() {
    byte[] ip = new byte[4];
    for (int i = 0; i < 4; i++) {
      ip[i] = (byte) ((int)Math.random()*0xff);
    }
    return ip;
  }

  public static boolean sameNetwork(byte[] ipA, byte[] ipB) {
    if (ipA.length != 4 || ipB.length != 4) {
      System.err.println("Incorrect IP format");
      return false;
    }
    byte[] networkA = new byte[3];
    byte[] networkB = new byte[3];

    System.arraycopy(ipA, 0, networkA, 0, 3);
    System.arraycopy(ipB, 0, networkB, 0, 3);

    if (Arrays.equals(networkA, networkB)) {
      return true;
    } else {
      return false;
    }
  }

  public static byte[] toBytes(int ip) {
    byte[] newIP = new byte[4];
    newIP[0] = (byte) ((ip>>24)&0xff);
    newIP[1] = (byte) ((ip>>16)&0xff);
    newIP[2] = (byte) ((ip>>8)&0xff);
    newIP[3] = (byte) (ip&0xff);
    return newIP;
  }

}

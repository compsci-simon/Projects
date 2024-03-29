package nat;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * The IP protocol and all responsibilities and constants assosciated with
 * the protocol
 */
public class IP {
  public static byte[] broadcastIP = {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
  public static byte[] relayIP = {0, 0, 0, 0};
  public static byte[] nilIP = {0, 0, 0, 0};
  public static final byte UDP_PORT = 17;
  public static final byte TCP_PORT = 11;
  public static final byte ICMP_PORT = 1;
  private byte[] destIP;
  private byte[] sourceIP;
  private int identifier;
  private int demuxPort;
  private byte[] payload;
  private int totalLength;

  /**
   * Used to construct a new IP packet
   * @param destIP The destination address
   * @param sourceIP The source address
   * @param identifier The packet ID
   * @param demuxPort The ID of the protocol of the payload
   * @param payload The byte array representation of the encapsulated protocol 
   * packet
   */
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
  
  /**
   * Creates an IP packet from a byte array
   * @param packet The byte array
   */
  public IP (byte[] packet) {
    this.totalLength = (packet[2]<<8) | (packet[3]&0xff);
    this.identifier = ((packet[4]&0xff)<<8) | (packet[5]&0xff);
    this.demuxPort = packet[9];
    this.destIP = new byte[4];
    this.sourceIP = new byte[4];
    this.payload = new byte[this.totalLength - 20];
    System.arraycopy(packet, 16, destIP, 0, 4);
    System.arraycopy(packet, 12, sourceIP, 0, 4);
    try {
      System.arraycopy(packet, 20, payload, 0, this.totalLength - 20);      
    } catch (Exception e) {
      System.out.println(Constants.bytesToString(packet, 6));
      System.out.println("total length = " + totalLength);
      System.out.println("packet.length = " + packet.length);
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    byte[] x = {(byte) 0xC0, (byte) 0xA8, 0, (byte)0xff};
    byte[] y = {23, 23, 23, 23};
    System.out.println(sameNetwork(x, y));
  }

  /**
   * Gets the byte representation of an IP address
   * @return The byte array that represents the IP address
   */
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
  
  /**
   * Getter for the source address
   * @return The source address
   */
  public byte[] source() {
    return sourceIP;
  }
  
  /**
   * Getter for the destination address
   * @return
   */
  public byte[] destination() {
    return destIP;
  }
  
  /**
   * Setter for the source address
   * @param ip The source address
   */
  public void setSource(byte[] ip) {
    if (ip.length != 4) {
      System.err.println("Incorrect IP format");
      return;
    }
    sourceIP = ip;
  }

  /**
   * Setter for the destination address
   * @param ip The destination address
   */
  public void setDest(byte[] ip) {
    if (ip.length != 4) {
      System.err.println("Incorrect IP format");
      return;
    }
    destIP = ip;
  }

  /**
   * Getter for the packet ID
   * @return The packet ID
   */
  public int getIdentifier() {
    return identifier;
  }

  /**
   * Getter for the demux port
   * @return The demux port
   */
  public int getDemuxPort() {
    return demuxPort;
  }

  /**
   * Determines whether a packet is a broadcast
   * @return True if the packet destination is broadcast
   */
  public boolean isBroadcast() {
    return Arrays.equals(destIP, broadcastIP);
  }

  /**
   * Static method to determine with or not a 4 byte IP address is a braodcast
   */
  private static boolean isBroadcast(byte[] ip) {
    return Arrays.equals(ip, broadcastIP);
  }

  /**
   * Getter for the payload
   * @return The payload
   */
  public byte[] payload() {
    return payload;
  }

  /**
   * General toString method
   */
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

  /**
   * Used in a few other toString method and converts an IP address to a string
   * @param ip The IP to convert
   * @return The string representation
   */
  public static String ipString(byte[] ip) {
    if (ip.length != 4) {
      System.err.println("Invalid IP format");
      return null;
    }
    String i = String.format("%d.%d.%d.%d", ip[0]&0xff, ip[1]&0xff, ip[2]&0xff, ip[3]&0xff);
    return i;
  }

  /**
   * Used in a few other toString method and converts an IP address to a string
   * @param ip The IP to convert
   * @return The string representation
   */
  public static String ipString(int ipInt) {
    byte[] ip = toBytes(ipInt);
    return String.format("%d.%d.%d.%d", ip[0]&0xff, ip[1]&0xff, ip[2]&0xff, ip[3]&0xff);
  }

  /**
   * Converts an IP address to an integer
   * @param ip The IP address to convert
   * @return The integer that represents that IP
   */
  public static int toInt(byte[] ip) {
    if (ip.length != 4) {
      System.err.println("Invalid IP format");
      return -1;
    }
    int temp = 0;
    temp = (ip[0]<<24)&0xff | (ip[1]<<16)&0xff | (ip[2]<<8)&0xff | ip[3]&0xff;
    return temp;
  }

  /**
   * Generates a random IP address
   * @return The random IP
   */
  public static byte[] generateRandomIP() {
    byte[] ip = new byte[4];
    for (int i = 0; i < 4; i++) {
      ip[i] = (byte) ((int)(Math.random()*0xff));
      if (i == 1 && (ip[0]&0xff) == 192 && (ip[1]&0xff) == 168) {
        while ((ip[1]&0xff) == 168) {
          ip[i] = (byte) ((int)(Math.random()*0xff));
        }
      }
    }
    return ip;
  }

  /**
   * Determines whether two IP addresses are on the same LAN
   * @param ipA The first IP
   * @param ipB The second IP
   * @return Whether or not they are on the same LAN
   */
  public static boolean sameNetwork(byte[] ipA, byte[] ipB) {
    if (isBroadcast(ipA) || isBroadcast(ipB)) 
      return true;
    if (Arrays.equals(ipA, nilIP) || Arrays.equals(ipB, nilIP))
      return true;
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

  /**
   * Converts an integer to a byte array
   * @param ip The integer
   * @return The byte array
   */
  public static byte[] toBytes(int ip) {
    byte[] newIP = new byte[4];
    newIP[0] = (byte) ((ip>>24)&0xff);
    newIP[1] = (byte) ((ip>>16)&0xff);
    newIP[2] = (byte) ((ip>>8)&0xff);
    newIP[3] = (byte) (ip&0xff);
    return newIP;
  }

  /**
   * Used to determine whether or not an IP is the nil IP
   * @param ip The IP address in question
   * @return True if the IP is nil
   */
  public static boolean isNilIP(byte[] ip) {
    return Arrays.equals(nilIP, ip);
  }
}

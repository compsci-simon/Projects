package nat;

import java.util.Arrays;

/**
 * This class handles all responsibilities assosciated with the ethernet 
 * protocol
 */
public class Ethernet {
  public static final byte[] BROADCASTMAC = {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
  public static final byte[] ZEROMAC = {(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00};
  public static final int IP_PORT = 2048;
  public static final int ARP_PORT = 2054;
  private byte[] payload;
  private byte[] destMAC;
  private byte[] sourceMAC;
  private int demuxProtocol;

  /**
   * Construct a new ethernet frame from scratch
   * @param destMAC Destination MAC address
   * @param sourceMAC Source MAC address
   * @param demuxProtocol Protocol that ethernet frame is encapsulating
   * @param payload The packet that the frame is encapsulating
   */
  public Ethernet(byte[] destMAC, byte[] sourceMAC, int demuxProtocol, byte[] payload) {

    if (destMAC.length != 6 || sourceMAC.length != 6) {
      System.err.println("MAC addresses are 48 bits long");
      return;
    }
    this.destMAC = destMAC;
    this.sourceMAC = sourceMAC;
    this.payload = payload;
    this.demuxProtocol = demuxProtocol;
  }

  /**
   * Create an ethernet frame from a byte array
   * @param frame The byte array that represents an ethernet frame
   */
  public Ethernet(byte[] frame) {
    destMAC = new byte[6];
    sourceMAC = new byte[6];
    this.payload = new byte[frame.length - 14];
    System.arraycopy(frame, 0, destMAC, 0, 6);
    System.arraycopy(frame, 6, sourceMAC, 0, 6);
    demuxProtocol = (frame[12]&0xff)<<8 | (frame[13]&0xff);
    System.arraycopy(frame, 14, payload, 0, frame.length - 14);
  }

  /**
   * Construct a new ethernet frame from scratch
   * @param destMAC Destination MAC address
   * @param sourceMAC Source MAC address
   * @param demuxProtocol Protocol that ethernet frame is encapsulating
   * @param packet The packet that the frame is encapsulating
   */
  public Ethernet(byte[] destMAC, byte[] sourceMAC, int demuxProtocol, IP packet) {

    if (destMAC.length != 6 || sourceMAC.length != 6) {
      System.err.println("MAC addresses are 48 bits long");
      return;
    }
    this.destMAC = destMAC;
    this.sourceMAC = sourceMAC;
    this.payload = packet.payload();
    this.demuxProtocol = demuxProtocol;
  }

  /**
   * Generates a random MAC address
   * @return The MAC address
   */
  public static byte[] generateRandomMAC() {
    byte[] mac = new byte[6];
    for (int i = 0; i < 6; i++) {
      mac[i] = (byte) (Math.random()*0xff);
    }
    return mac;
  }

  /**
   * Converts an Ethernet object into a byte array that can be transported over
   * the network
   * @return The byte array that represents the object
   */
  public byte[] getBytes() {
    byte[] header = new byte[14];
    System.arraycopy(destMAC, 0, header, 0, 6);
    System.arraycopy(sourceMAC, 0, header, 6, 6);
    header[12] = (byte) ((demuxProtocol>>8)&0xff);
    header[13] = (byte) (demuxProtocol&0xff);
    
    byte[] frame = new byte[14 + payload.length];
    System.arraycopy(header, 0, frame, 0, header.length);
    System.arraycopy(payload, 0, frame, header.length, payload.length);

    return frame;
  }

  /**
   * Getter for the destination MAC 
   * @return The destination MAC
   */
  public byte[] destination() {
    return destMAC;
  }

  /**
   * Getter for the source MAC
   * @return The source MAC
   */
  public byte[] source() {
    return sourceMAC;
  }

  /**
   * Getter for the payload of the frame
   * @return The payload of the frame
   */
  public byte[] payload() {
    return payload;
  }

  /**
   * Getter for the demux protocol used to determine the level 3 protocol
   * @return The protocol
   */
  public int protocol() {
    return demuxProtocol;
  }

  /**
   * Determines if the frame is a broadcast frame
   * @return True if it is a broadcast frame
   */
  public boolean isBroadcast() {
    return Arrays.equals(destMAC, BROADCASTMAC);
  }

  /**
   * Returns a string representation of the ethernet frame
   * @return The String representing the Ethernet frame
   */
  public String toString() {
    String s = String.format("\n\t-----------------------------------------\n" + 
    "\t|***************************************|\n" +
    "\t-----------------------------------------\n\n" +
    "Timestamp: %d\nETHERNET toString\n----------------------\nDestination MAC = %s" + 
    "\nSource MAC = %s\nDemux Port = %d\n", 
      System.currentTimeMillis(), macString(destMAC), macString(sourceMAC), demuxProtocol);
    return s;
  }

  /**
   * Used in a few toString methods to convert a MAC address to a string
   * @param mac The MAC address
   * @return The string of the MAC. Hexadecimal bytes seperated by colons
   */
  public static String macString(byte[] mac) {
    if (mac.length != 6) {
      System.err.println("Invalid MAC format");
      return null;
    }
    String m = String.format("%02x:%02x:%02x:%02x:%02x:%02x", mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
    return m;
  }

}

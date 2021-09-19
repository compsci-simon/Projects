package nat;

import java.util.Arrays;

public class IP {
  public static byte[] broadcastIP = {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
  public static byte[] relayIP = {0, 0, 0, 0};
  public static int demuxID;
  private byte[] destIP;
  private byte[] sourceIP;
  private int demuxPort;
  private byte[] payload;
  private int totalLength;

  public IP (byte[] destIP, byte[] sourceIP, int demuxPort, byte[] payload) {
    this.destIP = destIP;
    this.sourceIP = sourceIP;
    this.demuxPort = demuxPort;
    this.payload = payload;
  }

  public IP (byte[] packet) {
    this.totalLength = (packet[2]<<8) | (packet[3]&0xff);
    this.demuxPort = packet[9];
    this.destIP = new byte[4];
    this.sourceIP = new byte[4];
    this.payload = new byte[this.totalLength - 20];
    System.arraycopy(packet, 16, destIP, 0, 4);
    System.arraycopy(packet, 12, sourceIP, 0, 4);
    System.arraycopy(packet, 20, payload, 0, this.totalLength - 20);
  }

  public byte[] getBytes(int packetCount) {
    byte[] header = new byte[20];
    header[0] = 0x45;
    int payloadSize = payload.length + 20;
    // 2 bytes for the payloadSize of the payload
    header[2] = (byte) ((payloadSize>>8)&0xff);
    header[3] = (byte) (payloadSize&0xff);
    header[4] = (byte) (packetCount>>8&0xff);
    header[5] = (byte) (packetCount&0xff);
    // Flags
    header[6] = 0x00;
    // Fragment Offset
    header[7] = 0x00;
    // TTL
    header[8] = (byte) 0xff;
    // Protocol
    if (demuxPort == 11) {
      header[9] = 0x11;
    }
    header[9] = 0x11;
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

  public byte[] destination() {
    return destIP;
  }

  public int demuxPort() {
    return demuxPort;
  }

  public boolean isBroadcast() {
    return Arrays.equals(destIP, broadcastIP);
  }

  public byte[] payload() {
    return payload;
  }

  public String toString() {
    String s = "Destination IP = ";
    for (int i = 0; i < 4; i++) {
      s = String.format("%s%d.", s, destIP[i]&0xff);
    }
    s = s.substring(0, s.length() - 1);
    s = String.format("%s\nSource IP = ", s);
    for (int i = 0; i < 4; i++) {
      s = String.format("%s%d.", s, sourceIP[i]&0xff);
    }
    s = s.substring(0, s.length() - 1);
    s = String.format("%s\nDemux port = %d", s, demuxPort);
    return s;
  }

}

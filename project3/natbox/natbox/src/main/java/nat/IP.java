package nat;

import java.util.Arrays;

public class IP {
  public static byte[] broadcastIP = {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
  public static byte[] nilIP = {0, 0, 0, 0};
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
    this.totalLength = (packet[2]&0xff)<<8 | (packet[3]&0xff);
    this.demuxPort = packet[9];
    for (int i = 0; i < 4; i ++) {
      this.destIP[i] = packet[16 + i];
    }
    for (int i = 0; i < 4; i ++) {
      this.sourceIP[i] = sourceIP[12 + i];
    }
    System.arraycopy(packet, 16, destIP, 0, 4);
    System.arraycopy(packet, 12, destIP, 0, 4);
    System.arraycopy(packet, 20, payload, 0, totalLength - 20);
  }

  public byte[] getBytes(int packetCount) {
    byte[] header = new byte[20];
    header[0] = 0x45;
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
    if (demuxPort == 11) {
      header[9] = 0x11;
    }
    header[9] = 0x11;
    // Header checksum (disabled)
    header[10] = 0x4c;
    header[11] = 0x0d;
    // Source address
    System.arraycopy(destIP, 0, header, 12, 4);
    // Destination address
    System.arraycopy(sourceIP, 0, header, 16, 4);

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
    return payload();
  }

}

package nat;

public class IP {
  public static byte[] broadcastIP = {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
  public static byte[] nilIP = {0, 0, 0, 0};
  private byte[] destIP;
  public byte[] sourceIP;
  public int demuxPort;
  public byte[] payload;

  public IP (byte[] destIP, byte[] sourceIP, int demuxPort, byte[] payload) {
    this.destIP = destIP;
    this.sourceIP = sourceIP;
    this.demuxPort = demuxPort;
    this.payload = payload;
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
}

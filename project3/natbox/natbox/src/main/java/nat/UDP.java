package nat;

public class UDP {
  public static final int DEMUXPORT = 17;
  private int destPort;
  private int sourcePort;
  private byte[] payload;

  public UDP(byte[] packet) {
    this.sourcePort = (packet[0]&0xff)<<8 | packet[1]&0xff;
    this.destPort = (packet[2]&0xff)<<8 | packet[3]&0xff;
    this.payload = new byte[packet.length - 4];
    System.arraycopy(packet, 4, this.payload, 0, this.payload.length);
  }

  public UDP(int destPort, int sourcePort, byte[] payload) {
    this.destPort = destPort;
    this.sourcePort = sourcePort;
    this.payload = payload;
  }

  public byte[] getBytes() {
    byte[] udpPacket = new byte[payload.length + 4];
    
    udpPacket[0] = (byte) (sourcePort>>8);
    udpPacket[1] = (byte) (sourcePort&0xff);
    udpPacket[2] = (byte) (destPort>>8);
    udpPacket[3] = (byte) (destPort&0xff);
    System.arraycopy(payload, 0, udpPacket, 4, payload.length);
    
    return udpPacket;
  }

  public int demuxPort() {
    return destPort;
  }

  public byte[] payload() {
    return payload;
  }

  public int destinationPort() {
    return destPort;
  }

  public String toString() {
    String s = String.format("UDP toString\n----------------------" + 
      "\nDest port = %d\nSource port = %d\n", destPort, sourcePort);
    return s;
  }

}

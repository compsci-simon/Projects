package nat;

public class UDP {
  private int destPort;
  private int sourcePort;
  private byte[] payload;

  public UDP(byte[] packet) {
    this.destPort = (packet[0]&0xff)<<8 | packet[1]&0xff;
    this.sourcePort = (packet[2]&0xff)<<8 | packet[3]&0xff;
    this.payload = new byte[packet.length - 4];
    System.arraycopy(packet, 4, this.payload, 0, this.payload.length);
  }

  public int demuxPort() {
    return destPort;
  }

  public byte[] payload() {
    return payload;
  }
}

package nat;

public class ICMP {
  public static final byte PING_REQ = 0x08;
  public static final byte PING_RES = 0x00;
  private byte type;
  private byte identifier;
  private byte[] data;

  public ICMP(byte type, byte identifier, byte[] data) {
    this.type = type;
    this.identifier = identifier;
    this.data = data;
  }

  public ICMP(byte[] packet) {
    this.type = packet[0];
    this.identifier = packet[1];
    this.data = new byte[packet.length - 2];
    System.arraycopy(packet, 2, this.data, 0, data.length);
  }

  public static ICMP pingResponse(ICMP req) {
    req.setType(PING_RES);
    return req;
  }

  public void setType(byte type) {
    this.type = type;
  }

  public byte[] getBytes() {
    byte[] packet = new byte[data.length + 2];
    packet[0] = type;
    packet[1] = identifier;
    System.arraycopy(data, 0, packet, 2, data.length);
    return packet;
  }

  public int getType() {
    return type;
  }

  public int getIdentifier() {
    return identifier;
  }

  public byte[] getData() {
    return data;
  }

}

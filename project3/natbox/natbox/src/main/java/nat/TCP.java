package nat;

public class TCP {
  private int destPort;
  private int sourcePort;
  private int seqNum;
  private int ackNum;
  private byte[] payload;

  public TCP(byte[] packet) {

    this.destPort = (packet[0]&0xff)<<8 | packet[1]&0xff;
    this.sourcePort = (packet[2]&0xff)<<8 | packet[3]&0xff;
    this.seqNum = (packet[4]&0xff)<<24 | (packet[5]&0xff)<<16 | (packet[6]&0xff)<<8 | (packet[7]&0xff)
    this.seqNum = (packet[8]&0xff)<<24 | (packet[9]&0xff)<<16 | (packet[10]&0xff)<<8 | (packet[11]&0xff)
    this.payload = new byte[packet.length - 12];
    System.arraycopy(packet, 12, this.payload, 0, this.payload.length);
  }

  public TCP(int destPort, int sourcePort, byte[] payload) {
    this.destPort = destPort;
    this.sourcePort = sourcePort;
    this.payload = payload;
  }

  public byte[] getBytes() {
    byte[] tcpPacket = new byte[payload.length + 4];
    
    tcpPacket[0] = (byte) (sourcePort>>8);
    tcpPacket[1] = (byte) (sourcePort&0xff);
    tcpPacket[2] = (byte) (destPort>>8);
    tcpPacket[3] = (byte) (destPort&0xff);
    tcpPacket[4] = (byte) (seqNum>>24);
    tcpPacket[5] = (byte) (seqNum>>16);
    tcpPacket[6] = (byte) (seqNum>>8);
    tcpPacket[7] = (byte) (seqNum&0xff);
    tcpPacket[8] = (byte) (ackNum>>24);
    tcpPacket[9] = (byte) (ackNum>>16);
    tcpPacket[10] = (byte) (ackNum>>8);
    tcpPacket[11] =  (byte) (ackNum&0xff);

    System.arraycopy(payload, 0, tcpPacket, 12, payload.length);
    
    return tcpPacket;
  }

  public int demuxPort() {
    return destPort;
  }

  public byte[] payload() {
    return payload;
  }
}
package nat;

public class TCP {
  private int destPort;
  private int sourcePort;
  private int seqNum;
  private int ackNum;
  private byte type;
  private byte[] payload;
  public static final int MESSAGE_PORT = 6001;
  public static final byte SYN = 1;
  public static final byte ACK = 2;
  public static final byte PUSH = 3;

  public TCP(int destPort, int sourcePort, byte type, byte[] payload) {
    this.destPort = destPort;
    this.sourcePort = sourcePort;
    this.type = type;
    this.payload = payload;
  }

  public TCP(byte[] packet) {

    this.destPort = (packet[0]&0xff)<<8 | packet[1]&0xff;
    this.sourcePort = (packet[2]&0xff)<<8 | packet[3]&0xff;
    this.seqNum = (packet[4]&0xff)<<24 | (packet[5]&0xff)<<16 | (packet[6]&0xff)<<8 | (packet[7]&0xff);
    this.ackNum = (packet[8]&0xff)<<24 | (packet[9]&0xff)<<16 | (packet[10]&0xff)<<8 | (packet[11]&0xff);
    this.type = packet[12];
    this.payload = new byte[packet.length - 13];
    System.arraycopy(packet, 13, this.payload, 0, this.payload.length);
  }

  public byte[] getBytes() {
    byte[] tcpPacket = new byte[payload.length + 13];
    
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
    tcpPacket[12] =  type;

    System.arraycopy(payload, 0, tcpPacket, 13, payload.length);
    
    return tcpPacket;
  }

  public int destinationPort() {
    return destPort;
  }

  public int sourcePort() {
    return sourcePort;
  }

  public byte getType() {
    return type;
  }

  public byte[] payload() {
    return payload;
  }
  
  public String toString() {
    String stringType = "None";
    if (this.type == ACK) {
      stringType = "Ack";
    } else if (this.type == SYN) {
      stringType = "Syn";
    } else if (this.type == PUSH) {
      stringType = "Push";
    }
	    String s = String.format("TCP toString\n----------------------" + 
	      "\nDest port = %d\nSource port = %d\nType = %s\n", destPort, sourcePort, stringType);
	    return s;
	  }
}
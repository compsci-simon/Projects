package nat;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class DHCP {
  public static int bootRequest = 1;
  public static int bootReply = 2;
  public static int ethernet = 1;
  private static byte optionDHCPMessageType = 0x35;
  private static byte DHCPRequest = 0x03;
  private static byte parameterRequestList = 0x37;
  private static byte requestListItemSubnet = 0x01;
  private static byte requestListItemRouter = 0x03;
  private static byte requestListItemDNS = 0x06;
  private static byte hostnameOption = 0x0c;
  public static int serverPort = 67;
  private int messageType;
  private int hardwareType;
  private int hardwareAddrLen;
  private int hops;
  private int transactionID;
  private int secondsElapsed;
  private int bootpFlags;
  private byte[] ciaddr;
  private byte[] yiaddr;
  private byte[] siaddr;
  private byte[] giaddr;
  private byte[] chaddr;
  private String hostname;
  private ArrayList<Byte> options;

  public DHCP(byte[] packet) {
    this.messageType = packet[0]&0xff;
    this.hardwareType = packet[1]&0xff;
    this.hardwareAddrLen = packet[2]&0xff;
    this.hops = packet[3]&0xff;
    this.transactionID = (packet[4]&0xff)<<24 | (packet[5]&0xff)<<16 | (packet[6]&0xff)<<8 |
      packet[7]&0xff;
    this.secondsElapsed = (packet[8]&0xff)<<8 | (packet[9]&0xff);
    this.bootpFlags = (packet[10]&0xff)<<8 | (packet[11]&0xff);
    this.ciaddr = new byte[4];
    System.arraycopy(packet, 12, ciaddr, 0, 4);
    this.yiaddr = new byte[4];
    System.arraycopy(packet, 16, yiaddr, 0, 4);
    this.siaddr = new byte[4];
    System.arraycopy(packet, 20, siaddr, 0, 4);
    this.giaddr = new byte[4];
    System.arraycopy(packet, 24, giaddr, 0, 4);
    this.chaddr = new byte[6];
    System.arraycopy(packet, 28, chaddr, 0, 6);
  }


  public int getMessageType() {
    return messageType;
  }

  public void setMessageType(int messageType) {
    this.messageType = messageType;
  }

  public byte[] toBytes() {
    ArrayList<Byte> packetBytes = new ArrayList<Byte>();
    // Setting operation code
    packetBytes.add((byte) (messageType&0xff));
    // Setting hardware address type
    packetBytes.add((byte) 0x01);
    // Setting hardware address length
    packetBytes.add((byte) 0x06);
    // Setting number of hops that have occured to 0
    packetBytes.add((byte) 0x00);
    // Setting the transaction identifier for the packet
    packetBytes.add((byte) ((transactionID>>24)&0xff));
    packetBytes.add((byte) ((transactionID>>16)&0xff));
    packetBytes.add((byte) ((transactionID>>8)&0xff));
    packetBytes.add((byte) ((transactionID)&0xff));
    // Setting the seconds that have elapsed
    packetBytes.add((byte) 0x00);
    packetBytes.add((byte) 0x00);
    // Setting the Bootp flags bytes to 0 for unicast
    packetBytes.add((byte) 0x00);
    packetBytes.add((byte) 0x00);
    // ciAddress
    packetBytes.add(ciaddr[0]);
    packetBytes.add(ciaddr[1]);
    packetBytes.add(ciaddr[2]);
    packetBytes.add(ciaddr[3]);
    // yiAddress
    packetBytes.add(yiaddr[0]);
    packetBytes.add(yiaddr[1]);
    packetBytes.add(yiaddr[2]);
    packetBytes.add(yiaddr[3]);
    // siAddress
    packetBytes.add(siaddr[0]);
    packetBytes.add(siaddr[1]);
    packetBytes.add(siaddr[2]);
    packetBytes.add(siaddr[3]);
    // giAddress
    packetBytes.add(giaddr[0]);
    packetBytes.add(giaddr[1]);
    packetBytes.add(giaddr[2]);
    packetBytes.add(giaddr[3]);
    // chAddress
    packetBytes.add(chaddr[0]);
    packetBytes.add(chaddr[1]);
    packetBytes.add(chaddr[2]);
    packetBytes.add(chaddr[3]);
    packetBytes.add(chaddr[4]);
    packetBytes.add(chaddr[5]);
    // Hardware address padding
    for (int i = 0; i < 10; i++) {
      packetBytes.add((byte) 0x00);
    }
    // Server host name
    for (int i = 0; i < 64; i++) {
      packetBytes.add((byte) 0x00);
    }
    // Boot file name
    for (int i = 0; i < 128; i++) {
      packetBytes.add((byte) 0x00);
    }
    // Options

    // Boot request options
    if (this.messageType == DHCP.bootRequest) {
      // Option (53) message type
      packetBytes.add(DHCP.optionDHCPMessageType);
      packetBytes.add((byte) 0x01);
      packetBytes.add(DHCP.DHCPRequest);
      // Option (54) parameters
      packetBytes.add(DHCP.parameterRequestList);
      packetBytes.add(DHCP.requestListItemSubnet);
      packetBytes.add(DHCP.requestListItemRouter);
      packetBytes.add(DHCP.requestListItemDNS);
      // Option (12) hostname
      if (this.hostname != null) {
        packetBytes.add(DHCP.hostnameOption);
        byte[] hostnameBytes = this.hostname.getBytes();
        packetBytes.add((byte) (hostnameBytes.length&0xff));
        for (Byte b : hostnameBytes) {
          packetBytes.add(b);
        }
      }
    }

    // Boot reply options
    if (this.messageType == DHCP.bootReply) {

    }
    
    int length = packetBytes.size();
    byte[] packetB = new byte[length];
    for (int i = 0; i < length; i++) {
      packetB[i] = packetBytes.get(i);
    }
    return packetB;
  }

  public static byte[] bootRequest(int transactionIdentifier, byte[] addressMAC) {
    return createPacket(DHCP.bootRequest, transactionIdentifier, addressMAC);
  }

  private static byte[] createPacket(int opCode, int transactionIdentifier, byte[] addressMAC) {
    byte[] message = new byte[236];
    // Setting operation code
    message[0] = (byte) opCode;
    // Setting hardware type
    message[1] = 0x01;
    // Setting hardware address length
    message[2] = 0x06; // 6 bytes
    // Setting number of hops that have occured to 0
    message[3] = 0x00;
    // Setting the transaction identifier for the packet
    ByteBuffer bb = ByteBuffer.allocate(4);
    bb.putInt(transactionIdentifier);
    byte[] transactionIDByteArray = bb.array();
    System.arraycopy(transactionIDByteArray, 0, message, 4, 4);
    // Setting the seconds that have elapsed
    message[8] = 0x00;
    message[9] = 0x00;
    // Setting the Bootp flags bytes to 0 for unicast
    message[10] = 0x00;
    message[11] = 0x00;
    // y is 0 in the beginning
    message[12] = 0x00;
    message[13] = 0x00;
    message[14] = 0x00;
    message[15] = 0x00;
    // yiAddress is 0 because it needs to be set by the server
    message[16] = 0x00;
    message[17] = 0x00;
    message[18] = 0x00;
    message[19] = 0x00;
    // siAddress is 0 because there is no secondary server
    message[20] = 0x00;
    message[21] = 0x00;
    message[22] = 0x00;
    message[23] = 0x00;
    // giAddress is 0 because there is no secondary server
    message[24] = 0x00;
    message[25] = 0x00;
    message[26] = 0x00;
    message[27] = 0x00;
    // set chaddr to my MAC address
    // 6 byte MAC address
    System.arraycopy(addressMAC, 0, message, 28, 6);

    // Client hardware padding
    for (int i = 0; i < 10; i++) {
      message[34+i] = 0x00;
    }

    // 64 0x00 bytes for server address
    // 128 0x00 bytes for boot file name
    return message;
  }

  public static byte[] createResponse(DHCP packet) {
    return null;
  }
}

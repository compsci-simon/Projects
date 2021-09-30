package nat;

/**
 * The ICMP protocol
 */
public class ICMP {
  public static final byte PING_REQ = 0x08;
  public static final byte PING_RES = 0x00;
  public static final byte ERROR_UNREACHABLE = (byte) 0x03;
  public static final byte ROUTER_SOLICITATION = (byte) 0x85;
  public static final byte ROUTER_ADVERTISEMENT = (byte) 0x86;
  private byte type;
  private byte identifier;
  private byte[] data;

  /**
   * Used to construct an ICMP packet
   * @param type The type of ICMP packet
   * @param identifier The ID of the packet
   * @param data The payload of the ICMP packet
   */
  public ICMP(byte type, byte identifier, byte[] data) {
    this.type = type;
    this.identifier = identifier;
    this.data = data;
  }

  /**
   * Creates an ICMP packet from bytes. Generally on the receiving side
   * to construct from a bytes contained in the IP payload
   * @param packet The payload of the IP packet
   */
  public ICMP(byte[] packet) {
    this.type = packet[0];
    this.identifier = packet[1];
    this.data = new byte[packet.length - 2];
    System.arraycopy(packet, 2, this.data, 0, data.length);
  }

  /**
   * Generates a ping response
   * @param req The ping request
   * @return The ping response
   */
  public static ICMP pingResponse(ICMP req) {
    req.setType(PING_RES);
    return req;
  }
  
  /**
   * Generates an ICMP error message for unreachable hosts
   * @return The ICMP error message
   */
  public static ICMP UnreachableResponse(ICMP req) {
	    req.setType(ERROR_UNREACHABLE);
	    return req;
	  }

  /**
   * Setter for the message type
   * @param type The type to set the message to
   */
  public void setType(byte type) {
    this.type = type;
  }

  /**
   * Used to convert the ICMP message into an encapsulatable object
   * @return The byte array that can be encapsulated in an IP packet
   */
  public byte[] getBytes() {
    byte[] packet = new byte[data.length + 2];
    packet[0] = type;
    packet[1] = identifier;
    System.arraycopy(data, 0, packet, 2, data.length);
    return packet;
  }

  /**
   * Getter for ICMP message type
   * @return Gets the ICMP message
   */
  public byte getType() {
    return type;
  }

  /**
   * Getter for the ICMP ID
   * @return The ID of the message
   */
  public int getIdentifier() {
    return identifier;
  }

  /**
   * Getter for the payload
   * @return The payload
   */
  public byte[] getData() {
    return data;
  }

  /**
   * General toString
   */
  public String toString() {
    String messageType = "";
    switch (type) {
      case PING_REQ:
        messageType = "Ping Request";
        break;
      case PING_RES:
        messageType = "Ping Reply";
        break;
      case ROUTER_ADVERTISEMENT:
        messageType = "Router advertisement";
        break;
      case ROUTER_SOLICITATION:
          messageType = "Router solicitation";
          break;
      case ERROR_UNREACHABLE:
          messageType = "ERROR - Destination unreachable";
        break;
      default:
        break;
    }
    return String.format("ICMP toString\n----------------------" +
      "\nMessage Type: %s\n", messageType);
  }

}

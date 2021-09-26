package nat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;

import java.util.*;

public class DHCP implements Serializable {
  public static final byte BOOT_REQUEST = 1;
  public static final byte BOOT_REPLY = 2;
  public static final byte ADDRESS_RELEASE = 3;
  public static final byte ETHERNET = 1;
  public static final byte OP_MESSAGE_TYPE = 0x35;
  public static final byte OP_SERVER_IP = 0x36;
  public static final byte OP_SUBMASK = 0x01;
  public static final byte OP_END_MARKER = (byte)0xff;
  public static final  byte SERVER_PORT = 67;
  public static final byte CLIENT_PORT = 68;
  private int messageType;
  private int transactionID;
  private byte[] ciaddr;
  private byte[] yiaddr;
  private byte[] siaddr;
  private byte[] giaddr;
  private byte[] chaddr;

  public DHCP(int messageType, int transactionID, byte[] chaddr) {
    this.messageType = messageType;
    this.transactionID = transactionID;
    this.chaddr = chaddr;
    this.ciaddr = new byte[4];
    this.yiaddr = new byte[4];
    this.siaddr = new byte[4];
    this.giaddr = new byte[4];
  }

  public DHCP(byte[] packet) {
    this.messageType = packet[0]&0xff;
    this.transactionID = (packet[4]&0xff)<<24 | (packet[5]&0xff)<<16 | (packet[6]&0xff)<<8 |
      packet[7]&0xff;
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

  public static DHCP bootRequest(int transactionID, byte[] chaddr) {
    return new DHCP(BOOT_REQUEST, transactionID, chaddr);
  }

  public static DHCP bootReply(DHCP bootRequest, byte[] ciaddr, byte[] siaddr) {
    bootRequest.messageType = BOOT_REPLY;
    bootRequest.ciaddr = ciaddr;
    bootRequest.siaddr = siaddr;
    return bootRequest;
  }

  public byte[] serialize() throws Exception {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(bos);
    oos.writeObject(this);
    oos.flush();
    return bos.toByteArray();
  }

  public int getMessageType() {
    return messageType;
  }

  public void setMessageType(int messageType) {
    this.messageType = messageType;
  }

  public byte[] getBytes() {
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
    
    
    int length = packetBytes.size();
    byte[] packetB = new byte[length];
    for (int i = 0; i < length; i++) {
      packetB[i] = packetBytes.get(i);
    }
    return packetB;
  }

  public byte[] getCiaddr() {
    return ciaddr;
  }

  public byte[] getChaddr() {
    return chaddr;
  }

  public void setciaddr(byte[] ip) {
    if (ip == null || ip.length != 4) {
      System.err.println("IP format incorrect");
      return;
    }
    this.ciaddr = ip;
  }

  public void setsiaddr(byte[] ip) {
    if (ip == null || ip.length != 4) {
      System.err.println("IP format incorrect");
      return;
    }
    this.siaddr = ip;
  }

  public byte[] getGateway() {
    return siaddr;
  }

  public String toString() {
    String s = String.format("DHCP toString:\nMessage Type = %d\n" +
                            "ciaddr = %s\nyiaddr = %s\nsiaddr = %s\n" +
                            "giaddr = %s\nchaddr = %s\n", messageType, IP.ipString(ciaddr), 
                            IP.ipString(yiaddr), IP.ipString(siaddr), IP.ipString(giaddr), 
                            Ethernet.macString(chaddr));
    return s;
  }

}

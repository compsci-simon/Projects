package nat;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

import io.netty.buffer.ByteBufOutputStream;

import java.util.*;

public class DHCP implements Serializable {
  public static final byte BOOT_REQUEST = 1;
  public static final byte BOOT_REPLY = 2;
  public static final byte ETHERNET = 1;
  public static final byte OP_MESSAGE_TYPE = 0x35;
  public static final byte OP_SERVER_IP = 0x36;
  public static final byte OP_SUBMASK = 0x01;
  public static final byte OP_END_MARKER = (byte)0xff;
  public static final  byte SERVERPORT = 67;
  public static final byte CLIENTPORT = 68;
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
  }

  public byte[] serialize() throws Exception {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(bos);
    oos.writeObject(this);
    oos.flush();
    return bos.toByteArray();
  }

  public void setOptions(Byte key, Byte[] value) {
    this.options.put(key, value);
  }

  public void setOptions(Byte key, Byte value) {
    Byte[] tmp = { value };
    this.options.put(key, tmp);
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
    // Options

    System.out.println("here ja");
    for (Byte key : this.options.keySet()) {
      packetBytes.add(key);
      packetBytes.add((byte)options.get(key).length);
      for (Byte b : this.options.get(key)) {
        packetBytes.add(b);
      }
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

}

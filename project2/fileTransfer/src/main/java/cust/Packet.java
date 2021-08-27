package cust;

import java.io.Serializable;

public class Packet implements Serializable {
  static final int packetBaseSize = 93;
  private int packetID;
  private boolean sent = false;
  private byte[] payload;

  public Packet(int packetID) {
    this.packetID = packetID;
  }

  public int getPacketID() {
    return packetID;
  }

  public byte[] getPayload() {
    return payload;
  }

  public void setPacketID(int packetID) {
    this.packetID = packetID;
  }

  public void setPayload(byte[] payload) {
    this.payload = payload;
  }

  public boolean isSent() {
    return sent;
  }

  public void send() {
    sent = true;
  }
}

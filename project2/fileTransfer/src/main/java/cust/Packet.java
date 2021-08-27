package cust;

import java.io.Serializable;

public class Packet implements Serializable {
  static final int packetBaseSize = 121;
  private int packetID;
  private int blastNum;
  private int packetStartLoc;
  private byte[] payload;

  public Packet(int packetID, int blastNum, int packetStartLoc) {
    this.packetID = packetID;
    this.packetStartLoc = packetStartLoc;
    this.blastNum = blastNum;
  }

  public int getPacketID() {
    return packetID;
  }

  public int getBlastNum() {
    return blastNum;
  }

  public int getPacketStartLoc() {
    return packetStartLoc;
  }

  public byte[] getPayload() {
    return payload;
  }

  public void setPacketID(int packetID) {
    this.packetID = packetID;
  }

  public void setBlastNum(int blastNum) {
    this.blastNum = blastNum;
  }

  public void setPacketStartLoc(int packetStartLoc) {
    this.packetStartLoc = packetStartLoc;
  }

  public void setPayload(byte[] payload) {
    this.payload = payload;
  }
}

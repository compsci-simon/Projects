package nat;

import java.util.Arrays;

public class Ethernet {
  public static byte[] broadcastMAC = {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
  public static int demuxIP = 2048;
  private byte[] payload;
  private byte[] destMAC;
  private byte[] sourceMAC;
  private int demuxProtocol;

  public Ethernet(byte[] destMAC, byte[] sourceMAC, int demuxProtocol, byte[] payload) {
    if (destMAC.length != 6 || sourceMAC.length != 6) {
      System.err.println("MAC addresses are 48 bits long");
      return;
    }
    this.destMAC = destMAC;
    this.sourceMAC = sourceMAC;
    this.payload = payload;
    this.demuxProtocol = demuxProtocol;
  }

  public Ethernet(byte[] frame) {
    destMAC = new byte[6];
    sourceMAC = new byte[6];
    this.payload = new byte[frame.length - 14];
    System.arraycopy(frame, 0, destMAC, 0, 6);
    System.arraycopy(frame, 6, sourceMAC, 0, 6);
    demuxProtocol = (frame[12]&0xff)<<8 | (frame[13]&0xff);
    System.arraycopy(frame, 14, payload, 0, frame.length - 14);
  }

  public byte[] getBytes() {
    byte[] header = new byte[14];
    System.arraycopy(destMAC, 0, header, 0, 6);
    System.arraycopy(sourceMAC, 0, header, 6, 6);
    header[12] = (byte) 0x80;
    header[13] = 0x00;
    
    byte[] frame = new byte[14 + payload.length];
    System.arraycopy(header, 0, frame, 0, header.length);
    System.arraycopy(payload, 0, frame, header.length, payload.length);

    return frame;
  }

  public byte[] destination() {
    return destMAC;
  }

  public byte[] payload() {
    return payload;
  }

  public boolean isBroadcast() {
    return Arrays.equals(destMAC, broadcastMAC);
  }

  public String toString() {
    String s = "Destination MAC = ";
    for (int i = 0; i < 6; i++) {
      s = String.format("%s%02x:", s, destMAC[i]&0xff);
    }
    s = s.substring(0, s.length() - 1);
    s = String.format("%s\nSource MAC = ", s);
    for (int i = 0; i < 6; i++) {
      s = String.format("%s%02x:", s, sourceMAC[i]&0xff);
    }
    s = s.substring(0, s.length() - 1);
    return s;
  }

}

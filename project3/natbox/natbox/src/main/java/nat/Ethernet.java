package nat;

public class Ethernet {
  public static byte[] broadcastMAC = {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
  private byte[] payload;
  private byte[] destMAC;
  private byte[] sourceMAC;
  private int demuxProtocol;

  public Ethernet(byte[] frame) {
    System.arraycopy(frame, 0, destMAC, 0, 6);
    System.arraycopy(frame, 6, sourceMAC, 0, 6);
    demuxProtocol = (frame[12]&0xff)<<8 | (frame[13]&0xff);
    System.arraycopy(frame, 14, payload, 0, frame.length - 14);
  }

  public byte[] getBytes(byte[] destMAC, byte[] sourceMAC) {
    byte[] header = new byte[14];
    System.arraycopy(destMAC, 0, header, 6, 6);
    System.arraycopy(sourceMAC, 0, header, 0, 6);
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
    return
  }

}

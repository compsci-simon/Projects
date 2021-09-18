package nat;

public class Ethernet {
  public static byte[] broadcastAddr = {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
  private byte[] payload;

  public Ethernet(byte[] payload) {
    this.payload = payload;
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
  
}

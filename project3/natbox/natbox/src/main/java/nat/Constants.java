package nat;

public class Constants {
  public static byte[] broadcastIP = {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
  public static byte[] broadcastMAC = {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
  public static int demuxPortUDP = 17;
  public static int demuxPortTCP = 17;
  public static int routerDHCPPort = 67;
  public static int demuxPortDHCP = 68;
  public static int mTUEthernet = 17;

  public static Byte[] bToB(byte[] b) {
    Byte[] B = new Byte[b.length];
    for (int i = 0; i < b.length; i++) {
      B[i] = Byte.valueOf(b[i]);
    }
    return B;
  }
}

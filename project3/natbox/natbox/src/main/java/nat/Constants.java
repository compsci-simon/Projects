package nat;

/**
 * This class defines constants and tools that are useful in various other 
 * classes but are not specific to that class
 */
public class Constants {
  public static byte[] broadcastIP = {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
  public static byte[] broadcastMAC = {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
  public static int demuxPortUDP = 17;
  public static int demuxPortTCP = 17;
  public static int routerDHCPPort = 67;
  public static int demuxPortDHCP = 68;
  public static int mTUEthernet = 17;

  /**
   * Converts a byte[] aray to a Byte[] array
   * @param b the byte[] array to convert
   * @return Byte[] array
   */
  public static Byte[] bToB(byte[] b) {
    Byte[] B = new Byte[b.length];
    for (int i = 0; i < b.length; i++) {
      B[i] = Byte.valueOf(b[i]);
    }
    return B;
  }

  /**
   * Converts a Byte[] array to a byte[] array
   * @param B The Byte[] array to convert
   * @return The converted byte[] array
   */
  public static byte[] bToB(Byte[] B) {
    byte[] b = new byte[B.length];
    System.arraycopy(B, 0, b, 0, B.length);
    return b;
  }

  /**
   * Converts a byte array into a string of hexadecimal bytes
   * @param bytes The array to convert
   * @return The string representation of the bytes in hexadecimal
   */
  public static String bytesToString(byte[] bytes) {
    String s = "";
    for (byte b : bytes) {
      s = String.format("%s %02x", s, b);
    }
    return s;
  }

  /**
   * Converts a specifid number of bytes to a string in hexadecimal 
   * representation
   * @param bytes The bytes to convert
   * @param n The number of bytes to convert
   * @return The string that represents the first n bytes
   */
  public static String bytesToString(byte[] bytes, int n) {
    String s = "";
    for (int i = 0; i < n && i < bytes.length; i++) {
      s = String.format("%s%02x ", s, bytes[i]);
    }
    return s;
  }

  /**
   * Convert an integer to a byte array
   * @param number The integer
   * @return A 4 byte long byte array
   */
  public static byte[] intToBytes(int number) {
    byte[] x = new byte[4];
    x[0] = (byte) ((number >> 24) & 0xff);
    x[1] = (byte) ((number >> 16) & 0xff);
    x[2] = (byte) ((number >> 8) & 0xff);
    x[3] = (byte) (number & 0xff);
    return x;
  }
}

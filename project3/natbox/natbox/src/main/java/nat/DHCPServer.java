package nat;

import java.util.ArrayList;

public class DHCPServer {
  ArrayList<Integer> allocatedIP;
  int addressIP = 0xC0A90001;
  long addressMAC;

  public DHCPServer() {
    this.addressMAC = generateRandomMAC();
  }

  private static long generateRandomMAC() {
    long mac = 0;
    for (int i = 0; i < 6; i++) {
      mac = mac<<8;
      mac = mac | (int) (Math.random()*0xFF);
    }
    return mac;
  }
}

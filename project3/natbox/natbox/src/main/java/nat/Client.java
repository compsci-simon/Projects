package nat;

import java.net.*;

public class Client {
  boolean internalClient = true;
  int addressIP = 0x7F000001;
  long addressMAC;

  public Client(boolean internalClient) {
    if (!internalClient) {
      this.addressIP = generateRandomIP();
    }
    this.internalClient = internalClient;
    this.addressMAC = generateRandomMAC();
  }

  public static void main(String[] args) {
    Client c = new Client(true);
  }

  private static long generateRandomMAC() {
    long mac = 0;
    for (int i = 0; i < 6; i++) {
      mac = mac<<8;
      mac = mac | (int) (Math.random()*0xFF);
    }
    return mac;
  }

  private static int generateRandomIP() {
    int addressIP = 0;
    for (int i = 0; i < 4; i++) {
      addressIP = addressIP<<8;
      addressIP = addressIP | (int) (Math.random()*0xFF);
    }
    return addressIP;
  }
  
}

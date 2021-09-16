package nat;

import java.net.*;

public class Client {
  private boolean internalClient = true;
  private int addressIP = 0x7F000001;
  private long addressMAC;
  private DatagramSocket socket;
  private DatagramPacket packet;

  public Client(boolean internalClient, String address, int port) {
    try {
      this.socket = new DatagramSocket();
      this.packet = new DatagramPacket(new byte[1500], 1500, InetAddress.getByName(address), port);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }
    if (!internalClient) {
      this.addressIP = generateRandomIP();
    }
    this.internalClient = internalClient;
    this.addressMAC = generateRandomMAC();
  }

  public static void main(String[] args) {
    Client c = new Client(true, "localhost", 5000);
    c.sendPacquet(args[0]);
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
  
  public void sendPacquet(String message) {
    packet.setData(message.getBytes(), 0, message.getBytes().length);
    try {
      socket.send(packet);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}

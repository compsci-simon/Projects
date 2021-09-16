package nat;

import java.net.*;
import java.net.DatagramPacket;
import java.util.ArrayList;

public class DHCPServer extends Thread {
  private DatagramSocket socket;
  private ArrayList<Integer> allocatedIP;
  private byte[] addressIP = {127, 0, 0, 1};
  private byte[] addressMAC;
  private DatagramPacket packet;

  public DHCPServer() throws Exception {
    this.addressMAC = generateRandomMAC();
    this.socket = new DatagramSocket(67);
    this.packet = new DatagramPacket(new byte[1500], 1500);
  }

  @Override
  public void run() {
    System.out.println("DHCP server running...");
    handleMessages();
  }

  public void handleMessages() {
    while (true) {
      try {
        socket.receive(packet);
      } catch (Exception e) {
        e.printStackTrace();
      }
      if (!handlePacket(packet.getData())) {
        break;
      }
    }
  }

  public boolean handlePacket(byte[] packet) {
    System.out.println("Packet received by DHCP server!");
    return false;
  }

  public void handleMessage(byte[] message) {
    switch (message[0]) {
      case 1:
        System.out.println();
        break;
      case 2:
        break;
      default:
        break;
    }
  }

  private static byte[] generateRandomMAC() {
    byte[] mac = new byte[6];
    for (int i = 0; i < 6; i++) {
      mac[i] = (byte) (Math.random()*0xff);
    }
    return mac;
  }
}

package nat;

import java.net.*;
import java.net.DatagramPacket;
import java.util.ArrayList;

public class DHCPServer extends Thread {
  private DatagramSocket socket;
  private ArrayList<Integer> allocatedIP;
  private int addressIP = 0xC0A90001;
  private long addressMAC;
  private DatagramPacket packet;

  public DHCPServer() throws Exception {
    this.addressMAC = generateRandomMAC();
    this.socket = new DatagramSocket(67);
    this.packet = new DatagramPacket(new byte[1500], 1500);
  }

  @Override
  public void run() {
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
    System.out.println("Packet received");
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

  private static long generateRandomMAC() {
    long mac = 0;
    for (int i = 0; i < 6; i++) {
      mac = mac<<8;
      mac = mac | (int) (Math.random()*0xFF);
    }
    return mac;
  }
}

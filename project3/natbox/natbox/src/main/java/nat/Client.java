package nat;

import java.math.BigInteger;
import java.net.*;
import java.nio.ByteBuffer;

public class Client {
  private boolean internalClient = true;
  private int addressIP = 0x7F000001;
  private long addressMAC;
  private DatagramSocket socket;
  private DatagramPacket packet;
  private int transactionIdentifier;
  private DHCPClient dhcpClient;

  public Client(boolean internalClient, String address) {
    try {
      this.socket = new DatagramSocket();
      this.packet = new DatagramPacket(new byte[1500], 1500, InetAddress.getByName(address), 10000);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }
    if (!internalClient) {
      this.addressIP = generateRandomIP();
    }
    this.internalClient = internalClient;
    this.addressMAC = generateRandomMAC();
    this.transactionIdentifier = 0;
    this.dhcpClient = new DHCPClient();
  }

  public static void main(String[] args) {
    Client c = new Client(true, "localhost");
    
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
  
  public void sendPacquet(String message, int address) {
    packet.setData(message.getBytes(), 0, message.getBytes().length);
    try {
      socket.send(packet);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public void sendDHCPDiscover() {
    byte[] packetBytes = dhcpClient.createDHCPPacket((byte)0x01, addressMAC);
    packet.setData(packetBytes);
    packet.setPort(67);
    try {
      socket.send(packet);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  

}

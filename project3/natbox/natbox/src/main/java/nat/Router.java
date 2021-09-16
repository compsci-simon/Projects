package nat;

import java.net.*;

public class Router {
  private DatagramSocket serverSock;
  private DatagramPacket packet;

  public Router (int portNum) {
    try {
      this.serverSock = new DatagramSocket(portNum);
      packet = new DatagramPacket(new byte[1500], 1500);
      handleConnections();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    Router router = new Router(5000);
  }

  private void handleConnections() {
    try {
      while (true) {
        serverSock.receive(packet);
        String message = new String(packet.getData());
        if (message.strip().equals("quit")) {
          break;
        } else {
          System.out.println(message + " from: "+packet.getAddress() + ", port: "+packet.getPort());
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

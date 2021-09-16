package nat;

import java.net.*;
import java.util.Arrays;

public class Router {
  private DatagramSocket serverSock;

  public Router (int portNum) {
    try {
      this.serverSock = new DatagramSocket(portNum);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    
  }
}

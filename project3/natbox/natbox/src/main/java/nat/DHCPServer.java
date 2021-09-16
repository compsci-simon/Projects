package nat;

import java.util.ArrayList;

public class DHCPServer {
  private ArrayList<Integer> allocatedIPs;

  public DHCPServer() {
    allocatedIPs = new ArrayList<Integer>();
    allocatedIPs.add(1);
    System.out.println("DHCP server started...");
  }

  public void processDHCPPacket(byte[] packet) {
    
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

}

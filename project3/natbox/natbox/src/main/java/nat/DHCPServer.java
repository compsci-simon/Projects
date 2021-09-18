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
    DHCP dhcpPacket = new DHCP(packet);
    if (dhcpPacket.getMessageType() == DHCP.bootRequest) {
      // dhcpPacket.setMessageType(DHCP.bootReply);
      System.out.println("here in dhcp");
    }
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

  public int lowestFreeIP() {
    for (int i = 2; i < 0xff; i++) {
      int j = 0;
      for (; j < allocatedIPs.size(); j++) {
        if (allocatedIPs.get(j) == i)
          break;
      }
      if (j == allocatedIPs.size())
        return j;
    }
    return -1;
  }

}

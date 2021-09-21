package nat;

import java.util.ArrayList;

public class DHCPServer {
  private ArrayList<Integer> allocatedIPs;
  private byte[] routerIP;

  public DHCPServer(byte[] routerIP) {
    if (routerIP == null || routerIP.length != 4) {
      System.err.println("Incorrect IP format");
      return;
    }
    this.routerIP = routerIP;
    allocatedIPs = new ArrayList<Integer>();
    allocatedIPs.add(1);
    System.out.println("DHCP server started...");
  }

  public DHCP generateBootResponse(DHCP dhcpPacket) {
    // dhcpPacket.setMessageType(DHCP.bootReply);
    byte[] newIP = new byte[4];
    System.arraycopy(routerIP, 0, newIP, 0, 3);
    int freeIP = lowestFreeIP();
    allocatedIPs.add(freeIP);
    newIP[3] = (byte) (freeIP&0xff);
    return DHCP.bootReply(dhcpPacket, newIP, routerIP);
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
        return i;
    }
    return -1;
  }

}

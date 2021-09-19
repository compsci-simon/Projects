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

  public byte[] generateBootResponse(DHCP dhcpPacket) {
    // dhcpPacket.setMessageType(DHCP.bootReply);
    System.out.println("here in dhcp");
    dhcpPacket.setMessageType(DHCP.bootReply);
    byte[] newIP = new byte[4];
    System.arraycopy(routerIP, 0, newIP, 0, 3);
    newIP[3] = (byte) (lowestFreeIP()&0xff);
    dhcpPacket.setciaddr(newIP);
    dhcpPacket.setOptions(DHCP.optionDHCPMessageType, (byte) DHCP.bootReply);
    dhcpPacket.setOptions(DHCP.optionDHCPServerIdentifier, Constants.bToB(routerIP));
    dhcpPacket.setOptions(DHCP.optionDHCPSubnetMask, Constants.bToB(Router.subnetMask));
    dhcpPacket.setOptions(DHCP.optionDHCPBroadcastIP, Constants.bToB(Router.broadcastIP));
    return dhcpPacket.getBytes();
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

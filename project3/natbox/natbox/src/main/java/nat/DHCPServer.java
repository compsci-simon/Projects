package nat;

import java.util.ArrayList;
import java.util.Arrays;

public class DHCPServer {
  private ArrayList<int[]> allocatedIPs;
  private byte[] routerIP;
  private int defaultTimeout = 120;

  public DHCPServer(byte[] routerIP, int timeout) {
    if (routerIP == null || routerIP.length != 4) {
      System.err.println("Incorrect IP format");
      return;
    }
    this.routerIP = routerIP;
    this.defaultTimeout = timeout;
    allocatedIPs = new ArrayList<int[]>();
    System.out.println("DHCP server started...");
  }

  public DHCP generateBootResponse(DHCP dhcpPacket) {
    byte[] newIP = new byte[4];
    System.arraycopy(routerIP, 0, newIP, 0, 3);
    int freeIP = lowestFreeIP();
    int[] temp = {freeIP, defaultTimeout};
    allocatedIPs.add(temp);
    newIP[3] = (byte) (freeIP&0xff);
    return DHCP.bootReply(dhcpPacket, newIP, routerIP);
  }

  public DHCP[] tick() {
    ArrayList<DHCP> messages = new ArrayList<DHCP>();
    ArrayList<Integer> toRemove = new ArrayList<Integer>();
    for (int i = 0; i < allocatedIPs.size(); i++) {
    	allocatedIPs.get(i)[1]--;
      if (allocatedIPs.get(i)[1] <= 0) {
        DHCP tmp = new DHCP(DHCP.ADDRESS_RELEASE, 1, new byte[6]);
        byte[] ciaddr = {(byte) 0xC0, (byte) 0xA8, (byte) 0, (byte) allocatedIPs.get(i)[0]};
        tmp.setciaddr(ciaddr);
        System.out.println("IP CI:" + IP.ipString(tmp.getCiaddr()));
        messages.add(tmp);
        toRemove.add(i);
      }
    }
    if (messages.size() > 0) {
      // Some entry has been removed and the table should be printed
      DHCP[] messagesDHCP = new DHCP[messages.size()];
      for (int i = 0; i < messages.size(); i++) {
        messagesDHCP[i] = messages.get(i);
        allocatedIPs.remove(toRemove.get(i));
      }
      System.out.println(toString());
      return messagesDHCP;
    } else {
      return null;
    }
  }

  public void resetIPCountDown(byte[] ip) {
    for (int i = 0; i < allocatedIPs.size(); i++) {
      if (allocatedIPs.get(i)[0] == (ip[3]&0xff)) {
        allocatedIPs.get(i)[1] = defaultTimeout;
        break;
      }
    }
  }

  public int lowestFreeIP() {
    for (int i = 2; i < 0xff; i++) {
      int j = 0;
      for (; j < allocatedIPs.size(); j++) {
        if (allocatedIPs.get(j)[0] == i)
          break;
      }
      if (j == allocatedIPs.size())
        return i;
    }
    return -1;
  }

  public String toString() {
    String s = String.format("----------------------\nDHCP Table");
    if (allocatedIPs.size() == 0) {
      s += "Empty";
    } else {
      for (int i = 0; i < allocatedIPs.size(); i++) {
        s = String.format("%s\n(%d) 192.168.0.%d", s, i + 1, allocatedIPs.get(i)[0]);
      }
    }
    s += "\n";
    return s;
  }

}

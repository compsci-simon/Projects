package nat;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Used in the router to maintain a list of available IP addresses
 */
public class DHCPServer {
  private ArrayList<Integer> allocatedIPs;
  private byte[] routerIP;

  /**
   * Creates a new DHCP server
   * @param routerIP The IP address of the router on which the DHCP server runs
   */
  public DHCPServer(byte[] routerIP) {
    if (routerIP == null || routerIP.length != 4) {
      System.err.println("Incorrect IP format");
      return;
    }
    this.routerIP = routerIP;
    allocatedIPs = new ArrayList<Integer>();
    System.out.println("DHCP server started...");
  }

  /**
   * Generates a bootresponse to a bootrequest
   * @param dhcpPacket The bootrequest
   * @return The bootresponse
   */
  public DHCP generateBootResponse(DHCP dhcpPacket) {
    byte[] newIP = new byte[4];
    System.arraycopy(routerIP, 0, newIP, 0, 3);
    int freeIP = lowestFreeIP();
    allocatedIPs.add(freeIP);
    newIP[3] = (byte) (freeIP&0xff);
    return DHCP.bootReply(dhcpPacket, newIP, routerIP);
  }

  /**
   * Remove an IP address from the pool of taken IP's
   * @param ipAddress The IP address to unallocate
   */
  public void removeIP(byte[] ipAddress) {
	  int ip = IP.toInt(ipAddress);
	  for (int i = 0; i < allocatedIPs.size(); i++) {
		  if (allocatedIPs.get(i) == ip) {
			  allocatedIPs.remove(i);
			  break;
		  }
	  }
  }

  /**
   * Gets the first IP address that can be allocated
   * @return The value of the last octet
   */
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

  /**
   * @return A string representation of the DHCP server
   */
  public String toString() {
    String s = String.format("----------------------\nDHCP Table");
    if (allocatedIPs.size() == 0) {
      s += "Empty";
    } else {
      for (int i = 0; i < allocatedIPs.size(); i++) {
        s = String.format("%s\n(%d) 192.168.0.%d", s, i + 1, allocatedIPs.get(i));
      }
    }
    s += "\n";
    return s;
  }

}

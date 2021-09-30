package nat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This table object is used to map all IP addresses to physical hardware.
 */
public class ARPTable {
  private HashMap<Integer, byte[]> arpTable;

  /**
   * Constructs an empty arp table
   */
  public ARPTable() {
    arpTable = new HashMap<Integer, byte[]>();
  }

  /**
   * Adds an IP address MAC address pair to the table
   */
  public void addPair(byte[] addressIP, byte[] addressMAC) {
    arpTable.put(toInt(addressIP), addressMAC);
  }

  /**
   * Verifies whether an IP / MAC pair is contained within the table. Needed 
   * for packets incoming from the external interface on a router.
   * @param addressIP The IP address of the source of the IP packet
   * @return True if the IP address and source can be mapped
   */
  public boolean containsMAC(byte[] addressIP) {
    return arpTable.containsKey(toInt(addressIP));
  }
  
  /**
   * Gets the physical address for a given IP address.
   * @param addressIP The IP address to be resolved to a MAC address
   * @return The byte array that represents the MAC address
   */
  public byte[] getMAC(byte[] addressIP) {
	  return arpTable.get(toInt(addressIP));
  }
  
  /**
   * Converts a 4 byte array IP address into an integer to be used
   * when hashing for the key in the arptable to see if the entry 
   * exists.
   * @param bytes The IP address array
   * @return the IP address in an integer form
   */
  public int toInt(byte[] bytes) {
	  return ((bytes[0] & 0xff) << 24) | ((bytes[1] & 0xff) << 16) | 
    ((bytes[2] & 0xff) << 8) | (bytes[3] & 0xff);
  }

  /**
   * Returns a string representation of the table which is used to track
   * the status of the arp table
   */
  public String toString() {
    Iterator<Map.Entry<Integer, byte[]>> hmIterator = arpTable.entrySet().iterator();
    String s = "\nARP TABLE toString\n----------------------";
    if (arpTable.size() == 0) {
      s += "\nARP Table is empty";
    } else {
      while (hmIterator.hasNext()) {
        Map.Entry<Integer, byte[]> element = hmIterator.next();
        s = String.format("%s\n%s -> %s", s, IP.ipString(element.getKey()), Ethernet.macString(element.getValue()));
      }
    }
    s += "\n";
    return s;
  }
  
}
package nat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ARPTable {
  private HashMap<Integer, byte[]> arpTable;

  public ARPTable() {
    arpTable = new HashMap<Integer, byte[]>();
  }

  public void addPair(byte[] addressIP, byte[] addressMAC) {
    arpTable.put(toInt(addressIP), addressMAC);
  }

  public boolean containsMAC(byte[] addressIP) {
    return arpTable.containsKey(toInt(addressIP));
  }
  
  public byte[] getMAC(byte[] addressIP) {
	  return arpTable.get(toInt(addressIP));
  }
  
  public int toInt(byte[] bytes) {
	  return ((bytes[0] & 0xff) << 24) | ((bytes[1] & 0xff) << 16) | 
    ((bytes[2] & 0xff) << 8) | (bytes[3] & 0xff);
  }

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
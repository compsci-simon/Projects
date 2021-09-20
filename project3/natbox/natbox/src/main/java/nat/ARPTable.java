package nat;

import java.util.HashMap;

public class ARPTable {
  HashMap<Integer, byte[]> arpTable;

  public ARPTable() {
    arpTable = new HashMap<Integer, byte[]>();
  }

  public void addPair(byte[] addressIP, byte[] addressMAC) {
    arpTable.put(toInt(addressIP), addressMAC);
  }

  public boolean containsMAC(byte[] addressIP) {
    if (arpTable.containsKey(toInt(addressIP))) {
      /* pair not in table */
      return true;
    } else {
      /* pair in table */
      return false;
    }
  }
  
  public byte[] getMAC(byte[] addressIP) {
	  return arpTable.get(toInt(addressIP));
  }
  
  public int toInt(byte[] bytes) {
	  return ((bytes[0] & 0xff << 24) | (bytes[1] & 0xff << 16) | 
    (bytes[2] & 0xff << 8) | (bytes[3] & 0xff));
  }
  
}
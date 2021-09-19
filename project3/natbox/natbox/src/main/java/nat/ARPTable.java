package nat;

import java.util.ArrayList;
import java.util.HashMap;

public class ARPTable {
  HashMap<Integer, Integer> arpTable;

  public ARPTable() {
    arpTable = new HashMap<Integer, Integer>();
  }

  private void addPair(Integer addressMAC, Integer addressIP) {
    arpTable.put(addressIP, addressMAC);
  }

  private boolean containsMAC(Integer addressIP) {
    if (arpTable.containsKey(addressIP)) {
      /* pair not in table */
      return true;
    } else {
      /* pair in table */
      return false;
    }
  }

  
}
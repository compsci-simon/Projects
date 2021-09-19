package nat;

import java.util.ArrayList;

public class ARPTable {
  HashMap<Integer, Integer> arpTable;

  public ARPTable() {
    arpTable = new HashMap<Integer, Integer>();
  }

  private void addPair(Integer addressMAC, Integer addressIP) {
    arpTable.put(addressIP, addressMAC);
  }

  private Integer getMAC(Integer addressIP) {
    int result;
    if (!(result = arpTable.get(addressIP)) {
      /* pair not in table, send request */
    } else {
      /* pair in table */
      return result;
    }
  }

  
}
package nat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class NAPT {
	byte[] externalIP;
	private ArrayList<Integer> allocatedPorts;
	private HashMap<Long, byte[]> naptTable;

	public NAPT(byte[] externalIP) {
		this.externalIP = externalIP;
		allocatedPorts = new ArrayList<Integer>();
		naptTable = new HashMap<Long, byte[]>();
	}

	public void addPair(byte[] addressIP, int port) {
		int externalPort = lowestFreePort();
	    allocatedPorts.add(externalPort);
		naptTable.put(toLong(addressIP, port), toBytes(externalIP, externalPort));
	}
	
	public boolean containsSession(byte[] addressIP, int port) {
		return naptTable.containsKey(toLong(addressIP, port));
	}
	  
	public byte[] getSession(byte[] addressIP, int port) {
		return naptTable.get(toLong(addressIP, port));
	}
	
	public long toLong(byte[] ipAddress, int port) {
		byte[] bytes = new byte[8];
		bytes[0] = (byte) ((port >> 24) & 0xff);
		bytes[1] = (byte) ((port >> 16) & 0xff);
		bytes[2] = (byte) ((port >> 8) & 0xff);
		bytes[3] = (byte) (port & 0xff);
		bytes[4] = ipAddress[0];
		bytes[5] = ipAddress[1];
		bytes[6] = ipAddress[2];
		bytes[7] = ipAddress[3];
		
		long key = 0;
		for (int i = 0; i < bytes.length; i++)
		{
		   key = (key << 8) + (bytes[i] & 0xff);
		}
		return key;
	}
	
	public byte[] toBytes(byte[] ipAddress, int port) {
		byte[] bytes = new byte[8];
		bytes[0] = (byte) ((port >> 24) & 0xff);
		bytes[1] = (byte) ((port >> 16) & 0xff);
		bytes[2] = (byte) ((port >> 8) & 0xff);
		bytes[3] = (byte) (port & 0xff);
		bytes[4] = ipAddress[0];
		bytes[5] = ipAddress[1];
		bytes[6] = ipAddress[2];
		bytes[7] = ipAddress[3];
		
		return bytes;
	}
	
	public byte[] toBytes(long Long) {
		byte[] bytes = new byte[8];
		bytes[0] = (byte) ((Long >> 56) & 0xff);
		bytes[1] = (byte) ((Long >> 48) & 0xff);
		bytes[2] = (byte) ((Long >> 40) & 0xff);
		bytes[3] = (byte) ((Long >> 32) & 0xff);
		bytes[4] = (byte) ((Long >> 24) & 0xff);
		bytes[5] = (byte) ((Long >> 16) & 0xff);
		bytes[6] = (byte) ((Long >> 8) & 0xff);
		bytes[7] = (byte) (Long & 0xff);
		
		return bytes;
	}
	
	public byte[] getAddress(byte[] bytes) {
		byte[] ipAddress = new byte[4];
		ipAddress[0] = bytes[4];
		ipAddress[1] = bytes[5];
		ipAddress[2] = bytes[6];
		ipAddress[3] = bytes[7];
		return ipAddress;
	}
	
	public int getPort(byte[] bytes) {
		return ((bytes[0] & 0xff) << 24) | ((bytes[1] & 0xff) << 16) | 
		    ((bytes[2] & 0xff) << 8) | (bytes[3] & 0xff);
	}
	
	public String combinationToString(byte[] bytes) {
		String ipAddress = IP.ipString(getAddress(bytes));
		int port = getPort(bytes);
		return ipAddress + ":" + port;
	}
	
	public int lowestFreePort() {
		return allocatedPorts.size();
	}
	
	public String toString() {
	    Iterator<Map.Entry<Long, byte[]>> hmIterator = naptTable.entrySet().iterator();
	    String s = "\nNAPT TABLE toString\n----------------------";
	    if (naptTable.size() == 0) {
	      s += "\nNAPT Table is empty";
	    } else {
	      while (hmIterator.hasNext()) {
	        Map.Entry<Long, byte[]> element = hmIterator.next();
	        s = String.format("%s\n%s -> %s", s, combinationToString(toBytes(element.getKey())), combinationToString(element.getValue()));
	      }
	      s += "\n";
	    }
	    return s;
	  }
	
}

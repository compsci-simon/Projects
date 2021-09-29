package nat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class NAPT {
	private byte[] externalIP;
  private byte[] internalIP = {(byte) 0xC0, (byte) 0xA8, 0, 1};
	private ArrayList<Integer> allocatedPorts;
	private HashMap<Long, Long> timeStampTable;
	private HashMap<Long, byte[]> naptTable;
	/**
	 * key = 
	 * 
	 * value[] Structure = {
	 * 			port, port, port, port, 
	 * 			destIP, destIP, destIP, destIP, 
	 * 			sourceIP, sourceIP, sourceIP, sourceIP
	 * }
	 * 
	 */

	public NAPT(byte[] externalIP) {
		this.externalIP = externalIP;
		allocatedPorts = new ArrayList<Integer>();
		naptTable = new HashMap<Long, byte[]>();
		timeStampTable = new HashMap<Long, Long>();
	}

	public IP translate(IP packet) {
		System.out.println(toString());
		if (packet.getDemuxPort() != IP.UDP_PORT && packet.getDemuxPort() != IP.TCP_PORT) {
			System.err.println("IP Packet must be UDP or TCP to pass natbox");
			return null;
		}
		if (IP.sameNetwork(internalIP, packet.source())) {
			// Translating outgoing packets
			if (!containsSession(packet, transportLayerPortSource(packet))) {
				addSession(packet.source(), packet.destination(), transportLayerPortSource(packet));
			}
			packet.setSource(externalIP);
			return packet;
		} else {
			// Translating incoming packets
			if (!containsSession(packet, transportLayerPortDest(packet))) {
				System.out.println("Session not contained");
				return packet;
			} else {
				byte[] value = getExternalSession(packet, transportLayerPortDest(packet));
				byte[] newIP = new byte[4];
				System.arraycopy(value, 4, newIP, 0, 4);
				packet.setDest(newIP);
				return packet;
			}
		}
	}

	public void addPair(byte[] addressIP, int port) {
		int externalPort = lowestFreePort();
	  allocatedPorts.add(externalPort);
		naptTable.put(toLong(addressIP, port), toBytes(externalIP, externalPort));
	}

	public void addSession(byte[] sourceIP, byte[] destIP, int port) {
		if (port == -1)
			return;
		byte[] portBytes = Constants.intToBytes(port);

		Long key = toLong(sourceIP, port);
		byte[] value1 = new byte[12];
		byte[] value2 = new byte[12];

		System.arraycopy(portBytes, 0, value1, 0, 4);
		System.arraycopy(destIP, 0, value1, 4, 4);
		System.arraycopy(sourceIP, 0, value1, 8, 4);
		naptTable.put(key, value1);
		timeStampTable.put(key, System.currentTimeMillis());

		if (!IP.isNilIP(sourceIP)) {
			// This conditional checks that we dont put the nil IP in the destination
			// when port forwarding
			key = toLong(destIP, port);
			System.arraycopy(portBytes, 0, value2, 0, 4);
			System.arraycopy(sourceIP, 0, value2, 4, 4);
			System.arraycopy(destIP, 0, value2, 8, 4);
			naptTable.put(key, value2);
			timeStampTable.put(key, System.currentTimeMillis());
		}

		naptTable.toString();
	}
	
	public boolean containsSession(IP packet, int port) {
		if (port == -1) {
			System.out.println("Invalid port");
			return false;
		}
		byte[] sourceIP = packet.source();
		if (!IP.sameNetwork(packet.source(), internalIP)) {
			// Sessions bound from WAN to LAN
			if (naptTable.containsKey(toLong(IP.nilIP, port))) {
				// reset timer because entry was used
				timeStampTable.put(toLong(IP.nilIP, port), System.currentTimeMillis());
				return true;
			}
			if (!naptTable.containsKey(toLong(sourceIP, port))) {
				System.out.println("Source : Port combo not contained in table");
				return false;
			}
		} else {
			// Sessions bound from LAN to WAN
			if (!naptTable.containsKey(toLong(sourceIP, port)))
				return false;
		}
		// reset timer because entry was used
		timeStampTable.put(toLong(sourceIP, port), System.currentTimeMillis());
		return true;
	}

	public void portForward(int port, byte[] destLANIP) {
		if (!IP.sameNetwork(destLANIP, internalIP)) {
			System.err.println("You cannot forward to external IP " + IP.ipString(destLANIP));
			return;
		}
		addSession(IP.nilIP, destLANIP, port);
	}
	
	
	/**
	 * Removes entries that have been idle for a certain amount of time
	 * 
	 * @param minutesToRefresh how often the NAPT table should be refreshed
	 * @param SecsToRemove how long a napt entry should be unused before removing
	 */
	public void refreshNAPTTable(int minutesToRefresh, int SecsToRemove) {
    	while (true) {
    		try {
    			ArrayList<Long> toRemoveList = new ArrayList<Long>(); 
    			Iterator<Map.Entry<Long, Long>> hmIterator = timeStampTable.entrySet().iterator();
    		    
    		    if (timeStampTable.size() == 0) {
    		    	// NAPT table is empty
    		    } else {
    		    	// iterate through napt table and check for old entries 
    		      while (hmIterator.hasNext()) {
    		        Map.Entry<Long, Long> element = hmIterator.next();
    		        if (System.currentTimeMillis() - (long) 1000*SecsToRemove >= element.getValue()) {
    		        	naptTable.remove(element.getKey());
    		        	System.out.println(toString());
    		        	toRemoveList.add(element.getKey());
    		        }
    		      }
    		    }
    		    for (int i = 0; i < toRemoveList.size(); i++) {
    		    	timeStampTable.remove(toRemoveList.get(i));
    		    }
    	        Thread.sleep(1000*60*minutesToRefresh);
    		    } catch (Exception e) {
    	        e.printStackTrace();
    	      }
    		}
	}

	/**
	 * We allow traffic from any source through if the dest IP is the nil IP
	 * @param value the value of the key - value pair form the table
	 * @return whether or not the destination address is the nilIP
	 */
	public boolean nilSource(byte[] value) {
		for (int i = 4; i < 8; i++) {
			if (value[i] != 0)
				return false;
		}
		return true;
	}
	 
	public int transportLayerPortSource(IP packet) {
		int port;
		if (packet.getDemuxPort() == IP.UDP_PORT) {
			UDP udpPack = new UDP(packet.payload());
			port = udpPack.sourcePort();
		} else if (packet.getDemuxPort() == IP.TCP_PORT) {
			TCP tcpPack = new TCP(packet.payload());
			port = tcpPack.sourcePort();
		} else {
			System.err.println("Unknown demux port " + packet.getDemuxPort());
			return -1;
		}
		return port;
	}	 

	public int transportLayerPortDest(IP packet) {
		int port;
		if (packet.getDemuxPort() == IP.UDP_PORT) {
			UDP udpPack = new UDP(packet.payload());
			port = udpPack.destinationPort();
		} else if (packet.getDemuxPort() == IP.TCP_PORT) {
			TCP tcpPack = new TCP(packet.payload());
			port = tcpPack.destinationPort();
		} else {
			System.err.println("Unknown demux port " + packet.getDemuxPort());
			return -1;
		}
		return port;
	}

	public byte[] getExternalSession(IP externalPacket, int port) {
		if (naptTable.get(toLong(externalPacket.source(), port)) == null) {
			return naptTable.get(toLong(IP.nilIP, port));
		} else {
			return naptTable.get(toLong(externalPacket.source(), port));
		}
	}

	public byte[] getInternalSession(IP internalPacket, int port) {
		return naptTable.get(toLong(internalPacket.source(), port));
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
    for (int i = 2; i < 0xff; i++) {
      int j = 0;
      for (; j < allocatedPorts.size(); j++) {
        if (allocatedPorts.get(j) == i)
          break;
      }
      if (j == allocatedPorts.size())
        return i;
    }
    return -1;
	}

	public String valueToString(byte[] value, Long key) {
		String s = String.format("Destination IP = %d.%d.%d.%d, " 
				+ "Source IP = %d.%d.%d.%d, port number = %d, key = %016x", 
				value[4]&0xff, value[5]&0xff, value[6]&0xff, value[7]&0xff, 
				value[8]&0xff, value[9]&0xff, value[10]&0xff, value[11]&0xff, 
				getPort(value), key);
		return s;
	}
	
	public String toString() {
	    Iterator<Map.Entry<Long, byte[]>> hmIterator = naptTable.entrySet().iterator();
	    String s = "\nNAPT TABLE toString\n----------------------";
	    if (naptTable.size() == 0) {
	      s += "\nNAPT Table is empty";
	    } else {
	      while (hmIterator.hasNext()) {
	        Map.Entry<Long, byte[]> element = hmIterator.next();
	        s = String.format("%s\n%s", s, valueToString(element.getValue(), element.getKey()));
	      }
	    }
	    s += "\n";
	    return s;
	  }
	
}

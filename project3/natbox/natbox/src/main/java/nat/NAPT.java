package nat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class NAPT {
	private byte[] externalIP;
  private byte[] internalIP = {(byte) 0xC0, (byte) 0xA8, 0, 1};
	private ArrayList<Integer> allocatedPorts;
	private HashMap<Long, byte[]> naptTable;

	public NAPT(byte[] externalIP) {
		this.externalIP = externalIP;
		allocatedPorts = new ArrayList<Integer>();
		naptTable = new HashMap<Long, byte[]>();
	}

	public IP translate(IP packet) {
		if (packet.getDemuxPort() != IP.UDP_PORT && packet.getDemuxPort() != IP.TCP_PORT) {
			System.err.println("IP Packet must be UDP or TCP to pass natbox");
			return null;
		}
		if (IP.sameNetwork(internalIP, packet.source())) {
			if (!containsSession(packet)) {
				addSession(packet);
			}
			packet.setSource(externalIP);
			return packet;
		} else {
			if (!containsSession(packet)) {
				System.err.println("Cannot forward this packet");
				return null;
			} else {

			}
		}
		return null;
	}

	public void addPair(byte[] addressIP, int port) {
		int externalPort = lowestFreePort();
	  allocatedPorts.add(externalPort);
		naptTable.put(toLong(addressIP, port), toBytes(externalIP, externalPort));
	}

	public void addSession(IP packet) {
		if (containsSession(packet))
			return;
		int port = transportLayerPort(packet);
		if (port == -1)
			return;
		byte[] portBytes = Constants.intToBytes(port);
		byte[] sourceIP = packet.source();
		byte[] destIP = packet.destination();

		Long key = toLong(packet.source(), packet.getDemuxPort());
		byte[] value = new byte[12];
		System.arraycopy(portBytes, 0, value, 0, 4);
		System.arraycopy(sourceIP, 4, value, 8, 4);
		System.arraycopy(destIP, 8, value, 12, 4);
		naptTable.put(key, value);
	}
	
	public boolean containsSession(IP packet) {
		int port = transportLayerPort(packet);
		if (port == -1)
			return false;
		byte[] sourceIP = packet.source();
		byte[] destIP = packet.destination();
		if (!naptTable.containsKey(toLong(sourceIP, port)))
			return false;
		byte[] value = naptTable.get(toLong(sourceIP, port));
		byte[] portBytes = Constants.intToBytes(port);
		for (int i = 0; i < 4; i++) {
			if (portBytes[i] != value[i]) 
				return false;
		}
		for (int i = 4; i < 8; i++) {
			if (sourceIP[i] != value[i]) 
				return false;
		}
		for (int i = 8; i < 12; i++) {
			if (destIP[i] != value[i]) 
				return false;
		}
		return true;
	}
	 
	public int transportLayerPort(IP packet) {
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

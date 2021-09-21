package nat;

public class ARP {
    public static final int DEMUX_PORT = 2054;
    public static final int ARP_REQUEST = 1;
    public static final int ARP_REPLY = 2;
    private int hardwareType;
    private int protocolType;
    private int hardwareSize;
    private int protocolSize;
    private int opCode;
    private byte[] srcMAC;
    private byte[] destMAC;
    private byte[] srcIP;
    private byte[] destIP;
    public static byte[] broadcastMAC = {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
    public static byte[] zeroMAC = {(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00};

    public ARP(byte[] packet) {
        this.hardwareType = (packet[0]&0xff)<<8 | (packet[1]&0xff);
        this.protocolType = (packet[2]&0xff)<<8 | (packet[3]&0xff);
        this.hardwareSize = packet[4]&0xff;
        this.protocolSize = packet[5]&0xff;
        this.opCode = (packet[6]&0xff)<<8 | (packet[7]&0xff);
        this.srcMAC = new byte[6];
        System.arraycopy(packet, 8, srcMAC, 0, 6);
        this.srcIP = new byte[4];
        System.arraycopy(packet, 14, srcIP, 0, 4);
        this.destMAC = new byte[6];
        System.arraycopy(packet, 18, destMAC, 0, 6);
        this.destIP = new byte[4];
        System.arraycopy(packet, 24, destIP, 0, 4);
    }

    public ARP(int opCode, byte[] srcMAC, byte[] srcIP, byte[] destMAC, byte[] destIP) {
    	this.opCode = opCode;
    	this.srcMAC = new byte[6];
        System.arraycopy(srcMAC, 0, this.srcMAC, 0, 6);
        this.srcIP = new byte[4];
        System.arraycopy(srcIP, 0, this.srcIP, 0, 4);
        this.destMAC = new byte[6];
        System.arraycopy(destMAC, 0, this.destMAC, 0, 6);
        this.destIP = new byte[4];
        System.arraycopy(destIP, 0, this.destIP, 0, 4);
    }
    
    public byte[] getBytes() {
    	byte[] message = new byte[28];
        /* hardware type (ethernet) */
        message[0] = 0x00;
        message[1] = 0x01;
        
        /* protocol type */
        message[2] = 0x08;
        message[3] = 0x00;
        
        /* hardware size */
        message[4] = 0x06;

        /* protocol size */
        message[5] = 0x04;

        /* opcode */
        message[6] = (byte) 0x00;
        if (opCode == 1) {
        	message[7] = (byte) 0x01;
        } else if (opCode == 2) {
        	message[7] = (byte) 0x02;
        }
        
        System.arraycopy(srcMAC, 0, message, 8, 6);
        System.arraycopy(srcIP, 0, message, 14, 4);
        System.arraycopy(destMAC, 0, message, 18, 6);
        System.arraycopy(destIP, 0, message, 24, 4);

        return message;
    }

    public int opCode() {
        return opCode;
    }

    public byte[] srcMAC() {
        return srcMAC;
    }

    public byte[] srcIP() {
        return srcIP;
    }

    public byte[] destMAC() {
        return destMAC;
    }

    public byte[] destIP() {
        return destIP;
    }

    public String toString() {
        String s = String.format("\n\nARP toString\n----------------------" + 
            "\nDestination MAC = %s\nSource MAC = %s\nDestination IP = %s\n" + 
            "Source IP = %s\nopCode = %d", Ethernet.macString(destMAC), 
            Ethernet.macString(srcMAC), IP.ipString(destIP), IP.ipString(srcIP), 
            opCode);
        return s;
      }
}
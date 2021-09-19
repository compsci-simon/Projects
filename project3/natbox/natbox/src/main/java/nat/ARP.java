package nat;

public class ARP {
    public static int demuxARP = 2048;
    private int opCode;
    private byte[] srcMAC;
    private byte[] destMAC;
    private byte[] srcIP;
    private byte[] destIP;
    private static byte[] broadcastMAC = {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
    private static byte[] zeroMAC = {(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00};

    public byte[] createPacketARP(int opCode, byte[] srcMAC, byte[] destMAC, byte[] srcIP, byte[] destIP) {
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
        message[7] = 0x00;
        message[8] = (byte) opCode;
    
        System.arraycopy(srcMAC, 0, message, 9, 6);
        System.arraycopy(srcIP, 0, message, 15, 4);
        System.arraycopy(destMAC, 0, message, 19, 6);
        System.arraycopy(destIP, 0, message, 25, 4);

        return message;
    }

    public void sendRequestARP(byte[] srcMAC, byte[] srcIP, byte[] destIP) {
        byte[] packetARP = createPacketARP(1, srcMAC, zeroMAC, srcIP, destIP);
        //byte[] frame = encapsulateEthernet(broadcastMAC, srcMAC, packetARP);
        //sendFrame(frame);
    }

    public void sendResponseARP(byte[] srcMAC, byte[] destMAC, byte[] srcIP, byte[] destIP) {
        byte[] packetARP = createPacketARP(2, srcMAC, zeroMAC, srcIP, destIP);
        //byte[] frame = encapsulateEthernet(destMAC, srcMAC, packetARP);
       // sendFrame(frame);
    }
}
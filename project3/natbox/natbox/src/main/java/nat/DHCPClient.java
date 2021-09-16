package nat;

import java.nio.ByteBuffer;

public class DHCPClient {
  int transactionIdentifier;
  long addressMAC;

  public DHCPClient(long addressMAC) {
    this.transactionIdentifier = 0;
    this.addressMAC = addressMAC;
  }

  public byte[] encapDHCPRequest() {
    byte[] message = new byte[236];
    // Setting operation code
    message[0] = 0x01;
    // Setting hardware type
    message[1] = 0x01;
    // Setting hardware address length
    message[2] = 0x06; // 48 bits
    // Setting number of hops that have occured to 0
    message[3] = 0x00;
    // Setting the transaction identifier for the packet
    ByteBuffer bb = ByteBuffer.allocate(4);
    bb.putInt(transactionIdentifier);
    byte[] transactionIDByteArray = bb.array();
    System.arraycopy(transactionIDByteArray, 0, message, 4, 4);
    transactionIdentifier++;
    // Setting the seconds that have elapsed
    message[8] = 0x00;
    message[9] = 0x00;
    // Setting the Flags bytes to 0 for unicast
    message[10] = 0x00;
    message[11] = 0x00;
    // ciAddress is 0 in the beginning
    message[12] = 0x00;
    message[13] = 0x00;
    message[14] = 0x00;
    message[15] = 0x00;
    // yiAddress is 0 because it needs to be set by the server
    message[16] = 0x00;
    message[17] = 0x00;
    message[18] = 0x00;
    message[19] = 0x00;
    // siAddress is 0 because there is no secondary server
    message[21] = 0x00;
    message[22] = 0x00;
    message[23] = 0x00;
    message[24] = 0x00;
    // giAddress is 0 because there is no secondary server
    message[25] = 0x00;
    message[26] = 0x00;
    message[27] = 0x00;
    message[28] = 0x00;
    // set chaddr to my MAC address
    // 6 byte MAC address
    bb = ByteBuffer.allocate(8);
    bb.putLong(addressMAC);
    byte[] addressMACByteArray = bb.array();
    System.arraycopy(addressMACByteArray, 2, message, 29, 6);

    // Client hardware padding
    for (int i = 0; i < 10; i++) {
      message[35+i] = 0x00;
    }

    // 64 0x00 bytes for server address
    // 128 0x00 bytes for boot file name
    return message;
  }
}

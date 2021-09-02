package cust;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class NIOServer {
  public static void main(String[] args) throws IOException {
	  
    DatagramChannel channel = DatagramChannel.open();
    channel.socket().bind(new InetSocketAddress(9999));
    
    ByteBuffer buf = ByteBuffer.allocate(128);
    channel.receive(buf);
    
    for (int i = 0; i < 30; i++) {
		  channel.receive(buf);
	}
  }
}

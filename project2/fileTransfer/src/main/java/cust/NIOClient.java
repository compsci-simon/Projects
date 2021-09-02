package cust;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.DatagramChannel;

public class NIOClient {

  public static void main(String[] args) throws IOException {
	  DatagramChannel channel = DatagramChannel.open();
	  channel.socket().bind(new InetSocketAddress(10000));
	  
	  channel.connect(new InetSocketAddress("localhost", 9999));
	  
	  ByteBuffer buf = ByteBuffer.allocate(64000);
	  for (int i = 0; i < 30; i++) {
		  channel.write(buf);
	  }
  }
}

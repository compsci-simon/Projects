package cust;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * This class contains some static utility methods that are useful
 */
public class Utils {

  public static void main(String[] args) {
	  try {
		  DatagramSocket s = new DatagramSocket();
		  
		  InetAddress ia = InetAddress.getByName("localhost");
		  DatagramPacket p = new DatagramPacket(new byte[64000], 64000, ia, 30000);
		  for (int i = 0; i < 10000; i++) {
			  s.send(p);
		  }
	  } catch (Exception e) {
		  e.printStackTrace();
	  }
	  System.out.println("Done");
  }
  
  /*
   * Logger to print select statements which can easily be turned on and off.
   */
  public static void logger(String s) {
    System.out.println(s);
  }

  public static void logger(int s) {
    System.out.println(s);
  }

  public static void logger(boolean s) {
    System.out.println(s);
  }

  public static void logger(char s) {
    System.out.println(s);
  }

  public static void logger(byte s) {
    System.out.println(s);
  }

  /*
   * Used to log the progress of the file transfer
   */
  public static void logProgress(int bytesReceived, int fileSize) {
    System.out.printf("%f/100\n", (100.0 * bytesReceived)/fileSize);
  }

  public static void logToFile(String message, String filename) throws Exception {
    FileWriter writer = new FileWriter(filename);
    writer.write(message);
    writer.close();
  }
  
  public static int highest_common_denom(int size) {
	  int denom = 1;
	  for (int i = 1; i < 100; i++) {
		  if (size%i == 0) {
			  denom = i;
		  }
	  }
	  return denom;
  }

}

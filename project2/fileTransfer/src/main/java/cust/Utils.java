package cust;

import java.io.*;

/**
 * This class contains some static utility methods that are useful
 */
public class Utils {

  public static void main(String[] args) {
    
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

}

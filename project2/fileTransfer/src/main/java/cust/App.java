package cust;

import java.net.InetAddress;
import java.nio.file.*;
import java.util.Arrays;
import java.io.*;

/**
 * Hello world!
 *
 */
public class App 
{
    public static final String filePath = "/Users/simon/Developer/git_repos/Projects/project2/fileTransfer/assets/file.dmg";
    public static final String newFilePath = "/Users/simon/Developer/git_repos/Projects/project2/fileTransfer/assets/file2.dmg";
    public static void main( String[] args ) throws Exception
    {
        byte[] bytes = Files.readAllBytes(Paths.get(filePath));
        

        Thread t1 = new Thread() {
            @Override
            public void run() {
                try {
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        Thread t2 = new Thread() {
            @Override
            public void run() {
                try {
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        t1.start();
        t2.start();

    }
}


package test;

import javax.swing.JFrame;

import java.awt.Color;
import java.awt.color.*;

public class Jframes {
	
	public static void main(String[] args) {
		
		JFrame frame = new JFrame("Try this out");
		frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		frame.setSize(350, 350);
		frame.setResizable(false);
		frame.getContentPane().setBackground(Color.lightGray);
	}
	
}

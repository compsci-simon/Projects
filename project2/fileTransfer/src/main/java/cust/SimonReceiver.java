package cust;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

public class SimonReceiver extends JFrame {
	private Server server;
	private JPanel p1, p2;
	private Container c;
	private CardLayout card;
	private JLabel l11, l12, l13, l21, l22, l23;
	private JTextField tf11, tf12;
	private JButton b11, b21;
	JProgressBar progressBar;
	
	public SimonReceiver() {
		
		c = getContentPane();
		card = new CardLayout(40, 40);
		c.setLayout(card);
		
		p1 = new JPanel(new GridLayout(10, 1));
		l11 = new JLabel("UDP Port");
		l12 = new JLabel("TCP Port");
		tf11 = new JTextField();
		tf12 = new JTextField();
		b11 = new JButton("Start server");
		l13 = new JLabel("");
		b11.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				try {
					l13.setText("");
					int udpPort = Integer.parseInt(tf11.getText());
					int tcpPort = Integer.parseInt(tf12.getText());
					server = new Server(udpPort, tcpPort);
					card.show(c, "running");
				} catch (Exception e1) {
					l13.setText("Unable to create server");
				}
			}
			
		});
		p1.add(l11);
		p1.add(tf11);
		p1.add(l12);
		p1.add(tf12);
		p1.add(b11);
		p1.add(l13);
		
		p2 = new JPanel(new GridLayout(10, 1));
		l21 = new JLabel("Server running...");
		l22 = new JLabel("");
		progressBar = new JProgressBar(0, 100);
		b21 = new JButton("Close server");
		l23 = new JLabel("");
		b21.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				try {
					l23.setText("");
					server.exit();
					server = null;
					card.show(c, "start");
				} catch (Exception e1) {
					e1.printStackTrace();
					l23.setText("Error closing server");
				}
			}
			
		});
		
		p2.add(l21);
		p2.add(l22);
		p2.add(b21);
		p2.add(progressBar);
		p2.add(b21);
		p2.add(l23);
		
		
		c.add(p1, "start");
		c.add(p2, "running");
	}
	
	public static void main(String[] args) {
		String s = "asdf/asdf/fdg/file.txt";
		String[] parts = s.split("/");
		String fileName = parts[parts.length - 1];
		System.out.println(fileName);
//		SimonReceiver receiver = new SimonReceiver();
//		receiver.setSize(400, 400);
//		receiver.setDefaultCloseOperation(EXIT_ON_CLOSE);
//		receiver.setResizable(false);
//		receiver.setVisible(true);
	}
}

package cust;

import javax.swing.*;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class SimonSender extends JFrame {
	private Client client;
	private static JPanel p1, p2, p3;
	private CardLayout card;
	private Container c;
	private JButton b1, b21, b22, b23, b31, b32;
	private JLabel l1, l12,  l13, l14, l21, l22, l23, l31, l32;
	private JTextField tf1, tf12, tf13;
	private JRadioButton rb1, rb2;
	private File file;
	private String filePath;
	private JProgressBar progressBar;
	
	public SimonSender() {
		
		c = getContentPane();
		card = new CardLayout(40, 40);
		c.setLayout(card);
		c.setName("File sender");
		
		p1 = new JPanel(new GridLayout(10, 1));
		l1 = new JLabel("Address");
		l12 = new JLabel("UDP Port");
		l13 = new JLabel("TCP Port");
		l14 = new JLabel("");
		tf1 = new JTextField();
		tf12 = new JTextField();
		tf13 = new JTextField();
		b1 = new JButton("Connect");
		b1.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				try {
					String address = l1.getText();
					int udpPort = Integer.parseInt(tf12.getText());
					int tcpPort = Integer.parseInt(tf13.getText());
					client = new Client(udpPort, tcpPort, address);
					l14.setText("");
					
					card.show(c, "fileSelect");
				} catch (Exception e1) {
					e1.printStackTrace();
					l14.setText("Unable to connect to host");
				}
			}
			
		});
		
		p1.add(l1);
		p1.add(tf1);
		p1.add(l12);
		p1.add(tf12);
		p1.add(l13);
		p1.add(tf13);
		p1.add(b1);
		p1.add(l14);
		
		p2 = new JPanel(new GridLayout(10, 1));
		l21 = new JLabel("Choose file to send");
		l22 = new JLabel("");
		l23 = new JLabel("");
		b21 = new JButton("Select File");
		b22 = new JButton("Continue");
		b23 = new JButton("Back");
		b21.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				JFileChooser chooser = new JFileChooser(".");
				int result = chooser.showOpenDialog(c);
				if (result == JFileChooser.APPROVE_OPTION) {
					filePath = chooser.getSelectedFile().getAbsolutePath();
					l22.setText(filePath);
				} else {
					filePath = null;
				}
			}
			
		});
		b22.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				if (filePath != null) {
					l23.setText("");
					card.show(c, "protocolSelect");
				} else {
					l23.setText("You must first select a file!");
				}
			}
			
		});
		b23.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				client = null;
				card.show(c, "connect");
			}
			
		});
		
		p2.add(l21);
		p2.add(b21);
		p2.add(l22);
		p2.add(b22);
		p2.add(b23);
		p2.add(l23);
		
		p3 = new JPanel(new GridLayout(10, 1));
		l31 = new JLabel("Select protocal to use");
		l32 = new JLabel("");
		rb1 = new JRadioButton("RBUDP");
		rb2 = new JRadioButton("TCP");
		ButtonGroup bg = new ButtonGroup();
		b31 = new JButton("Send File");
		progressBar = new JProgressBar(0, 100);
		b32 = new JButton("Go back");
		b31.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				if (rb1.isSelected()) {
					l32.setText("RBUDP was selected");
					try {
						client.tcpSend("rbudp\n".getBytes());
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					Thread t = new Thread() {
						@Override
						public void run() {
							for (int i = 0; i <= 100; i+=3) {
								progressBar.setValue(i);
								try {
									Thread.sleep(70);
								} catch (Exception e2) {
									
								}
							}
							progressBar.setValue(100);
						}
					};
					t.start();
				} else if (rb2.isSelected()) {
					l32.setText("TCP was selected");
					try {
						client.tcpSend("tcp\n".getBytes());
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					Thread t = new Thread() {
						@Override
						public void run() {
							for (int i = 0; i < 100; i+=1) {
								progressBar.setValue(i);
								try {
									Thread.sleep(70);
								} catch (Exception e2) {
									
								}
							}
							progressBar.setValue(100);
						}
					};
					t.start();
				} else {
					l32.setText("No protocol was selected");
				}
			}
			
		});
		b32.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				card.show(c, "fileSelect");
			}
			
		});
		
		bg.add(rb1);
		bg.add(rb2);
		p3.add(rb1);
		p3.add(rb2);
		p3.add(progressBar);
		p3.add(b31);
		p3.add(l32);
		p3.add(b32);
		
		
		c.add("connect", p1);
		c.add("fileSelect", p2);
		c.add("protocolSelect", p3);
		
	}
	
	
	public static void main(String[] args) {
		SimonSender sender = new SimonSender();  
		sender.setSize(400,400);  
		sender.setResizable(false);
		sender.setVisible(true);
		sender.setDefaultCloseOperation(EXIT_ON_CLOSE);  
	}

}

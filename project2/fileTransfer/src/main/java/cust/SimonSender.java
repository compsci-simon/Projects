package cust;

import javax.swing.*;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

public class SimonSender extends JFrame {
	private Client client;
	private static JPanel p1, p2, p3, p4, p5;
	private CardLayout card;
	private Container c;
	private JButton b1, b21, b22, b23, b31, b32, b33;
	private JLabel l1, l12,  l13, l14, l21, l22, l23, l31, l32, l33, l34, l35, pingRes;
	private JTextField tf1, tf12, tf13;
	private JRadioButton rb1, rb2;
	private String filePath;
	private JProgressBar progressBar;
	ButtonGroup bg;
	byte[] fileBytes;
	JSlider packetSlider, blastLengthSlider;
	
	public SimonSender() {
		
		c = getContentPane();
		card = new CardLayout(40, 40);
		c.setLayout(card);
		setTitle("Client");
		
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
					String address = tf1.getText().strip();
					int udpPort = Integer.parseInt(tf12.getText());
					int tcpPort = Integer.parseInt(tf13.getText());
					
					client = new Client(udpPort, tcpPort, address);
					if (client.tcpConnect()) {
						l14.setText("");
						card.show(c, "fileSelect");
					} else {
						l14.setText("Cannot connect to server.");
					}
				} catch (Exception e1) {
					e1.printStackTrace();
					l14.setText("Unable to establish connection.");
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
		b23 = new JButton("Disconnect");
		b21.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				JFileChooser chooser = new JFileChooser(".");
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
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
					try {
						fileBytes = Client.readFileToBytes(filePath);
						l35.setText(String.format("%d bytes", fileBytes.length));
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				} else {
					l23.setText("You must first select a file!");
				}
			}
			
		});
		b23.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				try {
					client.tcpSend("quit\n");
				} catch (Exception e1) {
					e1.printStackTrace();
				}
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
		
		p3 = new JPanel(new GridLayout(13, 1));
		p4 = new JPanel(new GridLayout(1, 8));
		p5 = new JPanel(new GridLayout(1, 8));
		b33 = new JButton("Ping");
		pingRes = new JLabel("");
		p4.add(b33);
		p4.add(pingRes);
		l31 = new JLabel("Select protocal to use");
		l35 = new JLabel("");
		p5.add(l31);
		p5.add(l35);
		l32 = new JLabel("");
		l33 = new JLabel("Packet size (rbudp only)");
		l34 = new JLabel("Blast length (rbudp only)");
		packetSlider = new JSlider(JSlider.HORIZONTAL, 0, 64000, 10000);
		packetSlider.setMinorTickSpacing(2000);  
		packetSlider.setMajorTickSpacing(8000);  
		packetSlider.setPaintTicks(true);  
		packetSlider.setPaintLabels(true); 
		blastLengthSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 10);
		blastLengthSlider.setMinorTickSpacing(5);
		blastLengthSlider.setMajorTickSpacing(20);
		blastLengthSlider.setPaintTicks(true);
		blastLengthSlider.setPaintLabels(true);
		
		rb1 = new JRadioButton("RBUDP");
		rb2 = new JRadioButton("TCP");
		bg = new ButtonGroup();
		b31 = new JButton("Send File");
		progressBar = new JProgressBar(0, 100);
		b32 = new JButton("Back");
		b31.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				new Thread() {
					@Override
					public void run() {
						if (rb1.isSelected()) {
							try {
								l32.setText(String.format("%d", packetSlider.getValue()));
								String[] parts = filePath.split("/");
								handleProgressBar();
							    client.tcpSend("rbudp "+parts[parts.length-1]+"\n");
						        fileBytes = Client.readFileToBytes(filePath);
						        client.rbudpSend(fileBytes, packetSlider.getValue(), blastLengthSlider.getValue());
							} catch (Exception e1) {
								e1.printStackTrace();
							}
						} else if (rb2.isSelected()) {
							l32.setText("TCP was selected");
							try {
								byte[] file;
								String[] parts = filePath.split("/");
							    client.tcpSend("tcp "+parts[parts.length-1]+"\n");
								progressBar.setValue(100);
						        file = Client.readFileToBytes(filePath);
								client.tcpFileSend(file);
							} catch (Exception e1) {
								e1.printStackTrace();
							}
						} else {
							l32.setText("No protocol was selected");
						}
					}
				}.start();
			}
			
		});
		b32.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				filePath = null;
				progressBar.setValue(0);
				bg.clearSelection();
				l22.setText("");
				card.show(c, "fileSelect");
			}
			
		});
		b33.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
		      handlePing();
			}
			
		});
		
		p3.add(p4);
		p3.add(p5);
		bg.add(rb1);
		bg.add(rb2);
		p3.add(rb1);
		p3.add(l33);
		p3.add(packetSlider);
		p3.add(l34);
		p3.add(blastLengthSlider);
		p3.add(rb2);
		p3.add(progressBar);
		p3.add(b31);
		p3.add(l32);
		p3.add(b32);
		
		
		c.add("connect", p1);
		c.add("fileSelect", p2);
		c.add("protocolSelect", p3);
		
	}
	
	private void handlePing() {
      try {
    	  final long startTime = System.currentTimeMillis();
          client.tcpSend("ping\n");
          String res = client.tcpRecv();
          final long endTime = System.currentTimeMillis();
          pingRes.setText(String.format("\t%sms", endTime - startTime));
      } catch (Exception e) {
    	  e.printStackTrace();
      }
	}
	
	private void handleProgressBar() {

		new SwingWorker<Void, Integer>() {

			@Override
			protected Void doInBackground() throws Exception {
				int progress;
				while (((progress = (int) Math.ceil(client.getProgress()*100)) < 100)) {
					publish(progress);
				}
				return null;
			}
			
			@Override
			protected void process(List<Integer> chunks) {
				int lastVal = (int) chunks.get(chunks.size() - 1);
				System.out.println(lastVal);
				progressBar.setValue(lastVal);
			}
			
			@Override
			protected void done() {
				progressBar.setValue(100);
			}
			
			
		}.execute();
	}
	
	public void tcpSend() {
		String[] parts = filePath.split("/");
		String msg = parts[parts.length - 1];
		try {
			byte[] fileBytes = Client.readFileToBytes(filePath);
			client.tcpSend(("tcp "+msg+"\n").getBytes());
			client.tcpFileSend(fileBytes);
		} catch (Exception e1) {
			e1.printStackTrace();
			card.show(c, "connect");
			l14.setText("Server disconnected");
		}
	}
	
	
	public static void main(String[] args) throws Exception {
		SimonSender sender = new SimonSender();  
		sender.setSize(600, 600);  
		sender.setResizable(false);
		sender.setVisible(true);
		sender.setDefaultCloseOperation(EXIT_ON_CLOSE);
	}

}

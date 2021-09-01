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
	private JLabel l11, l12, l13, l14, l15, l21, l22, l23;
	private JTextField tf11, tf12;
	private JButton b11, b12, b21;
	private JProgressBar progressBar;
	Thread t;
	String outDir;
	
	public SimonReceiver() {
		
		c = getContentPane();
		card = new CardLayout(40, 40);
		c.setLayout(card);
		setTitle("Server");
		
		p1 = new JPanel(new GridLayout(10, 1));
		l11 = new JLabel("UDP Port");
		tf11 = new JTextField();
		l12 = new JLabel("TCP Port");
		tf12 = new JTextField();
		l14 = new JLabel("Select out dir");
		b12 = new JButton("Select dir");
		l15 = new JLabel("No dir selected");
		b11 = new JButton("Start server");
		l13 = new JLabel("");
		
		b11.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				try {
					if (outDir == null) {
						l13.setText("You must select an out dir");
						return;
					}
					l13.setText("");
					int udpPort = Integer.parseInt(tf11.getText());
					int tcpPort = Integer.parseInt(tf12.getText());
					server = new Server(udpPort, tcpPort);
					card.show(c, "running");

					transmissionHandler();
			
				} catch (Exception e1) {
					l13.setText("Unable to create server");
				}
			}
			
		});
		b12.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				JFileChooser chooser = new JFileChooser(".");
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int res = chooser.showOpenDialog(c);
				if (res == JFileChooser.APPROVE_OPTION) {
					outDir = chooser.getSelectedFile().getAbsolutePath()+"/";
					l15.setText(outDir);
				} else {
					outDir = null;
					l15.setText("");
				}
			}
			
		});
		
		p1.add(l11);
		p1.add(tf11);
		p1.add(l12);
		p1.add(tf12);
		p1.add(l14);
		p1.add(b12);
		p1.add(l15);
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
					l22.setText("");
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
	
	public void transmissionHandler() {
		new Thread() {
			@Override
			public void run() {
				try {
					
					Utils.logger("Waiting for tcp connection");
				    server.acceptTcpConnection();
				    Utils.logger("Received connection");
				    String msg;
				    while ((msg = server.tcpReceive()) != null) {
				    	if (msg.equals("quit"))
				    		break;
				    	l22.setText(msg);
					    String[] parts = msg.split(" ");
						int index = msg.indexOf(' ');
					    if (msg.substring(0, index).equals("rbudp")) {
					    	// Continue working here Handling the progress bar
					    	handleProgressBar();
						    byte[] fileByte = server.rbudpRecv();
						    if (fileByte == null)
						      return;
						    Server.writeFile(fileByte, outDir+msg.substring(index+1, msg.length()));
					    }
				    }
					server.closeTcp();
				    
				} catch (Exception e4) {
					e4.printStackTrace();
				}
			}
		}.start();
	}
	
	public void handleProgressBar() {
		new Thread () {
			@Override
			public void run() {
				int progress;
				while (((progress = (int) Math.ceil(server.getProgress()*100)) < 100)) {
					progressBar.setValue(progress);
				}
				progressBar.setValue(100);
			}
		}.start();
	}
	
	public void acceptConneciton() {
		System.out.println("Here");
		server.acceptTcpConnection();
		l23.setText("Accepted tcp connection");
		System.out.println("not Here");
	}
	
	public static void main(String[] args) {
		final SimonReceiver receiver = new SimonReceiver();
		receiver.setSize(400, 400);
		receiver.setDefaultCloseOperation(EXIT_ON_CLOSE);
		receiver.setResizable(false);
		receiver.setVisible(true);
	}
}
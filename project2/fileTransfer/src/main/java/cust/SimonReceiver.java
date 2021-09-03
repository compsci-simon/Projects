package cust;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;

import javax.swing.*;

public class SimonReceiver extends JFrame {
	private Server server;
	private JPanel p1, p2;
	private Container c;
	private CardLayout card;
	private JLabel l11, l12, l13, l14, l15, l16, l21, l22, l23;
	private JTextField tf11, tf12;
	private JButton b11, b12, b21;
	private JProgressBar progressBar;
	JSlider timeoutSlider;
	private Thread t;
	private String outDir;
	private static boolean receiveConnections = false;
	
	public SimonReceiver() {
		
		c = getContentPane();
		card = new CardLayout(40, 40);
		c.setLayout(card);
		setTitle("Server");
		
		p1 = new JPanel(new GridLayout(11, 1));
		l11 = new JLabel("UDP Port");
		tf11 = new JTextField();
		l12 = new JLabel("TCP Port");
		tf12 = new JTextField();
		l14 = new JLabel("Select out dir");
		b12 = new JButton("Select dir");
		l16 = new JLabel("Set UDP socket timeout");
		l15 = new JLabel("No dir selected");
		timeoutSlider = new JSlider(JSlider.HORIZONTAL, 20, 320, 70);
		timeoutSlider.setMinorTickSpacing(10);
		timeoutSlider.setMajorTickSpacing(30);
		timeoutSlider.setPaintTicks(true);
		timeoutSlider.setPaintLabels(true);
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
					server = new Server(udpPort, tcpPort, timeoutSlider.getValue());
					card.show(c, "running");
					receiveConnections = true;

					transmissionHandler();
			
				} catch (Exception e1) {
					e1.printStackTrace();
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
		p1.add(l16);
		p1.add(timeoutSlider);
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
				try {
					l23.setText("");
					server.exit();
					server = null;
					card.show(c, "start");
					l22.setText("");
					receiveConnections = false;
					
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
	
	private void transmissionHandler() {
		new Thread() {
			@Override
			public void run() {
				try {
					while (receiveConnections) {
						acceptConnection();
					    String msg;
					    while ((msg = server.tcpReceive()) != null) {
					    	if (server == null) {
					    		break;
					    	}
					    	if (msg.equals("quit")) {
					    		break;
					    	} else if (msg.equals("ping")) {
					    		server.tcpSend("ack\n");
					    		continue;
					    	} if (msg.isEmpty())
					    		continue;
							int index = msg.indexOf(' ');
						    if (msg.substring(0, index).equals("rbudp")) {
						    	l22.setText("Receiving file from rbudp...");
						    	handleProgressBar();
							    byte[] fileByte = server.rbudpRecv();
							    if (fileByte == null)
							      return;
							    Server.writeFile(fileByte, outDir+msg.substring(index+1, msg.length()));
						    	l22.setText(String.format("Received file. Packet success rate = %f", server.successRate));
						    	server.successRate = 0;
						    } else if (msg.substring(0, index).equals("tcp")) {
						    	l22.setText("Receiving file from tcp...");
						    	handleProgressBar();
							    byte[] fileByte = server.tcpReceiveFilev2();
							    if (fileByte == null)
							      return;
							    Server.writeFile(fileByte, outDir+msg.substring(index+1, msg.length()));
							    server.tcpSend("done\n");
						    	l22.setText("Received file.");
						    }
					    }
					    System.out.println("Client closed connection");
					    l22.setText("Client disconnected.");
						server.closeTcp();
					}
				    
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	private void handleProgressBar() {
		new SwingWorker<Void, Integer>() {

			@Override
			protected Void doInBackground() throws Exception {
				// TODO Auto-generated method stub
				int progress;
				while (((progress = (int) Math.ceil(server.getProgress()*100)) < 100)) {
					publish(progress);
				}
				return null;
			}
			
			@Override
			protected void process(List<Integer> chunks) {
				int lastVal = (int) chunks.get(chunks.size() - 1);
				progressBar.setValue(lastVal);
			}

			@Override
			protected void done() {
				progressBar.setValue(100);
			}
			
		}.execute();
	}
	
	public void acceptConnection() {
		Utils.logger("Waiting for tcp connection");
		if (server.acceptTcpConnection()) {
		    Utils.logger("Received connection");
		    l22.setText("Client connected.");
		}
	}
	
	public static void main(String[] args) {
		final SimonReceiver receiver = new SimonReceiver();
		receiver.setSize(400, 550);
		receiver.setDefaultCloseOperation(EXIT_ON_CLOSE);
		receiver.setResizable(false);
		receiver.setVisible(true);
	}
}

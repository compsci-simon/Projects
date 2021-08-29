package cust;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class ReceiverInterface {
	
	static JLabel chosen_file = new JLabel("");
	static JFrame receiver_frame = new JFrame("File Receiver");
	static JPanel main_panel = new JPanel();
	static JProgressBar progress_bar = new JProgressBar(0, 100);
	static String protocol;
	static Server receiver;
	
	public static void main(String[] args) throws Exception {
		receiver = new Server(5555, 5556);
	    receiver.acceptTcpConnection();
	    Utils.logger("Received tcp connection");
	    
	    while (true) {
	    	if ((protocol = receiver.tcpReceive()) == null) {
	    		System.exit(1);
	    	}
	    	if (protocol.compareTo("TCP") == 0) {
	    		/* Thread used for interface while main thread receives file */
	    		Thread x = new Thread() {
		    		public void run() {
		    			try {
							InitInterfaceImproved();
							UpdateInterface();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
		    		}
		    	};
		    	x.start();
		    	byte[] tcp_file_contents = receiver.tcpReceiveFile();
		    	if (tcp_file_contents == null) {
		    		break;
		    	}
		    	String path_tcp = "/home/jaco/tcp_receive.txt";
		    	receiver.writeFile(tcp_file_contents, path_tcp);
		    	
	    	} else if (protocol.compareTo("RBUDP") == 0) {
	    		/* Thread used for interface while main thread receives file */
	    		Thread x = new Thread() {
		    		public void run() {
		    			try {
							InitInterfaceImproved();
							UpdateInterface();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
		    		}
		    	};
		    	x.start();
	    		Utils.logger("Incoming RBUDP file");
	    		byte[] rbudp_file_contents = receiver.rbudpRecv();
		    	if (rbudp_file_contents == null) {
		    		break;
		    	}
		    	String path_rbudp = "/home/jaco/rbudp_receive.txt";
		    	receiver.writeFile(rbudp_file_contents, path_rbudp);
	    	}
	    }
	}
	
	public static void InitInterfaceImproved() throws InterruptedException {
		JLabel heading = new JLabel("Progress of receiving files");
		heading.setFont(new Font("Arial", Font.BOLD, 15));
		heading.setHorizontalAlignment(JLabel.CENTER);
		
		main_panel.setLayout(new BoxLayout(main_panel, BoxLayout.Y_AXIS));
		main_panel.add(heading, BorderLayout.CENTER);
		
		progress_bar.setStringPainted(true);
		progress_bar.setValue(0);
		main_panel.add(progress_bar);
		
		JButton exit_button = new JButton("Exit");
		exit_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Exit();
			}
		});
		
		receiver_frame.setSize(400, 400);
		receiver_frame.setLocationRelativeTo(null);
		receiver_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		receiver_frame.add(main_panel);
		receiver_frame.add(exit_button, BorderLayout.SOUTH);
		receiver_frame.setVisible(true);
	}
	
	public static void UpdateInterface() throws InterruptedException {
		main_panel.removeAll();
		
		JLabel heading = new JLabel("Progress of receiving files");
		heading.setFont(new Font("Arial", Font.BOLD, 15));
		heading.setHorizontalAlignment(JLabel.CENTER);
		main_panel.add(heading, BorderLayout.CENTER);
		
		progress_bar.setValue(0);
		main_panel.add(progress_bar);
		main_panel.repaint();
		receiver_frame.setVisible(true);
			int previous_progress = 0;
			int current_progress = 0;
			while (current_progress < 100) {
				previous_progress = current_progress;
				current_progress = (int) (receiver.ProgressRecv() * 100);
				if (previous_progress != current_progress) {
					progress_bar.setValue(current_progress);
					receiver_frame.setVisible(true);
				}
				/* TODO for some obscure reason the progress bar does not show without a print statement,
				 * still need to fix this
				 */
				System.out.print("");
			}
			progress_bar.setValue(100);
			receiver_frame.setVisible(true);
	}
	
	public static void Exit() {
		String message = "Sender disconnected";
		/* TODO have to send something to server to note that client disconnected */
		System.exit(1);
	}
}

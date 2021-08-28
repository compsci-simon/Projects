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
		    	InitInterface();
		    	byte[] tcp_file_contents = receiver.tcpReceiveFile();
		    	if (tcp_file_contents == null) {
		    		break;
		    	}
		    	String print = new String(tcp_file_contents);
		    	String path_tcp = "/home/jaco/tcp_receive.txt";
		    	receiver.writeFile(tcp_file_contents, path_tcp);
	    	} else if (protocol.compareTo("RBUDP") == 0) {
	    		Thread x = new Thread() {
	    	    	public void run() {
	    	        	InitInterface();
	    	    	}
	    	    };
	    		x.start();
		    	byte[] rbudp_file_contents = receiver.rbudpRecv();
		    	if (rbudp_file_contents == null) {
		    		break;
		    	}
		    	String print = new String(rbudp_file_contents);
		    	String path_tcp = "/home/jaco/rbudp_receive.txt";
		    	receiver.writeFile(rbudp_file_contents, path_tcp);
	    	}
	    }
	}
	
	public static void InitInterface() {	
		main_panel.removeAll();
		JFrame.setDefaultLookAndFeelDecorated(true);
		main_panel.setLayout(new BoxLayout(main_panel, BoxLayout.Y_AXIS));
		//main_panel.setLayout(new FlowLayout());
		JLabel heading = new JLabel("Progress of receiving files");
		heading.setFont(new Font("Arial", Font.BOLD, 15));
		heading.setHorizontalAlignment(JLabel.CENTER);
	
		main_panel.add(heading, BorderLayout.CENTER);
		main_panel.add(Box.createVerticalStrut(20));
		//main_panel.add(Box.createRigidArea(new Dimension(300,60)));
		final JProgressBar progress_bar = new JProgressBar();
		//progress_bar.setBounds(40, 40, 40, 40);
		progress_bar.setValue(0);
		receiver_frame.setVisible(true);
		progress_bar.setStringPainted(true);
		JButton exit_button = new JButton("Exit");
		exit_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Exit();
			}
		});
		if (protocol.compareTo("RBUDP") == 0) {
			main_panel.add(progress_bar);
		}
		main_panel.add(Box.createVerticalStrut(20));
		main_panel.add(exit_button, BorderLayout.SOUTH);
		main_panel.repaint();
		receiver_frame.setContentPane(main_panel);
		//sender_frame.add(main_panel, BorderLayout.CENTER);
		receiver_frame.setSize(400, 400);
		receiver_frame.setLocationRelativeTo(null);
		receiver_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		if (protocol.compareTo("RBUDP") == 0) {
				int previous_progress = 0;
				int current_progress = (int) (receiver.ProgressRecv() * 100);
				while (current_progress < 100) {
					if (previous_progress != current_progress) {
						System.out.println("hey");
						progress_bar.setValue(current_progress);
						receiver_frame.setVisible(true);
					}
					previous_progress = current_progress;
					current_progress = (int) (receiver.ProgressRecv() * 100);
					Utils.logger(current_progress);
				}
				progress_bar.setValue(100);
				receiver_frame.setVisible(true);
		}
		receiver_frame.setVisible(true);
	}
	
	public static void Exit() {
		String message = "Sender disconnected";
		/* have to send something to server to note that client disconnected */
		System.exit(1);
	}
}

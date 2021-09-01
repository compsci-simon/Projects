package cust;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
	static String outDir = "";
	static Font font = new Font("Arial", Font.BOLD, 16);
	
	public static void main(String[] args) throws Exception {
		receiver = new Server(5555, 5556);
	    InitSelectDirectory();
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
		    	String file_name = receiver.getFileName();
		    	String path_tcp = outDir + file_name;
		    	System.out.println(path_tcp);
		    	writeFile(tcp_file_contents, path_tcp);
		    	
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
		    	String file_name = receiver.getFileName();
		    	String path_rbudp = outDir + file_name;
		    	System.out.println(path_rbudp);
		    	writeFile(rbudp_file_contents, path_rbudp);
	    	}
	    }
	}
	
	public static void InitSelectDirectory() throws InterruptedException {
		final JFrame select_directory = new JFrame("Select directory");
		select_directory.setSize(400, 400);
		select_directory.setLocationRelativeTo(null);
		select_directory.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JLabel heading = new JLabel("Where do you want to save the file?");
		heading.setFont(font);
		
		JButton select_button = new JButton("Select Directory");
		select_button.setFont(font);
		select_button.setBackground(new Color(59, 89, 182));
        select_button.setForeground(Color.WHITE);
        select_button.setFocusPainted(false);
		select_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser directory_chooser = new JFileChooser();
				directory_chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int res = directory_chooser.showOpenDialog(null);
				if (res == JFileChooser.APPROVE_OPTION) {
					outDir = directory_chooser.getSelectedFile().getAbsolutePath()+"/";
					select_directory.setVisible(false);
				}
			}
		});
		
		JButton exit_button = new JButton("Exit");
		exit_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Exit();
			}
		});
		exit_button.setFont(font);
		exit_button.setBackground(new Color(59, 89, 182));
        exit_button.setForeground(Color.WHITE);
        exit_button.setFocusPainted(false);
        
		JPanel select_panel = new JPanel();
		select_panel.add(heading);
		select_panel.add(select_button);
		select_panel.add(Box.createVerticalStrut(200));
		select_directory.add(select_panel);
		select_directory.add(exit_button, BorderLayout.SOUTH);
		select_directory.setVisible(true);
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
		exit_button.setFont(font);
		exit_button.setBackground(new Color(59, 89, 182));
        exit_button.setForeground(Color.WHITE);
        exit_button.setFocusPainted(false);
		
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
		heading.setFont(font);
		heading.setHorizontalAlignment(JLabel.CENTER);
		main_panel.add(heading, BorderLayout.CENTER);
		main_panel.add(Box.createVerticalStrut(100));
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
	
	/*
	   * Used when the file has been received by rbudpRecv to write the file to the servers
	   * filesystem.
	   */
	public static void writeFile(byte[] fileBytes, String path) throws Exception {
	    Path newPath = Paths.get(path);
	    Files.write(newPath, fileBytes);
	 }
	
	public static void Exit() {
		String message = "Sender disconnected";
		/* TODO have to send something to server to note that client disconnected */
		System.exit(1);
	}
}

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
	static JFrame sender_frame = new JFrame("File Receiver");
	static JPanel main_panel = new JPanel();
	
	static Server s;
	
	public static void main(String[] args) throws Exception {
		Server s = new Server(5555, 5556, 5557);
	    InitInterface();
	    s.acceptTcpConnection();
	    Utils.logger("Received connection");
	    s.acceptFileTcpConnection();
	    Utils.logger("Received connection again");
	    while (true) {
	    	byte[] tcp_file_contents = s.tcpReceiveFile();
	    	if (tcp_file_contents == null) {
	    		break;
	    	}
	    	String print = new String(tcp_file_contents);
	    	//System.out.println(print);
	    	String path_tcp = "/home/jaco/tcp_receive.txt";
	    	s.writeFile(tcp_file_contents, path_tcp);
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
		JProgressBar progress_bar = new JProgressBar();
		//progress_bar.setBounds(40, 40, 40, 40);
		progress_bar.setValue(15);
		progress_bar.setStringPainted(true);
		JButton exit_button = new JButton("Exit");
		exit_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Exit();
			}
		});
		main_panel.add(progress_bar);
		main_panel.add(Box.createVerticalStrut(20));
		main_panel.add(exit_button, BorderLayout.SOUTH);
		main_panel.repaint();
		sender_frame.setContentPane(main_panel);
		//sender_frame.add(main_panel, BorderLayout.CENTER);
		sender_frame.setSize(400, 400);
		sender_frame.setLocationRelativeTo(null);
		sender_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		sender_frame.setVisible(true);
	}
	
	public static void Exit() {
		String message = "Sender disconnected";
		/* have to send something to server to note that client disconnected */
		System.exit(1);
	}
}

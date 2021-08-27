package cust;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class SenderInterface extends JFrame {
	static JLabel chosen_file = new JLabel("");
	static JFrame sender_frame = new JFrame("File Sender");
	static JPanel main_panel = new JPanel();
	
	static Client sender;
	
	public static void main(String[] args) throws Exception {
		try {
			sender = new Client(5555, 5556, 5557, "localhost");
			System.out.println("Trying to establish TCP connection");
		      if (!sender.tcpFileConnect() || !sender.tcpConnect()) {
		        System.out.println("Failed to connect");
		        return;
		      }
		      System.out.println("Successfully connected");
		      InitInterface();
		    } catch (Exception e) {
		      e.printStackTrace();
		    }
	}
	
	public static void InitInterface() {	
		main_panel.removeAll();
		JFrame.setDefaultLookAndFeelDecorated(true);
		//main_panel.setLayout(new BoxLayout(main_panel, BoxLayout.Y_AXIS));
		main_panel.setLayout(new FlowLayout());
		
		JLabel heading = new JLabel("Select your file");
		heading.setFont(new Font("Arial", Font.BOLD, 15));
		heading.setHorizontalAlignment(JLabel.CENTER);
		
		JButton select_button = new JButton("Select file");
		select_button.setFont(new java.awt.Font("Arial", Font.BOLD, 15));
		//select_button.setPreferredSize(new Dimension(50,50));
		select_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ChooseFile();
			}
		});
		
		main_panel.add(heading);
		main_panel.add(Box.createRigidArea(new Dimension(300,60)));
		main_panel.add(select_button);
		main_panel.repaint();
		sender_frame.add(main_panel, BorderLayout.CENTER);
		sender_frame.setSize(400, 400);
		sender_frame.setLocationRelativeTo(null);
		sender_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JButton exit_button = new JButton("Exit");
		exit_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Exit();
			}
		});
		sender_frame.add(exit_button, BorderLayout.SOUTH);
		sender_frame.setVisible(true);
	}
	
	public static void ChooseFile() {
		JFileChooser fileChooser = new JFileChooser();
		int result = fileChooser.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
            /* set the label to the path of the selected file */
            chosen_file.setText(fileChooser.getSelectedFile().getAbsolutePath());
        } else {
            chosen_file.setText("Cancelled");
            InitInterface();
            return;
        }
		
		JButton send_button = new JButton("Send");
		send_button.setFont(new java.awt.Font("Arial", Font.BOLD, 15));
		send_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					SendFile(chosen_file.getText());
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		
		main_panel.add(Box.createRigidArea(new Dimension(350,60)));
		main_panel.add(chosen_file);
		main_panel.add(Box.createRigidArea(new Dimension(350,60)));
		main_panel.add(send_button);
		sender_frame.setVisible(true);
     }
	
	public static void SendFile(String path) throws Exception {
		System.out.println("Sending file");
		byte[] file = sender.readFileToBytes(path);
	    sender.tcpFileSend(file);
	    
	    //sender.rbudpSend(file);
	    
	    /* refresh interface */
		InitInterface();
	}
	
	public static void Exit() {
		String message = "Sender disconnected";
		/* have to send something to server to note that client disconnected */
		System.exit(1);
	}
}


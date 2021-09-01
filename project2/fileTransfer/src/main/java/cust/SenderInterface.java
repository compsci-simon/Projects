package cust;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class SenderInterface {
	
	static JLabel chosen_file = new JLabel("");
	static JFrame sender_frame = new JFrame("File Sender");
	static JFrame protocol_frame = new JFrame("Protocol Frame");
	static JPanel main_panel = new JPanel();
	static Client sender;
	static String protocol;
	static Font font = new Font("Arial", Font.BOLD, 16);

	public static void main(String[] args) throws Exception {
		sender = new Client(5555, 5556, "localhost");
		if (!sender.tcpConnect()) {
			Utils.logger("Failed to connect");
			return;
		}
		System.out.println("tcp connected");
		InitInterfaceProtocol();
	}

	public static void InitInterfaceProtocol() {
		JLabel heading = new JLabel("Select your protocol");
		heading.setFont(new Font("Arial", Font.BOLD, 15));
		heading.setHorizontalAlignment(JLabel.CENTER);
		
		JPanel protocol_panel = new JPanel();
		
		JButton tcp_button = new JButton("TCP");
		tcp_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				protocol_frame.setVisible(false);
				try {
					tcpSetup();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		tcp_button.setFont(font);
		tcp_button.setBackground(new Color(59, 89, 182));
        tcp_button.setForeground(Color.WHITE);
        tcp_button.setFocusPainted(false);
		
		JButton rbudp_button = new JButton("RBUDP");
		rbudp_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				protocol_frame.setVisible(false);
				try {
					rbudpSetup();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		rbudp_button.setFont(font);
		rbudp_button.setBackground(new Color(59, 89, 182));
        rbudp_button.setForeground(Color.WHITE);
        rbudp_button.setFocusPainted(false);
		
		protocol_panel.add(tcp_button);
		protocol_panel.add(rbudp_button);
		
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
		
		protocol_frame.add(protocol_panel);
		protocol_frame.add(exit_button, BorderLayout.SOUTH);
		protocol_frame.setSize(400, 400);
		protocol_frame.setLocationRelativeTo(null);
		protocol_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		protocol_frame.setVisible(true);
	}
	
	public static void tcpSetup() throws IOException {
		protocol = "TCP";
		sender.tcpSend("TCP\n".getBytes());
		InitInterfaceSelect();	
	}
	
	public static void rbudpSetup() throws IOException {
		protocol = "RBUDP";
		sender.tcpSend("RBUDP\n".getBytes());
		InitInterfaceSelect();
	}
	
	public static void InitInterfaceSelect() {
		main_panel.removeAll();
		JFrame.setDefaultLookAndFeelDecorated(true);
		//main_panel.setLayout(new BoxLayout(main_panel, BoxLayout.Y_AXIS));
		main_panel.setLayout(new FlowLayout());
		
		JLabel heading = new JLabel("Select your file");
		heading.setFont(new Font("Arial", Font.BOLD, 20));
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
		select_button.setFont(font);
		select_button.setBackground(new Color(59, 89, 182));
        select_button.setForeground(Color.WHITE);
        select_button.setFocusPainted(false);
		
		main_panel.add(heading);
		main_panel.add(Box.createRigidArea(new Dimension(300,60)));
		main_panel.add(select_button);
		main_panel.repaint();
		
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
		
		sender_frame.add(main_panel, BorderLayout.CENTER);
		sender_frame.setSize(400, 400);
		sender_frame.setLocationRelativeTo(null);
		sender_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		sender_frame.add(exit_button, BorderLayout.SOUTH);
		sender_frame.setVisible(true);
	}
	public static void ChooseFile() {
		JFileChooser fileChooser = new JFileChooser();
		int result = fileChooser.showOpenDialog(null);
		final String file_path;
		if (result == JFileChooser.APPROVE_OPTION) {
            /* set the label to the path of the selected file */
            //chosen_file.setText(fileChooser.getSelectedFile().getAbsolutePath());
			file_path = fileChooser.getSelectedFile().getAbsolutePath();
		} else {
            chosen_file.setText("Cancelled");
            file_path = "";
            InitInterfaceSelect();
            return;
        }
		String[] path = file_path.split("/");
		String file_name = path[path.length-1];
		chosen_file.setText(file_name);
		JButton send_button = new JButton("Send");
		send_button.setFont(new java.awt.Font("Arial", Font.BOLD, 15));
		send_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					SendFile(file_path);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		send_button.setFont(font);
		send_button.setBackground(new Color(59, 89, 182));
        send_button.setForeground(Color.WHITE);
        send_button.setFocusPainted(false);
        
		main_panel.add(Box.createRigidArea(new Dimension(350,60)));
		main_panel.add(chosen_file);
		main_panel.add(Box.createRigidArea(new Dimension(350,60)));
		main_panel.add(send_button);
		sender_frame.setVisible(true);
     }
	public static void SendFile(String path) throws Exception {
		sender.setFileName(chosen_file.getText());
		System.out.println("Sending file");
		byte[] file = sender.readFileToBytes(path);
		if (protocol.compareTo("TCP") == 0) {
			sender.tcpFileSend(file);
		} else if (protocol.compareTo("RBUDP") == 0) {
			int packetsize = 10000;
			int blastlength = 30;
			sender.rbudpSend(file, packetsize, blastlength);
		}
		System.out.println("File sent");
		/* refresh interface */
		sender_frame.setVisible(false);
		InitInterfaceProtocol();
	}
	public static void Exit() {
		String message = "Sender disconnected";
		/* have to send something to server to note that client disconnected */
		System.exit(1);
	}
}
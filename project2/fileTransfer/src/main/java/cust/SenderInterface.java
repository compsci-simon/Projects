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
	static JFrame protocol_frame = new JFrame("Protocol Frame");
	static JPanel main_panel = new JPanel();
	static Client sender;

	public static void main(String[] args) throws Exception {
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
				tcpSetup();
			}
		});
		JButton rbudp_button = new JButton("RBUDP");
		rbudp_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				protocol_frame.setVisible(false);
				rbudpSetup();
			}
		});
		protocol_panel.add(tcp_button);
		protocol_panel.add(rbudp_button);
		JButton exit_button = new JButton("Exit");
		exit_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Exit();
			}
		});
		protocol_frame.add(protocol_panel);
		protocol_frame.add(exit_button, BorderLayout.SOUTH);
		protocol_frame.setSize(400, 400);
		protocol_frame.setLocationRelativeTo(null);
		protocol_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		protocol_frame.setVisible(true);
	}
	public static void tcpSetup() {
		try {
			sender = new Client(5555, 5556, 5557, "localhost");
			if (!sender.tcpConnect()) {
				Utils.logger("Failed to connect");
				return;
			}
			sender.tcpSend("TCP\n".getBytes());
			try {
				sender.tcpFileConnect();
			}	catch (Exception ex) {
			    ex.printStackTrace();
		}
			InitInterfaceSelect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void rbudpSetup() {
		try {
			sender = new Client(5555, 5556, 5557, "localhost");
			if (!sender.tcpConnect()) {
				Utils.logger("Failed to connect");
				return;
			}
			sender.tcpSend("RBUDP\n".getBytes());
			InitInterfaceSelect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void InitInterfaceSelect() {
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
            InitInterfaceSelect();
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
		InitInterfaceSelect();
	}
	public static void Exit() {
		String message = "Sender disconnected";
		/* have to send something to server to note that client disconnected */
		System.exit(1);
	}
}
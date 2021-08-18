import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ClientInterface extends JFrame {
	Client client;
	JFrame frame = new JFrame();
	JTextField username_text = new JTextField();
	JTextField password_text = new JTextField();
	JButton login_button = new JButton("Login");
	
	public ClientInterface() {
		this.client = new Client("localhost", 9005);
		frame.setSize(400,500);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel login_panel = new JPanel();
		login_panel.setLayout(new BoxLayout(login_panel, BoxLayout.Y_AXIS));
		login_panel.add(username_text);
		login_panel.add(password_text);
		login_panel.add(login_button);
		
		login_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				ClientLogin();
			}
		});
		frame.add(login_panel, BorderLayout.SOUTH);
		frame.setVisible(true);
		}
	
	public void ClientLogin() {
		/*
		if (client.connectToServer()) {
			System.out.println("Client connected");
		} else {
			System.out.println("Client could not connect");
			return;
		}
		*/
		String username = username_text.getText();
		String password = password_text.getText();
		System.out.println(username);
		System.out.println(password);
	}
	
	public static void main(String[] args) {
		ClientInterface client_interface = new ClientInterface();
	}
	
}

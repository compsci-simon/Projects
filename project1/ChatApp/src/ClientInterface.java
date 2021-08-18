import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ClientInterface extends JFrame {
	Client client;
	JFrame connection_frame = new JFrame();
	JFrame login_frame = new JFrame();
	JTextField username_text = new JTextField();
	JTextField password_text = new JTextField();
	JButton login_button = new JButton("Login");
	JTextField address_text = new JTextField();
	JTextField port_text = new JTextField();
	JButton conn_button = new JButton("Connect");
	
	public ClientInterface() {
		connection_page();
		Client client;
		
		//this.client = new Client("localhost", 9005);
	}
	
	public void connection_page() {
		connection_frame.setSize(400,500);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel conn_panel = new JPanel();
		conn_panel.setLayout(new BoxLayout(conn_panel, BoxLayout.Y_AXIS));
		conn_panel.add(address_text);
		conn_panel.add(port_text);
		conn_panel.add(conn_button);
		
		conn_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ClientConnect();
			}
		});
		connection_frame.add(conn_panel, BorderLayout.SOUTH);
		connection_frame.setVisible(true);
	}
	
	public void login_page() {
		login_frame.setSize(400,500);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel login_panel = new JPanel();
		login_panel.setLayout(new BoxLayout(login_panel, BoxLayout.Y_AXIS));
		login_panel.add(username_text);
		login_panel.add(password_text);
		login_panel.add(login_button);
		
		login_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					ClientLogin();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		login_frame.add(login_panel, BorderLayout.SOUTH);
		login_frame.setVisible(true);
	}
	
	public void ClientConnect() {
		String address = address_text.getText();
		int port = Integer.parseInt(port_text.getText());
		System.out.println(address);
		System.out.println(port);
		this.client = new Client(address, port);
		if (client.connectToServer()) {
			System.out.println("Client connected");
			connection_frame.setVisible(false);
			login_page();
		} else {
			System.out.println("Client could not connect");
			return;
		}
	}
	
	public void ClientLogin() throws IOException {
		String username = username_text.getText();
		String password = password_text.getText();
		System.out.println(username);
		System.out.println(password);
		this.client.login(username, password);
	}
	
	public static void main(String[] args) {
		ClientInterface client_interface = new ClientInterface();
	}
	
}

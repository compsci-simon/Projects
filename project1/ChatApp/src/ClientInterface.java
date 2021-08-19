import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ClientInterface extends JFrame {
	simpleClient client;
	JFrame connection_frame = new JFrame();
	JFrame login_frame = new JFrame();
	JFrame home_frame = new JFrame();
	JTextField username_text = new JTextField();
	JTextField password_text = new JTextField();
	JButton login_button = new JButton("Login");
	JTextField address_text = new JTextField();
	JTextField port_text = new JTextField();
	JButton conn_button = new JButton("Connect");
	JTextField message_text = new JTextField();
	JTextField destination_text = new JTextField();
	JButton send_button = new JButton("Send");
	JLabel conn_suc = new JLabel("You are connected to the server");
	
	public ClientInterface() throws IOException {
		connection_page();
		
	}
	
	public void connection_page() {
		connection_frame.setSize(400,500);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel conn_panel = new JPanel();
		conn_panel.setLayout(new BoxLayout(conn_panel, BoxLayout.Y_AXIS));
		JLabel address = new JLabel("address");
		conn_panel.add(address);
		conn_panel.add(address_text);
		JLabel port = new JLabel("port");
		conn_panel.add(port);
		conn_panel.add(port_text);
		conn_panel.add(conn_button);
		connection_frame.add(conn_panel, BorderLayout.SOUTH);
		connection_frame.setVisible(true);
		
		conn_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ClientConnect();
			}
		});
	}
	
	public void login_page() {
		login_frame.setSize(400,500);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel login_panel = new JPanel();
		login_panel.setLayout(new BoxLayout(login_panel, BoxLayout.Y_AXIS));
		JLabel username = new JLabel("username");
		login_panel.add(username);
		login_panel.add(username_text);
		JLabel password = new JLabel("password");
		login_panel.add(password);
		login_panel.add(password_text);
		login_panel.add(login_button);
		login_frame.add(conn_suc, BorderLayout.CENTER);
		login_frame.add(login_panel, BorderLayout.SOUTH);
		login_frame.setVisible(true);
		
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
	}
	
	public void ClientConnect() {
		String address = address_text.getText();
		int port = Integer.parseInt(port_text.getText());
		this.client = new simpleClient(address, port);
		if (client.connect()) {
			System.out.println("Client connected");
			connection_frame.setVisible(false);
			login_page();
		} else {
			JLabel conn_fail = new JLabel("Failed to connect, provide correct address and port");
			connection_frame.add(conn_fail, BorderLayout.CENTER);
			connection_frame.setVisible(true);
			System.out.println("Client could not connect");
			return;
		}
	}
	
	public void ClientLogin() throws IOException {
		String username = username_text.getText();
		String password = password_text.getText();
		if(this.client.login(username, password)) {
			login_frame.setVisible(false);
			home_page();
		} else {
			JLabel login_fail = new JLabel("Failed to login, provide correct username and password");
			login_frame.add(login_fail, BorderLayout.CENTER);
			conn_suc.setVisible(false);
			login_frame.setVisible(true);
			System.out.println("Client could not login");
		}
	}
	
	public void home_page() {
		home_frame.setSize(400,500); 
		JButton ListUsers_button = new JButton("List online users");
		home_frame.add(ListUsers_button);
		JLabel Message = new JLabel("Message");
		JPanel home_panel = new JPanel();
		home_panel.setLayout(new BoxLayout(home_panel, BoxLayout.Y_AXIS));
		home_panel.add(Message);
		home_panel.add(message_text);
		JLabel destination = new JLabel("To");
		home_panel.add(destination);
		home_panel.add(destination_text);
		home_panel.add(send_button);
		JButton Exit_button = new JButton("Exit");
		
		home_frame.add(ListUsers_button, BorderLayout.NORTH);
		home_frame.add(home_panel);
		home_frame.add(Exit_button, BorderLayout.SOUTH);
		
		home_frame.setVisible(true);
		
		send_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					SendMessage();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		ListUsers_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListUsers();
			}
		});
		Exit_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Exit();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
	}
	public void SendMessage() throws IOException {
		String destination = destination_text.getText();
		String message = message_text.getText();
		//client.send_message(destination, message);
		client.broadcast_message(message);
		System.out.println("Sent");
	}
	
	public void ListUsers() {
		System.out.println("listing online users");
	}
	public void Exit() throws IOException {
		client.quit();
		System.out.println("Exit");
	}
	
	public static void main(String[] args) throws IOException {
		ClientInterface client_interface = new ClientInterface();
	}
	
}

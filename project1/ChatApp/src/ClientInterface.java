import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ClientInterface extends JFrame {
	simpleClient client;
	JFrame connection_frame = new JFrame("Connect");
	JFrame login_frame = new JFrame("Login");
	JFrame home_frame = new JFrame("Home");
	JFrame news_frame = new JFrame("News");
	JFrame list_frame = new JFrame("Online Users");
	JTextField username_text = new JTextField();
	JTextField password_text = new JTextField();
	JButton login_button = new JButton("Login");
	JTextField address_text = new JTextField();
	JTextField port_text = new JTextField();
	JButton conn_button = new JButton("Connect");
	JTextField message_text = new JTextField();
	JTextField destination_text = new JTextField();
	JButton send_button = new JButton("Send");
	JButton broadcast_button = new JButton("Broadcast");
	JLabel conn_suc = new JLabel("You are connected to the server", JLabel.CENTER);
	JLabel user_list = new JLabel("");
	JLabel list_label = new JLabel("");
	BufferedReader clientIn;
	
	public ClientInterface() throws IOException {
		connection_page();
		/*
		client = new simpleClient("localhost", 9005);
		client.connect();
		login_page();
		*/
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
				}
			}
		});
	}
	
	public void home_page() {
		home_frame.setSize(400,500); 
		JButton ListUsers_button = new JButton("List online users");
		home_frame.add(ListUsers_button);
		JLabel Message = new JLabel("Message: ", JLabel.CENTER);
		JPanel home_panel = new JPanel();
		home_panel.setLayout(new BoxLayout(home_panel, BoxLayout.Y_AXIS));
		home_panel.add(Message);
		home_panel.add(message_text);
		JLabel destination = new JLabel("To: ", JLabel.CENTER);
		home_panel.add(destination);
		home_panel.add(destination_text);
		home_panel.add(send_button, BorderLayout.WEST);
		home_panel.add(broadcast_button, BorderLayout.EAST);
		JButton Exit_button = new JButton("Exit");
		
		home_frame.add(ListUsers_button, BorderLayout.NORTH);
		home_frame.add(home_panel);
		//home_frame.add(send_button, BorderLayout.WEST);
		//home_frame.add(broadcast_button, BorderLayout.EAST);
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
		
		broadcast_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					BroadcastMessage();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		
		ListUsers_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					ListUsers();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
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
	
	public void list_page(String users) {
		list_frame.remove(user_list);
		list_frame.remove(list_label);
		list_frame.setSize(400,500); 
		list_label = new JLabel("All of the online users:", JLabel.CENTER);
		list_label.setFont(new Font("Arial", Font.BOLD, 20));
		user_list = new JLabel(users, JLabel.CENTER);
		user_list.setFont(new Font("Arial", Font.BOLD, 15));
		list_frame.add(list_label, BorderLayout.NORTH);
		list_frame.add(user_list, BorderLayout.CENTER);
		list_frame.setVisible(true);
	}
	
	
	public void news_page() throws IOException {
		news_frame.setSize(400,500); 
		JLabel news_label = new JLabel("Your news will arrive here", JLabel.CENTER);
		news_frame.add(news_label, BorderLayout.NORTH);
		JLabel new_news = new JLabel("");
		Thread t = new Thread() {
			public void run() {
				try {
					String line;
					clientIn = new BufferedReader(new InputStreamReader(client.clientSock.getInputStream()));
					while ((line = clientIn.readLine()) != null) {
						System.out.println(line);
						if (line.contains("List of users:")) {
							list_page(line);
						} else {
							String add = "<html>" + new_news.getText() + "<br>" + line + "<html>";
							new_news.setText(add);
							news_frame.add(new_news, BorderLayout.CENTER);
							news_frame.setVisible(true);
						}
						if (line.equals("Successfully logged out")) {
							break;
						}
					}
				} catch (IOException e) {
				System.out.println("Could not create Thread");
				}
			}
		};
		t.start();
		news_frame.setVisible(true);
	}
	
	public void ClientConnect() {
		String address = address_text.getText();
		int port = 0;
		try {
	      	port = Integer.parseInt(port_text.getText());
		} catch (NumberFormatException e) {
		}
		this.client = new simpleClient(address, port);
		if (client.connect()) {
			System.out.println("Client connected");
			connection_frame.setVisible(false);
			login_page();
		} else {
			JLabel conn_fail = new JLabel("Failed to connect, provide valid address and port", JLabel.CENTER);
			connection_frame.add(conn_fail, BorderLayout.CENTER);
			connection_frame.setVisible(true);
			System.out.println("Client could not connect");
			return;
		}
	}
	
	public void ClientLogin() throws IOException {
		String username = username_text.getText();
		String password = password_text.getText();
		if(client.login(username, password)) {
			login_frame.setVisible(false);
			home_page();
			news_page();
			list_frame.add(list_label);
			list_frame.add(user_list);
		} else {
			JLabel login_fail = new JLabel("Failed to login, provide correct username and password");
			login_frame.add(login_fail, BorderLayout.CENTER);
			conn_suc.setVisible(false);
			login_frame.setVisible(true);
			System.out.println("Client could not login");
		}
	}
	
	public void SendMessage() throws IOException {
		String destination = destination_text.getText();
		String message = message_text.getText();
		client.send_message(destination, message);
	}
	
	public void BroadcastMessage() throws IOException {
		String message = message_text.getText();
		client.broadcast_message(message);
	}
	
	public void ListUsers() throws IOException {
		client.list_users();
	}
	public void Exit() throws IOException {
		client.quit();
		home_frame.setVisible(false);
		news_frame.setVisible(false);
		System.out.println("Exit");
		System.exit(0);
	}
	
	public static void main(String[] args) throws IOException {
		ClientInterface client_interface = new ClientInterface();
	}
}

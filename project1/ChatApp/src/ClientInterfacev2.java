import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;


public class ClientInterfacev2 extends JFrame {
	simpleClient client;
	int port;
	String hostAddress;
	Container container;
	CardLayout card;
	JPanel connectionPane, loginPane, chatPane;
	
	public ClientInterfacev2() {
		container = getContentPane();
		card = new CardLayout(30, 30);
		container.setLayout(card);
		
		setupConnectScreen();
		
		setupLoginScreen();
		
		setupChatScreen();
		
		container.add("chat", chatPane);
		container.add("connect", connectionPane);
		container.add("login", loginPane);
		
	}
	
	private void setupConnectScreen() {
		JLabel serverAddressLabel, portLabel, errorLabel;
		JButton connectButton;
		JTextField addressField, portField;
		
		connectionPane = new JPanel(new GridLayout(10, 1));
		serverAddressLabel = new JLabel("Server address");
		addressField = new JTextField();
		portLabel = new JLabel("Server port");
		portField = new JTextField();
		connectButton = new JButton("Connect to the server");
		errorLabel = new JLabel("");
		
		connectButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				try {
					int port = Integer.parseInt(portField.getText());
					String address = addressField.getText();
					client = new simpleClient(address, port);
					if (!client.connect()) {
						errorLabel.setText("Failed to connect to the server.");
						return;
					} else {
						addressField.setText("");
						portField.setText("");
						errorLabel.setText("");
						card.show(container, "login");
					}
					
				} catch (Exception e1) {
					e1.printStackTrace();
					errorLabel.setText("Failed to connect to the server.");
				}
			}
			
		});

		connectionPane.add(serverAddressLabel);
		connectionPane.add(addressField);
		connectionPane.add(portLabel);
		connectionPane.add(portField);
		connectionPane.add(connectButton);
		connectionPane.add(errorLabel);
	}
	
	public void setupLoginScreen() {
		loginPane = new JPanel(new GridLayout(10, 1));
		JLabel usernameLabel, passwordLabel, loginErrorLabel;
		usernameLabel = new JLabel("Username");
		passwordLabel = new JLabel("Password");
		JTextField usernameField, passwordField;
		usernameField = new JTextField();
		passwordField = new JTextField();
		loginErrorLabel = new JLabel("");
		JButton loginButton = new JButton("Login");
		
		loginButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				try {
					if (client.login(usernameField.getText(), passwordField.getText())) {
						loginErrorLabel.setText("");
						card.show(container, "chat");
					} else {
						loginErrorLabel.setText("Incorrect username password combination.");
					}
				} catch (Exception error) {
					error.printStackTrace();
					loginErrorLabel.setText("Error loging in.");
				}
			}
			
		});

		loginPane.add(usernameLabel);
		loginPane.add(usernameField);
		loginPane.add(passwordLabel);
		loginPane.add(passwordField);
		loginPane.add(loginButton);
		loginPane.add(loginErrorLabel);
	}
	
	private void setupChatScreen() {
		JLabel outputLabel, listOfUsersLabel;
		JScrollPane scrollPane;
		JTextField messageField;
		JButton sendButton, bcastButton;
		JPanel southPanel, eastPanel;
		
		outputLabel = new JLabel("Welcome to the server!");
		scrollPane = new JScrollPane();
		scrollPane.add(outputLabel);
		
		eastPanel = new JPanel(new FlowLayout());
		listOfUsersLabel = new JLabel("List of users:");
		eastPanel.add(listOfUsersLabel);
		
		southPanel = new JPanel(new GridLayout(3, 1));
		messageField = new JTextField();
		sendButton = new JButton("Send");
		bcastButton = new JButton("Broadcast");
		southPanel.add(messageField);
		southPanel.add(sendButton);
		southPanel.add(bcastButton);
		
		chatPane = new JPanel(new BorderLayout());
		chatPane.add(scrollPane, BorderLayout.CENTER);
		chatPane.add(eastPanel, BorderLayout.EAST);
		chatPane.add(southPanel, BorderLayout.SOUTH);
		
	}
	
	public static void main(String[] args) {
		ClientInterfacev2 gui = new ClientInterfacev2();
		gui.setSize(400, 400);
		gui.setDefaultCloseOperation(EXIT_ON_CLOSE);
		gui.setResizable(false);
		gui.setVisible(true);
	}
}

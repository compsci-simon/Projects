import java.awt.CardLayout;
import java.awt.Container;
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
	JPanel p1;
	JLabel l11, l12, l13;
	JButton b1;
	JTextField tf11, tf12;
	
	public ClientInterfacev2(int port, String hostAddress) {
		container = getContentPane();
		card = new CardLayout(30, 30);
		container.setLayout(card);
		
		setupConnectScreen();
		
		setupChatScreen();
		
		container.add("connect", p1);
		
	}
	
	private void setupChatScreen() {
		
	}
	
	private void setupConnectScreen() {
		p1 = new JPanel(new GridLayout(10, 1));
		l11 = new JLabel("Server address");
		tf11 = new JTextField();
		l12 = new JLabel("Server port");
		tf12 = new JTextField();
		b1 = new JButton("Connect to the server");
		l13 = new JLabel("");
		
		b1.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				try {
					int port = Integer.parseInt(tf12.getText());
					if (!client.connect()) {
						l13.setText("Failed to connect to the server.");
						return;
					}
					
				} catch (Exception e1) {
					e1.printStackTrace();
					l13.setText("Failed to connect to the server.");
				}
			}
			
		});

		p1.add(l11);
		p1.add(tf11);
		p1.add(l12);
		p1.add(tf12);
		p1.add(b1);
		p1.add(l13);
	}
	
	public static void main
}

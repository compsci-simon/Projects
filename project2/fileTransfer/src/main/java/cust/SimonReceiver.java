package cust;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

public class SimonReceiver extends JFrame {
	private Server s;
	private JPanel p1, p2;
	private Container c;
	private CardLayout card;
	private JLabel l11, l12, l13, l21, l22;
	private JTextField tf11, tf12;
	private JButton b11, b21;
	JProgressBar progressBar;
	
	public SimonReceiver() {
		c = getContentPane();
		card = new CardLayout(40, 40);
		c.setLayout(card);
		
		p1 = new JPanel(new GridLayout(10, 1));
		l11 = new JLabel("UDP Port");
		l12 = new JLabel("TCP Port");
		l13 = new JLabel("");
		tf11 = new JTextField();
		tf12 = new JTextField();
		b11 = new JButton("Start server");
		b11.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				try {
					l13.setText("");
					int udpPort = Integer.parseInt(tf11.getText());
					int tcpPort = Integer.parseInt(tf12.getText());
					card.show(c, "running");
				} catch (Exception e1) {
					l13.setText("Unable to create server");
				}
			}
			
		});
		p1.add(l11);
		p1.add(tf11);
		p1.add(l12);
		p1.add(tf12);
		
		p2 = new JPanel(new GridLayout(10, 1));
		l21 = new JLabel("Server running...");
		l22 = new JLabel("");
		progressBar = new JProgressBar(0, 100);
		b21 = new JButton("Close server");
		b21.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				s.exit();
				s = null;
				card.show(c, "start");
			}
			
		});
		
		p2.add(l21);
		
		p2.add(b21);
		
		c.add(p1, "start");
		c.add(p2, "running");
		
		setSize(400, 400);
	}
}

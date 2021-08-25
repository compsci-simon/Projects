

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

public class test implements ActionListener {
	int count = 0;
	
	public static void main(String[] args) {
		
		JFrame frame = new JFrame();
		
		JButton button = new JButton("Click me");
		JLabel label = new JLabel("Count = 0");
		
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 10, 30));
		panel.setLayout(new GridLayout(1, 0));
		panel.add(button);
		panel.add(label);
		
		frame.add(panel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.setVisible(true);
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		count++;
	}
}

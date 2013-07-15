package ru.olamedia.messenger;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jivesoftware.smack.XMPPConnection;

public class MessengerStatusLine extends JPanel {

	private static final long serialVersionUID = 6248100718535775173L;
	private JLabel label = new JLabel();

	public MessengerStatusLine() {
		setBackground(Color.darkGray);
		label.setForeground(Color.lightGray);
		add(label);
	}

	public void updateStatus(XMPPConnection connection) {
		label.setText((connection.isConnected() ? "Connected" : "Not connected") + " "
				+ (connection.isAnonymous() ? "Anonymous" : "Not anonymous") + " "
				+ (connection.isAuthenticated() ? "Authenticated" : "Not authenticated") + " "
				+ (connection.isSecureConnection() ? "Secure" : "Not secure") + " "
				+ (connection.isUsingTLS() ? "TLS" : "No TLS") + " "
				+ (connection.isUsingCompression() ? "Compressed" : "Not compressed"));
	}
}

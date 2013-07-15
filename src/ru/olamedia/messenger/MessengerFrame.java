package ru.olamedia.messenger;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jivesoftware.smack.packet.Presence;

public class MessengerFrame extends JFrame implements ConnectionStatusListener {
	private static final long serialVersionUID = -7018039145721503457L;
	private final Messenger messenger = new Messenger();
	private final RosterPanel rosterPanel = new RosterPanel();
	private final MessengerStatusLine statusLine = new MessengerStatusLine();
	private JPanel chatPanel = null;
	private LoginPanel loginPanel = new LoginPanel();
	private JComponent centralPanel = null;

	public LoginPanel getLoginPanel() {
		return loginPanel;
	}

	public RosterPanel getRoster() {
		return rosterPanel;
	}

	public MessengerStatusLine getStatusLine() {
		return statusLine;
	}

	public void setCentralPanel(JComponent panel) {
		removeCentralPanel();
		centralPanel = panel;
		getContentPane().add(centralPanel, BorderLayout.CENTER);
		pack();
		repaint();
	}

	public void removeCentralPanel() {
		if (null != centralPanel) {
			getContentPane().remove(centralPanel);
		}
		centralPanel = null;
		pack();
		repaint();
	}

	public MessengerFrame() {
		messenger.addListener(this);
		loginPanel.setMessenger(messenger);
		rosterPanel.setMessenger(messenger);
		chatPanel = rosterPanel.getChatPanelContainer();

		setTitle("Messenger");
		setSize(851, 640);
		setLocationRelativeTo(null);
		setLayout(new BorderLayout());
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setMinimumSize(new Dimension(851, 640));
		JPanel statusPanel = new JPanel();
		// statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
		statusPanel.setPreferredSize(new Dimension(getWidth(), 24));
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
		statusPanel.add(statusLine);

		rosterPanel.setMinimumSize(new Dimension(200, 320));
		rosterPanel.setPreferredSize(new Dimension(200, 640));
		statusLine.setPreferredSize(new Dimension(640, 24));
		//getContentPane().add(statusPanel, BorderLayout.SOUTH);
		getContentPane().add(rosterPanel, BorderLayout.EAST);
		pack();
		setCentralPanel(loginPanel);
	}

	@Override
	public void connectionStatusChanged() {
		synchronized (this) {
			statusLine.updateStatus(messenger.getConnection());
			if (!messenger.isConnected()) {
				setCentralPanel(loginPanel);
			} else {
				messenger.setPresence(Presence.Type.available);
				setCentralPanel(chatPanel);
				messenger.getConnection().getRoster().addRosterListener(rosterPanel);
				rosterPanel.reloadList();
				messenger.getConnection().getChatManager().addChatListener(rosterPanel);
			}
		}
	}
}

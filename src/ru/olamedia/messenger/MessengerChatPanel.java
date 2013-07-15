package ru.olamedia.messenger;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class MessengerChatPanel extends JPanel {
	private static final long serialVersionUID = 5708293877891725821L;
	private JEditorPane htmlArea;
	private JScrollPane scrollPane;
	// private JTextField textInput = new JTextField();
	private JidInfo jidInfo = null;
	private JTextArea textInput = new JTextArea();

	public void setJidInfo(JidInfo jidInfo) {
		this.jidInfo = jidInfo;
		getHtmlArea();
		validate();
		jidInfo.setRead();
	}

	public JEditorPane getHtmlArea() {
		if (null == htmlArea) {
			htmlArea = new JEditorPane();
			scrollPane = new JScrollPane(htmlArea);
			scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			scrollPane.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 0, Color.darkGray));
			htmlArea.setEditable(false);
			htmlArea.setContentType("text/html;charset=utf-8");
			htmlArea.addFocusListener(new FocusListener() {
				@Override
				public void focusLost(FocusEvent arg0) {

				}

				@Override
				public void focusGained(FocusEvent arg0) {
					jidInfo.setRead();
				}
			});
		}
		return htmlArea;
	}

	public void sendMessage() {
		jidInfo.sendMessage(textInput.getText());
	}

	public MessengerChatPanel() {
		getHtmlArea();
		setLayout(new BorderLayout());
		textInput.setPreferredSize(new Dimension(getWidth(), 80));
		textInput.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.darkGray));
		add(textInput, BorderLayout.SOUTH);
		add(scrollPane, BorderLayout.CENTER);
		textInput.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {

			}

			@Override
			public void keyReleased(KeyEvent e) {

			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					sendMessage();
					e.consume();
					textInput.setText("");
				}
			}
		});
	}
}

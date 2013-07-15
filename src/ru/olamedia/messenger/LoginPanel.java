package ru.olamedia.messenger;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class LoginPanel extends JPanel {
	private static final long serialVersionUID = -3130439544178597041L;
	private JLabel lbServer = new JLabel();
	private JTextField serverInput = new JTextField();
	private JLabel lbLogin = new JLabel();
	private JTextField loginInput = new JTextField();
	private JLabel lbPassword = new JLabel();
	private JTextField passwordInput = new JPasswordField();
	private JButton loginButton = new JButton();
	private JButton saveButton = new JButton();
	private Messenger messenger;
	private int port = 5222;
	private Properties properties = new Properties();

	public void setMessenger(Messenger messenger) {
		this.messenger = messenger;
	}

	public LoginPanel() {
		File pf = new File("login.properties");
		if (pf.isFile()) {
			try {
				properties.load(pf.toURI().toURL().openStream());
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (null != properties.getProperty("server")) {
			serverInput.setText(properties.getProperty("server"));
		}
		if (null != properties.getProperty("login")) {
			loginInput.setText(properties.getProperty("login"));
		}
		if (null != properties.getProperty("password")) {
			passwordInput.setText(properties.getProperty("password"));
		}
		setLayout(new GridBagLayout());
		GridBagConstraints cs = new GridBagConstraints();
		cs.fill = GridBagConstraints.HORIZONTAL;
		cs.insets = new Insets(3, 7, 3, 7);
		cs.gridx = 0;
		cs.gridy = 0;
		cs.gridwidth = 1;
		lbServer.setText("Сервер:");
		lbServer.setHorizontalAlignment(JLabel.RIGHT);
		add(lbServer, cs);
		cs.gridx = 1;
		cs.gridy = 0;
		cs.gridwidth = 2;
		serverInput.setPreferredSize(new Dimension(200, 24));
		add(serverInput, cs);
		cs.gridx = 0;
		cs.gridy = 1;
		cs.gridwidth = 1;
		lbLogin.setText("Логин:");
		lbLogin.setHorizontalAlignment(JLabel.RIGHT);
		add(lbLogin, cs);
		cs.gridx = 1;
		cs.gridy = 1;
		cs.gridwidth = 2;
		loginInput.setPreferredSize(new Dimension(200, 24));
		add(loginInput, cs);
		cs.gridx = 0;
		cs.gridy = 2;
		cs.gridwidth = 1;
		lbPassword.setText("Пароль:");
		lbPassword.setHorizontalAlignment(JLabel.RIGHT);
		add(lbPassword, cs);
		cs.gridx = 1;
		cs.gridy = 2;
		cs.gridwidth = 2;
		passwordInput.setPreferredSize(new Dimension(200, 24));
		add(passwordInput, cs);
		// add(passwordInput);
		saveButton.setText("Сохранить настройки");
		loginButton.setText("Войти");
		cs.gridx = 2;
		cs.gridy = 3;
		cs.gridwidth = 1;
		add(saveButton, cs);
		cs.gridx = 1;
		cs.gridy = 3;
		cs.gridwidth = 1;
		add(loginButton, cs);
		loginButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				if (null == messenger) {
				} else {
					messenger.connect(serverInput.getText(), port, loginInput.getText(), passwordInput.getText());
				}
			}
		});
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					properties.setProperty("server", serverInput.getText());
					properties.setProperty("login", loginInput.getText());
					if (null == properties.getProperty("password")) {
						properties.setProperty("password", ""); // can fill it
																// manually
					}
					// properties.setProperty("password",
					// passwordInput.getText());
					properties.store(new FileOutputStream("login.properties"), "login details");
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}
}

package ru.olamedia.messenger;

import java.security.Security;
import java.util.ArrayList;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.provider.VCardProvider;

import ru.olamedia.smack.pgp.PGPExtensionProvider;
import ru.olamedia.smack.pgp.PGPHandshakeExtensionProvider;

public class Messenger implements ConnectionListener, ConnectionCreationListener {
	private static final BouncyCastleProvider provider = new BouncyCastleProvider();

	public static BouncyCastleProvider getProvider() {
		return provider;
	}

	static {
		Security.addProvider(provider);
	}
	private XMPPConnection connection;
	private ArrayList<ConnectionStatusListener> listeners = new ArrayList<ConnectionStatusListener>();
	private Roster roster = null;
	private Presence presence = new Presence(Presence.Type.unavailable);
	private ChatManager chatManager;

	public Messenger() {
		Connection.addConnectionCreationListener(this);
	}

	public void addListener(ConnectionStatusListener listener) {
		listeners.add(listener);
	}

	public ChatManager getChatManager() {
		if (null == chatManager) {
			chatManager = getConnection().getChatManager();
		}
		return chatManager;
	}

	public XMPPConnection getConnection() {
		return connection;
	}

	public boolean isConnected() {
		return null != connection && connection.isConnected();
	}

	public boolean isLoggedIn() {
		return isConnected() && connection.isAuthenticated();
	}

	public Roster getRoster() {
		if (null == roster) {
			roster = getConnection().getRoster();
		}
		return roster;
	}

	public void setPresence(Presence.Type type) {
		presence.setType(type);
		if (connection.isConnected()) {
			connection.sendPacket(presence);
		}
	}

	public void disconnect() {
		setPresence(Presence.Type.unavailable);
		if (connection.isConnected()) {
			connection.disconnect();
		}
	}

	public void connect(String server, int port, String login, String password) {
		if (null != connection) {

		}
		ConnectionConfiguration config = new ConnectionConfiguration(server, port);
		config.setCompressionEnabled(true);
		config.setSecurityMode(SecurityMode.required);
		config.setNotMatchingDomainCheckEnabled(true);
		config.setSASLAuthenticationEnabled(true);
		connection = new XMPPConnection(config);
		ProviderManager.getInstance().addIQProvider("vCard", "vcard-temp", new VCardProvider());
		ProviderManager.getInstance().addExtensionProvider("olapgp", "http://olamedia.ru/jabber/pgp",
				new PGPExtensionProvider());
		ProviderManager.getInstance().addExtensionProvider("olapgphandshake",
				"http://olamedia.ru/jabber/pgp/handshake", new PGPHandshakeExtensionProvider());
		try {
			connection.connect();
			connection.addConnectionListener(this);
			ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(connection);
			discoManager.addFeature("http://olamedia.ru/jabber/pgp");
			discoManager.addFeature("http://olamedia.ru/jabber/pgp/handshake");
			connection.login(login, password, "Olajabber");
		} catch (XMPPException e) {
			e.printStackTrace();
		}
	}

	private void notifyConnectionChanged() {
		for (final ConnectionStatusListener l : listeners) {
			l.connectionStatusChanged();
		}
	}

	@Override
	public void connectionClosed() {
		notifyConnectionChanged();
	}

	@Override
	public void connectionClosedOnError(Exception arg0) {
		notifyConnectionChanged();
	}

	@Override
	public void reconnectingIn(int arg0) {
		notifyConnectionChanged();
	}

	@Override
	public void reconnectionFailed(Exception arg0) {
		notifyConnectionChanged();
	}

	@Override
	public void reconnectionSuccessful() {
		notifyConnectionChanged();
	}

	@Override
	public void connectionCreated(Connection arg0) {
		notifyConnectionChanged();
	}
}

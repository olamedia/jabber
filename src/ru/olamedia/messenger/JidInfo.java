package ru.olamedia.messenger;

import java.io.IOException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.Base64;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.VCard;

import ru.olamedia.pgp.PGPContext;
import ru.olamedia.pgp.PGPKeyUtil;
import ru.olamedia.smack.pgp.PGPExtension;
import ru.olamedia.smack.pgp.PGPHandshakeExtension;

public class JidInfo {
	private String jid;
	private VCard vcard = new VCard();
	private boolean isVcardLoaded = false;
	private boolean isVcardError = false;
	private int unreadMessages = 0;
	private Chat chat = null;
	private Presence presence = new Presence(Presence.Type.unavailable);
	private MessengerChatPanel panel;
	private boolean isPGPEnabled = false;
	private boolean isPGPKeyReceived = false;
	private boolean isPGPKeySent = false;
	private PGPContext pgpContext;
	private Messenger messenger;
	private ArrayList<String> deferredMessages = new ArrayList<String>();
	private ArrayList<JidStatusListener> listeners = new ArrayList<JidStatusListener>();

	private void loadVcard() {
		if (isVcardError) {
			return;
		}
		try {
			vcard.load(messenger.getConnection(), getJidName());
			isVcardLoaded = true;
		} catch (XMPPException e) {
			isVcardError = true;
			e.printStackTrace();
		}
	}

	private boolean isPGPEstablished() {
		return isPGPEnabled && isPGPKeyReceived && isPGPKeySent;
	}

	public void addListener(JidStatusListener listener) {
		listeners.add(listener);
	}

	private void notifyJidChanges() {
		for (JidStatusListener listener : listeners) {
			listener.onJidChanges();
		}
	}

	public boolean isServiceEnabled(String namespace) {
		try {
			DiscoverInfo result = ServiceDiscoveryManager.getInstanceFor(messenger.getConnection()).discoverInfo(jid);
			return result.containsFeature(namespace);
		} catch (XMPPException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void onChatInit() {
		isPGPEnabled = isServiceEnabled("http://olamedia.ru/jabber/pgp/handshake")
				&& isServiceEnabled("http://olamedia.ru/jabber/pgp");
		addServiceLine("Соединение установлено");
		if (isPGPEnabled) {
			addServiceLine("PGP поддерживается, посылаю публичный ключ...");
			sendHandshake();
		} else {
			addServiceLine("PGP не поддерживается");
		}
		notifyJidChanges();
	}

	public void sendHandshake() {
		Message message = new Message(jid);
		message.setFrom(messenger.getConnection().getUser());
		message.setBody("Переключение протоколов");
		PGPHandshakeExtension h = new PGPHandshakeExtension();
		h.addBody("<body>"
				+ Base64.encodeBytes(PGPKeyUtil.dump(getPgpContext().getOwnEncryptionKey(), true).getBytes())
				+ "</body>");
		message.addExtension(h);
		isPGPKeySent = true;
		try {
			getChat().sendMessage(message);
		} catch (XMPPException e) {
			e.printStackTrace();
		}
	}

	public void setMessenger(Messenger messenger) {
		this.messenger = messenger;
	}

	public void setChat(Chat chat) {
		this.chat = chat;
		chat.addMessageListener(messageListener);
		// onChatInit();
	}

	public PGPContext getPgpContext() {
		if (null == pgpContext) {
			pgpContext = new PGPContext();
			try {
				pgpContext.generateKeys(2048);
			} catch (PGPException e) {
				e.printStackTrace();
			}
		}
		return pgpContext;
	}

	public MessengerChatPanel getPanel() {
		if (null == panel) {
			panel = new MessengerChatPanel();
			panel.setJidInfo(this);
		}
		return panel;
	}

	public int getUnreadMessages() {
		return unreadMessages;
	}

	public void setRead() {
		unreadMessages = 0;
	}

	public void sendMessage(String body) {
		getChat();
		try {
			Message message = new Message(jid);
			message.setFrom(messenger.getConnection().getUser());
			if (isPGPKeyReceived) {
				// addServiceLine("Sending encoded text");
				message.setBody("Сообщение");
				PGPExtension p = new PGPExtension();
				p.addBody("<body>" + Base64.encodeBytes(pgpContext.encryptToBytes(body.getBytes(), true, true))
						+ "</body>");
				message.addExtension(p);
				getChat().sendMessage(message);
				addOwnLine(body);
			} else {
				if (isPGPEnabled) {
					// addServiceLine("Waiting for PGP...");
					deferredMessages.add(body);
					return;
				} else {
					addServiceLine("Не будет отправлено: протокол не поддерживается");
				}
				// message.setBody(body);
				// message.setBody("Switch protocol");
				// PGPHandshakeExtension h = new PGPHandshakeExtension();
				// h.addBody("<body>"
				// +
				// Base64.encodeBytes(PGPKeyUtil.dump(getPgpContext().getOwnEncryptionKey(),
				// true).getBytes())
				// + "</body>");
				// message.addExtension(h);
			}
			// System.out.println(message.toXML());

			// addOwnLine(message.toXML());
		} catch (XMPPException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		} catch (PGPException e) {
			e.printStackTrace();
		}
	}

	public void addServiceLine(String body) {
		Document doc = getHtmlArea().getDocument();
		try {
			doc.insertString(doc.getLength(), "[" + getDateString() + "] " + body + "\r\n", null);
		} catch (BadLocationException e1) {
			e1.printStackTrace();
		}
		notifyJidChanges();
	}

	public void addOwnLine(String body) {
		addLine(messenger.getConnection().getUser(), body, false);
	}

	public String twoDigits(int digit) {
		return (String) (digit > 9 ? String.valueOf(digit) : "0" + digit);
	}

	public String getDateString() {
		final Calendar cal = Calendar.getInstance();
		return twoDigits(cal.get(Calendar.HOUR)) + ":" + twoDigits(cal.get(Calendar.MINUTE)) + ":"
				+ twoDigits(cal.get(Calendar.SECOND));
	}

	public void addLine(String jid, String body, boolean unread) {
		if (unread) {
			unreadMessages++;
		}
		Document doc = getHtmlArea().getDocument();
		try {
			doc.insertString(doc.getLength(), "[" + getDateString() + "] <" + jid.split("/")[0] + "> " + body + "\r\n",
					null);
		} catch (BadLocationException e1) {
			e1.printStackTrace();
		}
		notifyJidChanges();
	}

	public JEditorPane getHtmlArea() {
		return getPanel().getHtmlArea();
	}

	private MessageListener messageListener = new MessageListener() {
		public void processMessage(Chat chat, Message message) {
			// addLine(jid, message.getBody(), true);
			if (isPGPEnabled) {
				// addLine(jid, message.toXML(), true);
				PGPExtension ext = (PGPExtension) message.getExtension("http://olamedia.ru/jabber/pgp");
				PGPHandshakeExtension hext = (PGPHandshakeExtension) message
						.getExtension("http://olamedia.ru/jabber/pgp/handshake");
				String pgpBody = "";
				if (null != hext) {
					// System.err.println("pgp/handshake");
					for (Iterator<String> i = hext.getBodies(); i.hasNext();) {
						String encoded = i.next();
						encoded = encoded.substring(6, encoded.length() - 7);
						encoded = PGPContext.bytesToString(Base64.decode(encoded));
						PGPPublicKey encryptionKey = PGPKeyUtil.getEncryptionKey(encoded);
						getPgpContext().setEncryptionKey(encryptionKey);
						isPGPKeyReceived = (null != encryptionKey);
						addServiceLine(isPGPKeyReceived ? "Публичный ключ получен" : "Ошибка: не найден ключ");
						notifyJidChanges();
						if (isPGPEstablished()) {
							if (deferredMessages.size() > 0) {
								Iterator<String> it = deferredMessages.iterator();
								while (it.hasNext()) {
									sendMessage(it.next());
								}
								deferredMessages.clear();
							}
						}
					}
				} else {
				}
				if (isPGPKeySent) {
					if (null != ext) {
						// System.err.println("pgp");
						for (Iterator<String> i = ext.getBodies(); i.hasNext();) {
							try {
								String encoded = i.next();
								encoded = encoded.substring(6, encoded.length() - 7);
								pgpBody = PGPContext.bytesToString(getPgpContext().decryptToBytes(
										Base64.decode(encoded)));
								addLine(jid, pgpBody, true);
							} catch (NoSuchProviderException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							} catch (PGPException e) {
								e.printStackTrace();
							}
						}
					} else {
						// System.err.println("no pgp");
					}
				}
			}
			// sendMessage(message.getBody());
		}

	};

	public Chat getChat() {
		if (null == chat) {
			setChat(messenger.getChatManager().createChat(jid, messageListener));
		}
		return chat;
	}

	public JidInfo(String jid) {
		this.jid = jid;
	}

	public String getJid() {
		return jid;
	}

	public String getJidName() {
		return jid.split("/")[0];
	}

	public Presence getPresence() {
		return presence;
	}

	public void setPresence(Presence presence) {
		this.presence = presence;
	}

	@Override
	public String toString() {
		if (!isVcardLoaded) {
			loadVcard();
		}
		String t = "";
		if (presence.getType() == Presence.Type.available) {
			t += "+";
		} else if (presence.getType() == Presence.Type.unavailable) {
			t += "-";
		} else if (presence.getType() == Presence.Type.error) {
			t += "e";
		} else {
			t += "?";
		}
		t += (isPGPKeyReceived ? "R" : isPGPEnabled ? "W" : "");
		t += (isPGPKeySent ? "S" : isPGPEnabled ? "W" : "");
		String name = "";
		if (isVcardLoaded) {
			if ("" == vcard.getNickName() || null == vcard.getNickName()) {
				if ("" != vcard.getFirstName() && null != vcard.getFirstName()) {
					name = vcard.getFirstName();
				}
				if ("" != vcard.getLastName() && null != vcard.getLastName()) {
					name += " " + vcard.getLastName();
				}
				name = name.trim();
			} else {
				name = vcard.getNickName();
			}
		}
		if ("" == name) {
			name = jid;
		}
		t += " " + name;
		return t;
	}
}

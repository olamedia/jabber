package ru.olamedia.messenger;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.packet.Presence;

public class RosterPanel extends JPanel implements RosterListener, ChatManagerListener {
	private Messenger messenger;
	private final HashMap<String, JidInfo> jids = new HashMap<String, JidInfo>();
	private JidInfo selectedJid = null;

	private static final long serialVersionUID = 6340805491101179033L;
	private JLabel presenceLine = new JLabel();
	private JList list = new JList();
	private Roster roster;
	private JToolBar toolbar = new JToolBar();
	private JButton addContactButton = new JButton();
	private JPanel chatPanelContainer = new JPanel();

	public JPanel getChatPanelContainer() {
		return chatPanelContainer;
	}

	public void setMessenger(Messenger messenger) {
		this.messenger = messenger;

	}

	private JidInfo createJid(String jid) {
		final JidInfo jidInfo = new JidInfo(jid);
		jidInfo.setMessenger(messenger);
		jidInfo.addListener(new JidStatusListener() {
			@Override
			public void onJidChanges() {
				onSomeJidChanges(jidInfo);
			}
		});
		return jidInfo;
	}

	public RosterPanel() {
		list.setSelectionBackground(Color.white);
		list.setSelectionForeground(Color.darkGray);
		list.setBackground(Color.getHSBColor(189f / 360f, 12f / 100f, 23f / 100f));
		list.setForeground(Color.getHSBColor(90f / 360f, 4f / 100f, 84f / 100f));
		list.setFixedCellHeight(24);
		list.setCellRenderer(new ListCellRenderer() {
			protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				JLabel label = (JLabel) (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index,
						isSelected, false);
				JidInfo info = (JidInfo) value;
				// label.setIcon(icon);
				if (info.getUnreadMessages() > 0) {
					label.setForeground(Color.red);
				} else {
				}
				label.setText(info.toString());
				return label;
			}
		});
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				final JidInfo jid = (JidInfo) list.getSelectedValue();
				selectedJid = jid;
				jid.setRead();
				chatPanelContainer.removeAll();
				if (jid.getPresence().getType() != Presence.Type.unavailable) {
					chatPanelContainer.add(jid.getPanel(), BorderLayout.CENTER);
				}
				chatPanelContainer.validate();
				chatPanelContainer.repaint();
			}
		});
		setLayout(new BorderLayout());
		chatPanelContainer.setLayout(new BorderLayout());
		presenceLine.setPreferredSize(new Dimension(200, 24));
		addContactButton.setText("Add contact");
		addContactButton.setPreferredSize(new Dimension(200, 32));
		addContactButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("add contact pressed");
			}
		});
		toolbar.add(addContactButton);
		// add(toolbar, BorderLayout.SOUTH);
		add(list, BorderLayout.CENTER);
		setBackground(Color.white);
	}

	public void reloadList() {
		if (messenger.isLoggedIn()) {
			roster = messenger.getRoster();
			roster.reload();
		}
	}

	@Override
	public void entriesAdded(Collection<String> entries) {
		for (String entry : entries) {
			final JidInfo jid = createJid(entry);
			jids.put(entry, jid);
			list.setListData(jids.values().toArray());
			validate();
		}
	}

	@Override
	public void entriesDeleted(Collection<String> entries) {
		for (String entry : entries) {
			jids.remove(entry);
			list.setListData(jids.values().toArray());
			validate();
		}
	}

	@Override
	public void entriesUpdated(Collection<String> entries) {
		// System.out.println("entriesUpdated: " + entries);
	}

	@Override
	public void presenceChanged(Presence presence) {
		final String jid = presence.getFrom();
		final String jidName = jid.split("/")[0];
		jids.remove(jidName);
		final JidInfo jidInfo = createJid(jid);
		jids.put(jid, jidInfo);
		jidInfo.setPresence(presence);
		list.setListData(jids.values().toArray());
		validate();
		// System.out.println("Presence changed: " + presence.getFrom() + " " +
		// presence);
	}

	@Override
	public void chatCreated(Chat chat, boolean createdLocally) {
		if (jids.containsKey(chat.getParticipant())) {
			jids.get(chat.getParticipant()).setChat(chat);
			jids.get(chat.getParticipant()).onChatInit();
		}
	}

	public void onSomeJidChanges(JidInfo jid) {
		if (null != selectedJid) {
			selectedJid.setRead();
		}
		repaint();
	}
}

package ru.olamedia.smack.pgp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.jivesoftware.smack.packet.PacketExtension;

public class PGPExtension implements PacketExtension {
	private ArrayList<String> bodies = new ArrayList<String>();

	public Iterator<String> getBodies() {
		synchronized (bodies) {
			return Collections.unmodifiableList(new ArrayList<String>(bodies)).iterator();
		}
	}

	public void addBody(String body) {
		synchronized (bodies) {
			bodies.add(body);
		}
	}

	public int getBodiesCount() {
		return bodies.size();
	}

	/**
	 * Returns the XML element name of the extension sub-packet root element.
	 * Always returns "olapgp"
	 * 
	 * @return the XML element name of the packet extension.
	 */
	@Override
	public String getElementName() {
		return "olapgp";
	}

	/**
	 * Returns the XML namespace of the extension sub-packet root element.
	 * According the specification the namespace is always
	 * "http://jabber.org/protocol/xhtml-im"
	 * 
	 * @return the XML namespace of the packet extension.
	 */
	@Override
	public String getNamespace() {
		return "http://olamedia.ru/jabber/pgp";
	}

	/**
	 * Returns the XML representation of a XHTML extension according the
	 * specification. Usually the XML representation will be inside of a Message
	 * XML representation like in the following example:
	 * <message id="MlIpV-4" to="gato1@gato.home" from="gato3@gato.home/Smack">
	 * <subject>Any subject you want</subject>
	 * <body>This message contains something interesting.</body>
	 * <html xmlns="http://jabber.org/protocol/xhtml-im">
	 * <body>
	 * <p style='font-size:large'>
	 * This message contains something <em>interesting</em>.
	 * </p>
	 * </body>
	 * </html>
	 * </message>
	 */
	@Override
	public String toXML() {
		StringBuffer buf = new StringBuffer();
		buf.append("<").append(getElementName()).append(" xmlns=\"").append(getNamespace()).append("\">");
		// Loop through all the bodies and append them to the string buffer
		for (Iterator<String> i = getBodies(); i.hasNext();) {
			buf.append(i.next());
		}
		buf.append("</").append(getElementName()).append(">");
		return buf.toString();
	}

}

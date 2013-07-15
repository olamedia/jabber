package ru.olamedia.smack.pgp;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

public class PGPExtensionProvider implements PacketExtensionProvider {
	public PGPExtensionProvider() {
	}

	@Override
	public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
		PGPExtension ext = new PGPExtension();
		boolean done = false;
		StringBuffer buffer = new StringBuffer();
		;
		while (!done) {
			int eventType = parser.next();
			if (eventType == XmlPullParser.START_TAG) {
				if (parser.getName().equals("body"))
					buffer = new StringBuffer();
				buffer.append(parser.getText());
			} else if (eventType == XmlPullParser.TEXT) {
				if (buffer != null)
					buffer.append(parser.getText());
			} else if (eventType == XmlPullParser.END_TAG) {
				if (parser.getName().equals("body")) {
					buffer.append(parser.getText());
					ext.addBody(buffer.toString());
				} else if (parser.getName().equals(ext.getElementName())) {
					done = true;
				} else
					buffer.append(parser.getText());
			}
		}

		return ext;
	}

}

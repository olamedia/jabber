package ru.olamedia.messenger;

import ru.olamedia.pgp.PGP;

public class Launcher {
	private static PGP pgp;

	public static void main(String[] args) {
		// pgp = new PGP();
		new MessengerFrame().setVisible(true);
	}

	public static PGP getPgp() {
		return pgp;
	}
}

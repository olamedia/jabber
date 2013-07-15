package ru.olamedia.pgp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Iterator;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;

import ru.olamedia.messenger.Messenger;

/**
 * Password-based Encryption (PBE)
 * 
 * @author olamedia
 * 
 */
public class PGPContext {
	private String id = "alice@example.com";
	private String password = "nya";
	private PGPKeyRingGenerator krgen;
	private PGPPublicKeyRing pkr;
	private PGPSecretKeyRing skr;
	private PGPSecretKey masterSecretKey; // decrypt with, private key
	private PGPSecretKey signingKey; // sign with, private key
	private PGPPublicKey encryptionKey = null; // encrypt with, public key
	private PGPPublicKey masterPublicKey; //
	private PGPPublicKey ownEncryptionKey; // public key to distribute
	private int encAlgorithm = PGPEncryptedData.CAST5;

	public PGPPublicKey getEncryptionKey() {
		return encryptionKey;
	}

	public void setEncryptionKey(String asc) {
		encryptionKey = PGPKeyUtil.getEncryptionKey(asc);
	}

	public void setEncryptionKey(PGPPublicKey encryptionKey) {
		this.encryptionKey = encryptionKey;
	}

	public PGPPublicKey getOwnEncryptionKey() {
		return ownEncryptionKey;
	}

	public void generateKeys(int keySize) throws PGPException {
		krgen = PGPKeyUtil.generateKeyRingGenerator(keySize, id, password.toCharArray());
		pkr = krgen.generatePublicKeyRing();
		skr = krgen.generateSecretKeyRing();
		masterSecretKey = skr.getSecretKey();
		@SuppressWarnings("unchecked")
		final Iterator<PGPSecretKey> skit = skr.getSecretKeys();
		while (skit.hasNext()) {
			final PGPSecretKey k = skit.next();
			if (k.isPrivateKeyEmpty()) {
				continue;
			}
			if (k.isMasterKey()) {
				masterSecretKey = k;
			}
			if (k.isSigningKey()) {
				signingKey = k;
			}
		}
		@SuppressWarnings("unchecked")
		final Iterator<PGPPublicKey> pkit = pkr.getPublicKeys();
		while (pkit.hasNext()) {
			final PGPPublicKey k = pkit.next();
			if (k.isRevoked()) {
				continue;
			}
			if (k.isMasterKey()) {
				masterPublicKey = k;
			}
			if (k.isEncryptionKey()) {
				ownEncryptionKey = k;
			}
		}
	}

	public static String bytesToString(byte[] data) {
		return String.valueOf(ByteUtils.toCharArray(data));
	}

	public byte[] encryptToBytes(String data, boolean armored, boolean withIntegrityCheck) throws IOException,
			PGPException, NoSuchProviderException {
		return encryptToBytes(data.getBytes(), armored, withIntegrityCheck);
	}

	public byte[] encryptToBytes(byte[] data, boolean armored, boolean withIntegrityCheck) throws IOException,
			PGPException, NoSuchProviderException {
		ByteArrayOutputStream bencOut = new ByteArrayOutputStream();
		OutputStream out = (OutputStream) bencOut;
		if (armored) {
			out = new ArmoredOutputStream(out);
		}
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		PGPCompressedDataGenerator compressedDataGenerator = new PGPCompressedDataGenerator(
				PGPCompressedDataGenerator.ZIP);
		OutputStream compressedOutputStream = compressedDataGenerator.open(bOut); // open
		PGPLiteralDataGenerator lData = new PGPLiteralDataGenerator();
		// we want to generate compressed data. This might be a user option
		// later, in which case we would pass in bOut.
		String name = "x";
		OutputStream pOut = lData.open(compressedOutputStream,//
				PGPLiteralData.BINARY, name, // "filename" to store
				data.length, // length of clear data
				new Date() // current time
				);
		pOut.write(data);
		lData.close();
		compressedDataGenerator.close();
		byte[] bytes = bOut.toByteArray(); // compressed data

		JcePGPDataEncryptorBuilder encBuilder = new JcePGPDataEncryptorBuilder(encAlgorithm)//
				.setWithIntegrityPacket(withIntegrityCheck)//
				.setSecureRandom(new SecureRandom())//
				.setProvider(PGP.getProvider())//
		;
		PGPEncryptedDataGenerator encGen = new PGPEncryptedDataGenerator(encBuilder);
		encGen.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(getEncryptionKey()).setProvider(PGP.getProvider()));
		OutputStream cOut = encGen.open(out, bytes.length);// new byte[1 <<
															// 16]);//
		cOut.write(bytes);
		cOut.close();
		out.close();
		return bencOut.toByteArray();
	}

	public byte[] decryptToBytes(byte[] data) throws NoSuchProviderException, IOException, PGPException {
		InputStream in = null;
		InputStream din = null;
		try {
			in = new ByteArrayInputStream(data);
			din = PGPUtil.getDecoderStream(in);

			PGPObjectFactory objectFactory = new PGPObjectFactory(din);
			PGPEncryptedDataList enc;
			Object o = objectFactory.nextObject();
			//
			// the first object might be a PGP marker packet.
			//
			if (o instanceof PGPEncryptedDataList) {
				enc = (PGPEncryptedDataList) o;
			} else {
				enc = (PGPEncryptedDataList) objectFactory.nextObject();
			}
			//
			// find the secret key
			//
			@SuppressWarnings("rawtypes")
			Iterator it = enc.getEncryptedDataObjects();
			PGPPrivateKey sessionKey = null;
			PGPPublicKeyEncryptedData encryptedData = null;
			while (sessionKey == null && it.hasNext()) {
				encryptedData = (PGPPublicKeyEncryptedData) it.next();
				sessionKey = findSecretKey(encryptedData.getKeyID());
			}
			if (sessionKey == null) {
				throw new IllegalArgumentException("secret key for message not found.");
			}
			PublicKeyDataDecryptorFactory decryptorFactory = new JcePublicKeyDataDecryptorFactoryBuilder().setProvider(
					PGP.getProvider()).build(sessionKey);
			InputStream clear = encryptedData.getDataStream(decryptorFactory);
			// InputStream clear =
			// encryptedData.getDataStream(decryptorFactory);

			PGPObjectFactory plainFact = new PGPObjectFactory(clear);
			Object message = plainFact.nextObject();
			// decompress if required:
			if (message instanceof PGPCompressedData) {
				PGPCompressedData cData = (PGPCompressedData) message;
				PGPObjectFactory pgpFact = new PGPObjectFactory(cData.getDataStream());
				message = pgpFact.nextObject();
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			if (message instanceof PGPLiteralData) {
				PGPLiteralData ld = (PGPLiteralData) message;
				InputStream unc = ld.getInputStream();
				int ch;
				while ((ch = unc.read()) > 0) {
					baos.write(ch);
				}
				if (encryptedData.isIntegrityProtected()) {
					if (!encryptedData.verify()) {
						System.err.println("message failed integrity check");
					} else {
						// System.err.println("message integrity check passed");
					}
				} else {
					// System.err.println("no message integrity check");
				}
				return baos.toByteArray();
			} else if (message instanceof PGPOnePassSignatureList) {
				throw new PGPException("encrypted message contains a signed message - not literal data.");
			} else {
				throw new PGPException("message is not a simple encrypted file - type unknown.");
			}
		} finally {
			in.close();
			din.close();
		}
	}

	public byte[] decryptToBytes(String s) throws NoSuchProviderException, IOException, PGPException {
		return decryptToBytes(s.getBytes());
	}

	private PGPPrivateKey findSecretKey(long keyId) throws PGPException {
		final PGPSecretKey key = skr.getSecretKey(keyId);
		if (key == null) {
			return null;
		}
		return key.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider(Messenger.getProvider()).build(
				password.toCharArray()));
	}

}

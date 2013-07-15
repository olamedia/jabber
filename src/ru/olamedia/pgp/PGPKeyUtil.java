package ru.olamedia.pgp;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Date;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.bcpg.sig.Features;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PBESecretKeyEncryptor;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPGPKeyPair;

public class PGPKeyUtil {
	/**
	 * Export Public Key into readable string
	 * 
	 * @param key
	 *            PGPPublicKey
	 * @return String
	 * @throws IOException
	 * @throws PGPException
	 */
	public static String publicKeyToString(PGPPublicKey key) throws IOException, PGPException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ArmoredOutputStream armoredOut = new ArmoredOutputStream(os);
		key.encode(armoredOut);
		armoredOut.close();
		return os.toString();
	}

	public static void dump(PGPPublicKeyRing pkr, String filename) {
		try {
			BufferedOutputStream pubout = new BufferedOutputStream(new FileOutputStream(filename));
			pkr.encode(pubout);
			pubout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void dump(PGPSecretKeyRing skr, String filename) {
		try {
			BufferedOutputStream secout = new BufferedOutputStream(new FileOutputStream(filename));
			skr.encode(secout);
			secout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void dump(PGPSecretKey masterKey, String filename, boolean armored) {
		try {
			OutputStream secout = new BufferedOutputStream(new FileOutputStream(filename));
			if (armored) {
				secout = new ArmoredOutputStream(secout);
			}
			masterKey.encode(secout);
			secout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void dump(PGPPublicKey masterKey, String filename, boolean armored) {
		try {
			OutputStream secout = new BufferedOutputStream(new FileOutputStream(filename));
			if (armored) {
				secout = new ArmoredOutputStream(secout);
			}
			masterKey.encode(secout);
			secout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("resource")
	public static String dump(PGPPublicKey masterKey, boolean armored) {
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			OutputStream secout = new BufferedOutputStream(bout);
			if (armored) {
				secout = new ArmoredOutputStream(secout);
			}
			masterKey.encode(secout);
			secout.close();
			String s = PGPContext.bytesToString(bout.toByteArray());
			bout.close();
			return s;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public final static PGPKeyRingGenerator generateKeyRingGenerator(int keySize, String id, char[] pass)
			throws PGPException {
		return generateKeyRingGenerator(keySize, id, pass, 0xc0);
	}

	// Note: s2kcount is a number between 0 and 0xff that controls the
	// number of times to iterate the password hash before use. More
	// iterations are useful against offline attacks, as it takes more
	// time to check each password. The actual number of iterations is
	// rather complex, and also depends on the hash function in use.
	// Refer to Section 3.7.1.3 in rfc4880.txt. Bigger numbers give
	// you more iterations. As a rough rule of thumb, when using
	// SHA256 as the hashing function, 0x10 gives you about 64
	// iterations, 0x20 about 128, 0x30 about 256 and so on till 0xf0,
	// or about 1 million iterations. The maximum you can go to is
	// 0xff, or about 2 million iterations. I'll use 0xc0 as a
	// default -- about 130,000 iterations.

	public final static PGPKeyRingGenerator generateKeyRingGenerator(int keySize, String id, char[] pass, int s2kcount)
			throws PGPException {
		// This object generates individual key-pairs.
		RSAKeyPairGenerator kpg = new RSAKeyPairGenerator();

		// Boilerplate RSA parameters, no need to change anything
		// except for the RSA key-size (2048). You can use whatever
		// key-size makes sense for you -- 4096, etc.
		kpg.init(new RSAKeyGenerationParameters(BigInteger.valueOf(0x10001), new SecureRandom(), 2048, 12));

		// First create the master (signing) key with the generator.
		PGPKeyPair rsakp_sign = new BcPGPKeyPair(PGPPublicKey.RSA_SIGN, kpg.generateKeyPair(), new Date());
		// Then an encryption subkey.
		PGPKeyPair rsakp_enc = new BcPGPKeyPair(PGPPublicKey.RSA_ENCRYPT, kpg.generateKeyPair(), new Date());

		// Add a self-signature on the id
		PGPSignatureSubpacketGenerator signhashgen = new PGPSignatureSubpacketGenerator();

		// Add signed metadata on the signature.
		// 1) Declare its purpose
		signhashgen.setKeyFlags(false, KeyFlags.SIGN_DATA | KeyFlags.CERTIFY_OTHER);
		// 2) Set preferences for secondary crypto algorithms to use
		// when sending messages to this key.
		signhashgen.setPreferredSymmetricAlgorithms(false, new int[] { SymmetricKeyAlgorithmTags.AES_256,
				SymmetricKeyAlgorithmTags.AES_192, SymmetricKeyAlgorithmTags.AES_128 });
		signhashgen.setPreferredHashAlgorithms(false, new int[] { HashAlgorithmTags.SHA256, HashAlgorithmTags.SHA1,
				HashAlgorithmTags.SHA384, HashAlgorithmTags.SHA512, HashAlgorithmTags.SHA224, });
		// 3) Request senders add additional checksums to the
		// message (useful when verifying unsigned messages.)
		signhashgen.setFeature(false, Features.FEATURE_MODIFICATION_DETECTION);

		// Create a signature on the encryption subkey.
		PGPSignatureSubpacketGenerator enchashgen = new PGPSignatureSubpacketGenerator();
		// Add metadata to declare its purpose
		enchashgen.setKeyFlags(false, KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE);

		// Objects used to encrypt the secret key.
		PGPDigestCalculator sha1Calc = new BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA1);
		PGPDigestCalculator sha256Calc = new BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA256);

		// bcpg 1.48 exposes this API that includes s2kcount. Earlier
		// versions use a default of 0x60.
		PBESecretKeyEncryptor pske = (new BcPBESecretKeyEncryptorBuilder(PGPEncryptedData.AES_256, sha256Calc, s2kcount))
				.build(pass);

		// Finally, create the keyring itself. The constructor
		// takes parameters that allow it to generate the self
		// signature.
		PGPKeyRingGenerator keyRingGen = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION, rsakp_sign, id,
				sha1Calc, signhashgen.generate(), null, new BcPGPContentSignerBuilder(rsakp_sign.getPublicKey()
						.getAlgorithm(), HashAlgorithmTags.SHA1), pske);

		// Add our encryption subkey, together with its signature.
		keyRingGen.addSubKey(rsakp_enc, enchashgen.generate(), null);
		return keyRingGen;
	}

	@SuppressWarnings("resource")
	public static PGPPublicKey getEncryptionKey(String asc) {
		InputStream in = new ByteArrayInputStream(asc.getBytes());
		PGPPublicKey key = null;
		try {
			in = PGPUtil.getDecoderStream(in);
			PGPObjectFactory of = new PGPObjectFactory(in);
			Object o = of.nextObject();
			if (o instanceof PGPPublicKey) {
				return (PGPPublicKey) o;
			}
			System.err.println(o.getClass().getName());
			return null;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return key;
	}

}

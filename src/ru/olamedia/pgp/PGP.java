package ru.olamedia.pgp;

import java.io.IOException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.text.Collator;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair;

public class PGP {
	private static final BouncyCastleProvider provider = new BouncyCastleProvider();

	public static BouncyCastleProvider getProvider() {
		return provider;
	}

	static {
		Security.addProvider(provider);
	}

	public void ver() {
		System.out.println();

		Provider jsafeJce = provider;

		System.out.println("JCE Provider Name: " + jsafeJce.getName());
		System.out.println("Product Version: " + jsafeJce.getVersion());
		System.out.println("Product Info: \n" + jsafeJce.getInfo());
		System.out.println();
		System.out.println("Supported Algorithms: \n");
		Collator collator = Collator.getInstance();
		Set<String> treeSet = new TreeSet<String>(collator);
		for (Enumeration<?> e = jsafeJce.propertyNames(); e.hasMoreElements();) {
			treeSet.add((String) e.nextElement());
		}
		for (String name : treeSet) {
			System.out.println("\t" + name + " = " + jsafeJce.getProperty(name));
		}
		System.out.println();
	}

	// private KeyPair ownKeyPair;
	public JcaPGPKeyPair ownKeyPair = null; // session keys
	private PGPContext context = new PGPContext();

	public String chunked(String s, int size) {
		return Arrays.deepToString(s.split("(?<=\\G.{" + size + "})"));
	}

	public PGP() {
		System.out.println("PGP START");
		// ver();
		try {
			context.generateKeys(2048);
			byte[] encoded = context.encryptToBytes("Hello, world!", true, true);
			String s = PGPContext.bytesToString(encoded);
			System.out.println(s);
			byte[] decoded = context.decryptToBytes(encoded);
			s = PGPContext.bytesToString(decoded);
			System.out.println(s);
		} catch (PGPException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// PGPKeyUtil.dump(pkr, "public.keyring");
		// PGPKeyUtil.dump(skr, "secret.keyring");
		// PGPKeyUtil.dump(masterKey, "secret.key", false);
		// PGPKeyUtil.dump(masterKey, "secret.asc", true);
		// PGPKeyUtil.dump(publicKey, "public.key", false);
		// PGPKeyUtil.dump(publicKey, "public.asc", true);
		catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

package yak.etc;

import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

public class DH extends Yak {
	public BigInteger big;
	
	public DH(String hex) {
		this(new BigInteger(hex, 16));
	}
	
	public DH(BigInteger big) {
		this.big = big;
		DataInputStream urandom = null;
		try {
			urandom = new JavaFileIO().openURandom();
			byte[] seed = new byte[256];
			urandom.read(seed);
			Rand.setSeed(seed);
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("DANGER: Cannot seed SecureRandom with /dev/urandom: " + e);
		}
	}

	static SecureRandom Rand = new SecureRandom();

	public static final int NumRandomBitsPerDHKey = 2047;
//	public static final String Rfc3526Modulus1536Bits = ""
//			+ "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"
//			+ "29024E088A67CC74020BBEA63B139B22514A08798E3404DD"
//			+ "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245"
//			+ "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED"
//			+ "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D"
//			+ "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F"
//			+ "83655D23DCA3AD961C62F356208552BB9ED529077096966D"
//			+ "670C354E4ABC9804F1746C08CA237327FFFFFFFFFFFFFFFF";
	
	public static final String Rfc3526Modulus2048Bits  = ""
			+ "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"
            + "29024E088A67CC74020BBEA63B139B22514A08798E3404DD"
            + "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245"
            + "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED"
            + "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D"
            + "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F"
            + "83655D23DCA3AD961C62F356208552BB9ED529077096966D"
            + "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B"
            + "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9"
            + "DE2BCBF6955817183995497CEA956AE515D2261898FA0510"
            + "15728E5A8AACAA68FFFFFFFFFFFFFFFF";

	/** Generator */
	public static BigInteger G = new BigInteger("2");
	/** Modulus */
	public static BigInteger M = new BigInteger(
			DH.Rfc3526Modulus2048Bits, 16);

	public static DH RandomKey() {
		return new DH(new BigInteger(NumRandomBitsPerDHKey, Rand));
	}

	public DH mutualKey(DH pubB) {
		return new DH(pubB.big.modPow(this.big, M));
	}

	public DH publicKey() {
		return new DH(G.modPow(this.big, M));
	}

	public static int randomInt(int max) {
		return Rand.nextInt(max);
	}
	
	@Override
	public String toString() {
		return big.toString(16);
	}
}

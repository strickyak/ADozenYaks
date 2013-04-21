package yak.etc;

import java.math.BigInteger;
import java.security.SecureRandom;

public class DH extends Yak {
	public BigInteger big;
	
	public DH(BigInteger big) {
		this.big = big;
	}

	static SecureRandom Rand = new SecureRandom();

	public static final int NumRandomBitsPerDHKey = 1535;
	public static final String Rfc3526Modulus1536Bits = ""
			+ "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"
			+ "29024E088A67CC74020BBEA63B139B22514A08798E3404DD"
			+ "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245"
			+ "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED"
			+ "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D"
			+ "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F"
			+ "83655D23DCA3AD961C62F356208552BB9ED529077096966D"
			+ "670C354E4ABC9804F1746C08CA237327FFFFFFFFFFFFFFFF";

	/** Generator */
	public static BigInteger G = new BigInteger("2");
	/** Modulus */
	public static BigInteger M = new BigInteger(
			DH.Rfc3526Modulus1536Bits, 16);

	public static DH RandomKey() {
		return new DH(new BigInteger(NumRandomBitsPerDHKey, Rand));
	}

	public DH mutualKey(DH pubB) {
		return new DH(pubB.big.modPow(this.big, M));
	}

	public DH publicKey() {
		return new DH(G.modPow(this.big, M));
	}
	
	@Override
	public String toString() {
		return big.toString(16);
	}
}

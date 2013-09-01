package yak.etc;

import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Hash extends Yak {
	public static final int HASH_LEN = 20; 
	public static final int IV_LEN = 16; 
	public static final int KEY_LEN = 16;

	public byte[] bytes;  // Constructor always set bytes with the hash.
	private String str;   // Lazily make Hex String if needed.
	SecretKeySpec keySpec;    // Lazily make AES KeySpec if needed.

	public Hash(String a) {
		this(StringToBytes(a));
	}

	public Hash(byte[] a) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.update(a);
			bytes = md.digest();
		} catch (Exception e) {
			e.printStackTrace();
			throw Bad("yak.etc.Hash: %s", e);
		}
	}

	public Hash(Bytes a) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.update(a.arr, a.off, a.len);
			bytes = md.digest();
		} catch (Exception e) {
			e.printStackTrace();
			throw Bad("yak.etc.Hash: %s", e);
		}
	}

	@Override
	public String toString() {
		if (str == null) {
			str = HexEncode(bytes);
		}
		return str;
	}

	public String oldEncrypt(String a, long iv) {
		try {
			if (keySpec == null) {
				keySpec = new SecretKeySpec(bytes, 0, KEY_LEN, "AES");
			}
			
			IvParameterSpec ivSpec = new IvParameterSpec(LongToBlock16(iv));
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
			return HexEncode(cipher.doFinal(StringToBytes(a)));
		} catch (Exception e) {
			e.printStackTrace();
			throw Bad("initCipher: %s", e);
		}
	}

	public String oldDecrypt(String a, long iv) {
		try {
			if (keySpec == null) {
				keySpec = new SecretKeySpec(bytes, 0, KEY_LEN, "AES");
			}
			IvParameterSpec ivSpec = new IvParameterSpec(LongToBlock16(iv));
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
			return BytesToString(cipher.doFinal(HexDecode(a)));
		} catch (Exception e) {
			e.printStackTrace();
			throw Bad("initCipher: %s", e);
		}
	}
	
	public static byte[] makeRandomBytes(int len) {
		byte[] b = new byte[len];
		DH.Rand.nextBytes(b);
		return b;
	}
	
	public Bytes encryptBytes(Bytes a) {
		try {
			if (keySpec == null) {
				keySpec = new SecretKeySpec(bytes, 0, KEY_LEN, "AES");
			}
			
			Hash check = new Hash(a);
			
			byte[] iv = makeRandomBytes(IV_LEN);
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
			cipher.update(check.bytes);    // First encrypt the hash.
			byte[] x = cipher.doFinal(a.arr, a.off, a.len);  // Then the data.
			
			Bytes z = new Bytes(iv);  // Result begins with iv
			z.appendBytes(x);         // followed by encrypted hash & data.
			return z;
		} catch (Exception e) {
			e.printStackTrace();
			throw Bad("encrypt: %s", e);
		}
	}
	public Bytes decryptBytes(Bytes a) {
		Bytes plain;
		try {
			if (keySpec == null) {
				keySpec = new SecretKeySpec(bytes, 0, KEY_LEN, "AES");
			}
			byte[] iv = a.popByteArray(IV_LEN);
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
			plain = new Bytes(cipher.doFinal(a.arr, a.off, a.len));
		} catch (Exception e) {
			e.printStackTrace();
			throw Bad("initCipher: %s", e);
		}

		byte[] check = plain.popByteArray(HASH_LEN);
		Hash check2 = new Hash(check);
		if (! Bytes.equalsBytes(check, check2.bytes)) {
			throw Bad("Checksum fails in decrypted block");
		}
		return plain;
	}

}

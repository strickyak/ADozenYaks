package yak.etc;

import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Hash extends Yak {

	public String str;
	public byte[] bytes;
	SecretKeySpec key;

	public Hash(String a) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] tmp = StringToBytes(a);
			md.update(tmp);
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

	public String Encrypt(String a, long iv) {
		try {
			if (key == null) {
				key = new SecretKeySpec(bytes, 0, 16, "AES");
			}
			
			IvParameterSpec ivSpec = new IvParameterSpec(LongToBlock16(iv));
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
			return HexEncode(cipher.doFinal(StringToBytes(a)));
		} catch (Exception e) {
			e.printStackTrace();
			throw Bad("initCipher: %s", e);
		}
	}

	public String Decrypt(String a, long iv) {
		try {
			if (key == null) {
				key = new SecretKeySpec(bytes, 0, 16, "AES");
			}
			IvParameterSpec ivSpec = new IvParameterSpec(LongToBlock16(iv));
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
			return BytesToString(cipher.doFinal(HexDecode(a)));
		} catch (Exception e) {
			e.printStackTrace();
			throw Bad("initCipher: %s", e);
		}
	}
}

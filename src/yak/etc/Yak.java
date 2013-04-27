package yak.etc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.regex.Pattern;

public abstract class Yak {

	public static byte[] LongToBlock16(long a) {
		byte[] z = new byte[16];
		z[0] = (byte) a;
		z[1] = (byte) (a >>> 8);
		z[2] = (byte) (a >>> 16);
		z[3] = (byte) (a >>> 24);
		z[4] = (byte) (a >>> 32);
		z[5] = (byte) (a >>> 40);
		z[6] = (byte) (a >>> 48);
		z[7] = (byte) (a >>> 56);
		return z;
	}

	public static int ValueOfHexChar(char c) {
		if ('0' <= c && c <= '9') {
			return c - '0';
		}
		if ('a' <= c && c <= 'f') {
			return c - 'a' + 10;
		}
		if ('A' <= c && c <= 'F') {
			return c - 'A' + 10;
		}
		throw new IllegalArgumentException();
	}

	/** Bytes of String, allowing only 8-bit (Latin1) chars */
	public static byte[] StringToBytes(String a) {
		final int n = a.length();
		byte[] bytes = new byte[n];
		for (int i = 0; i < n; i++) {
			char c = a.charAt(i);
			byte b = (byte) c;
			if (b != c) {
				throw Bad("Bad char in ToBytes(): %d", c);
			}
			bytes[i] = b;
		}
		return bytes;
	}

	public static void sleepSecs(double secs) {
		try {
			Thread.sleep((long) (secs * 1000) /* ms */);
		} catch (InterruptedException e) {
			// pass.
		}
	}

	public static String BytesToString(byte[] bytes) {
		final int n = bytes.length;
		char[] chars = new char[n];
		for (int i = 0; i < n; i++) {
			chars[i] = (char) bytes[i];
		}
		return String.valueOf(chars);
	}

	public static String CharsToString(char[] chars) {
		return String.valueOf(chars);
	}

	public static String UrlDecode(String s) {
		StringBuffer sb = new StringBuffer();
		final int n = s.length();
		for (int i = 0; i < n; i++) {
			char c = s.charAt(i);
			switch (c) {
			case '+':
				sb.append(" ");
				break;
			case '%':
				if (i + 2 < n) {
					char c1 = s.charAt(i + 1);
					char c2 = s.charAt(i + 2);
					int x = ValueOfHexChar(c1) * 16 + ValueOfHexChar(c2);
					sb.append((char) x);
					i += 2;
				} else {
					throw new IllegalArgumentException(s);
				}
				break;
			default:
				sb.append(c);
				break;
			}
		}
		return sb.toString();
	}

	public static String CurlyEncode(String s) {
		final int n = s.length();
		if (n == 0) {
			return "{}"; // Special Case.
		}
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < n; i++) {
			char c = s.charAt(i);
			if ('"' < c && c < '{') {
				sb.append(c);
			} else {
				sb.append("{" + (int) c + "}"); // {%d}
			}
		}
		return sb.toString();
	}

	public static char[] HexChars = { '0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9', 'a', 'b', 'c', 'd', 'e', 'f', };

	public static String HexEncode(byte[] bytes) {
		final int n = bytes.length;
		char[] chars = new char[2 * n];
		for (int i = 0; i < n; i++) {
			final byte b = bytes[i];
			chars[2 * i] = HexChars[(b >> 4) & 15];
			chars[2 * i + 1] = HexChars[b & 15];
		}
		return CharsToString(chars);
	}

	public static byte[] HexDecode(String a) {
		final int n = a.length() / 2;
		byte[] z = new byte[n];
		for (int i = 0; i < n; i++) {
			int p = ValueOfHexChar(a.charAt(2 * i));
			int q = ValueOfHexChar(a.charAt(2 * i + 1));
			z[i] = (byte) ((p << 4) | q);
		}
		return z;
	}

	public static String Show(HashMap<String, String> map) {
		StringBuffer sb = new StringBuffer();
		sb.append("{map ");
		for (String k : map.keySet()) {
			sb.append("[ " + CurlyEncode(k) + " ]= " + CurlyEncode(map.get(k))
					+ " ");
		}
		sb.append("}");
		return sb.toString();
	}

	public static String Show(String[] ss) {
		StringBuffer sb = new StringBuffer();
		sb.append("{arr ");
		for (int i = 0; i < ss.length; i++) {
			sb.append("[" + i + "]= " + CurlyEncode(ss[i]) + " ");
		}
		sb.append("}");
		return sb.toString();
	}

	public static String Show(byte[] bb) {
		StringBuffer sb = new StringBuffer();
		sb.append(fmt("{bytes*%d ", bb.length));
		for (int i = 0; i < bb.length; i++) {
			sb.append(fmt("%d ", (int) bb[i]));
		}
		sb.append("}");
		return sb.toString();
	}

	public static String Join(String[] a, String delim) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < a.length; i++) {
			if (i > 0) {
				sb.append(delim);
			}
			sb.append(a[i]);
		}
		return sb.toString();
	}

	public static String Hash(String s) {
		char x = 0;
		final int n = s.length();
		for (int i = 0; i < n; i++) {
			x += s.charAt(i);
		}
		return Integer.toString((int) x);
	}

	public static String ReadWholeFile(File f) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(
				new FileInputStream(f)));
		StringBuffer sb = new StringBuffer();
		while (true) {
			String s = r.readLine();
			if (s==null) break;
			sb.append(s);
			sb.append('\n');
		}
		r.close();
		return sb.toString();
	}

	public static void WriteWholeFile(File f, String value) throws IOException {
		BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(f)));
		w.write(value);
		w.close();
	}

	public static String ReadAll(InputStream in) throws IOException {
		// TODO Auto-generated method stub
		StringBuffer sb = new StringBuffer();
		while (true) {
			int x = in.read();
			if (x < 0) {
				break;
			}
			sb.append((char) x);
		}
		return sb.toString();
	}

	public static String ReadUrl(String url) throws IOException {
		return ReadUrl(new URL(url));
	}

	public static String ReadUrl(URL url) throws IOException {
		URLConnection conn = url.openConnection();
		conn.connect();
		InputStream in = (InputStream) conn.getContent();
		System.err.println("RESULT=" + in);

		String s = ReadAll(in);
		return s;
	}

	public static String htmlEscape(String s) {
		return htmlEscape(s, false);
	}

	public static String htmlEscape(String s, boolean lineBreaks) {
		StringBuffer sb = new StringBuffer();
		final int n = s.length();
		for (int i = 0; i < n; i++) {
			char c = s.charAt(i);
			if (32 <= c && c <= 126 || c == '\n') {
				switch (c) {
				case '<':
					sb.append("&lt;");
				case '>':
					sb.append("&gt;");
				case '&':
					sb.append("&amp;");
				case '"':
					sb.append("&quot;");
				case '\n':
					sb.append(lineBreaks ? "<br>" : "&#10;");
				default:
					sb.append(c);
				}
			} else {
				sb.append("&#" + (int) c + ";");
			}
		}
		return sb.toString();
	}

	public static String fmt(String s, Object... objects) {
		return String.format(s, objects);
	}

	/** matches what is valid in Html TAG */
	public static Pattern htmlTagP = Pattern
			.compile("[A-Za-z][-A-Za-z0-9_.:]*");

	public static class Ht { // For XSS Safety.
		private StringBuffer sb;

		public Ht() {
			this.sb = new StringBuffer();
		}

		public Ht(Ht that) {
			this.sb = new StringBuffer(that.sb.toString());
		}

		public Ht(String s) {
			this.sb = new StringBuffer(htmlEscape(s));
		}

		public String toString() {
			return sb.toString();
		}

		public Ht append(String s) {
			sb.append(htmlEscape(s));
			return this;
		}

		public Ht append(Ht that) {
			sb.append(that.toString());
			return this;
		}

		static public Ht entity(String name) {
			Ht ht = new Ht();
			ht.sb.append(fmt("&%s;", name));
			return ht;
		}

		static public Ht tag(Ht appendMe, String type, String[] args,
				String body) {
			return tag(appendMe, type, args, new Ht(body));
		}

		static public Ht tag(Ht appendMe, String type, String[] args, Ht body) {
			Ht z = appendMe == null ? new Ht() : appendMe;
			assert htmlTagP.matcher(type).matches();
			z.sb.append(fmt("<%s ", type));
			if (args != null) {
				for (int i = 0; i < args.length; i += 2) {
					assert htmlTagP.matcher(args[i]).matches();
					z.sb.append(fmt("%s=\"%s\" ", args[i],
							htmlEscape(args[i + 1])));
				}
			}
			z.sb.append(fmt(">%s</%s>", body, type));
			return z;
		}

		static public Ht tag(Ht appendMe, String type, String[] args) {
			Ht z = appendMe == null ? new Ht() : appendMe;
			assert htmlTagP.matcher(type).matches();
			z.sb.append(fmt("<%s ", type));
			if (args != null) {
				for (int i = 0; i < args.length; i += 2) {
					assert htmlTagP.matcher(args[i]).matches();
					z.sb.append(fmt("%s=\"%s\" ", args[i],
							htmlEscape(args[i + 1])));
				}
			}
			z.sb.append(" />");
			return z;
		}
	}

	public static boolean isAlphaNum(String s) {
		final int n = s.length();
		for (int i = 0; i < n; i++) {
			char c = s.charAt(i);
			if ('0' <= c && c <= '9')
				continue;
			if ('a' <= c && c <= 'z')
				continue;
			if ('A' <= c && c <= 'Z')
				continue;
			if (c == '_')
				continue;
			return false;
		}
		return true;
	}

	public static RuntimeException Bad(String msg, Object... args) {
		return new RuntimeException("BAD { " + fmt(msg, args) + " }");
	}

	public static void Say(String msg, Object... args) {
		System.err.println(fmt("## " + msg, args));
	}
}

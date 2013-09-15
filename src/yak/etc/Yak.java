package yak.etc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.regex.Pattern;

import android.content.Context;

public abstract class Yak {

	public static String[] strings(String ...args) {
		return args;
	}
	
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

	public static int ValueOfHexCharIgnoringJunk(char c) {
		if ('0' <= c && c <= '9') {
			return c - '0';
		}
		if ('a' <= c && c <= 'f') {
			return c - 'a' + 10;
		}
		if ('A' <= c && c <= 'F') {
			return c - 'A' + 10;
		}
		return -1;
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
			chars[i] = (char) (255&bytes[i]);
		}
		return String.valueOf(chars);
	}

	public static String CharsToString(char[] chars) {
		return String.valueOf(chars);
	}
	
	public static String UrlEncode(String s) {
		try {
			String z = URLEncoder.encode(s, "utf-8");
			Say("UrlEncode: {%s} -> {%s}", s, z);
			return z;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			throw Bad("UrlEncode(): " + e);
		}
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

	public static String CurlyEncode(byte[] b) {
		return CurlyEncode(BytesToString(b));
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

	public static String HexEncode(byte[] array) {
		final int n = array.length;
		char[] chars = new char[2 * n];
		for (int i = 0; i < n; i++) {
			final byte b = array[i];
			chars[2 * i] = HexChars[(b >> 4) & 15];
			chars[2 * i + 1] = HexChars[b & 15];
		}
		return CharsToString(chars);
	}
	public static String HexEncode(Bytes bytes) {
		final int n = bytes.len;
		char[] chars = new char[2 * n];
		for (int i = 0; i < n; i++) {
			final byte b = bytes.arr[bytes.off + i];
			chars[2 * i] = HexChars[(b >> 4) & 15];
			chars[2 * i + 1] = HexChars[b & 15];
		}
		return CharsToString(chars);
	}

	public static Bytes HexDecodeIgnoringJunk(String a) {
		final int alen = a.length();
		Bytes z = new Bytes();
		boolean complete = true;
		int x = 0;
		for (int i = 0; i < alen; i++) {
			int p = ValueOfHexCharIgnoringJunk(a.charAt(i));
			if (p < 0) continue;
			if (complete) {
				x = p << 4;
				complete = false;
			} else {
				z.appendByte((byte)(x | p));
				complete = true;
			}
		}
		return z;
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

	public static String Show(HashMap map) {
		if (map == null) {
			return "{map IS_NULL}";
		}
		StringBuffer sb = new StringBuffer();
		sb.append("{map ");
		for (Object k : map.keySet()) {
			sb.append("[ " + CurlyEncode(k.toString()) + " ]= " + CurlyEncode(map.get(k).toString())
					+ " ");
		}
		sb.append("}");
		return sb.toString();
	}

	public static String Show(String[] ss) {
		if (ss == null) {
			return "{arr IS_NULL}";
		}
		StringBuffer sb = new StringBuffer();
		sb.append("{arr ");
		for (int i = 0; i < ss.length; i++) {
			String ssi = ss[i] == null? "*null*" : ss[i];
			sb.append("[" + i + "]= " + CurlyEncode(ssi) + " ");
		}
		sb.append("}");
		return sb.toString();
	}

	public static String Show(byte[] bb) {
		StringBuffer sb = new StringBuffer();
		sb.append(Fmt("{bytes*%d ", bb.length));
		for (int i = 0; i < bb.length; i++) {
			sb.append(Fmt("%d ", (int) bb[i]));
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
	
	/** Append tails to array, returning new array */
	public static String[] PushBack(String[] array, String...tails) {
		Must(array != null);
		Must(tails != null);
		for (int i = 0; i < array.length; i++) {
			Must(array[i] != null);
			Say("array[%d] = %s", i, array[i]);
		}
		for (int i = 0; i < tails.length; i++) {
			Must(tails[i] != null);
			Say("tails[%d] = %s", i, tails[i]);
		}
		String[] z = new String[array.length + tails.length];
		System.arraycopy(array, 0, z, 0, array.length);
		System.arraycopy(tails, 0, z, array.length, tails.length);

		for (int i = 0; i < z.length; i++) {
			Must(z[i] != null);
			Say("z[%d] = %s", i, z[i]);
		}
		return z;
	}

	public static String ReadWholeTextFile(File f) throws IOException {
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

	public static void WriteWholeTextFile(File f, String value) throws IOException {
		BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(f)));
		w.write(value);
		w.close();
	}

	public static String ReadAllText(InputStream in) throws IOException {
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

	public static String FetchUrlText(String url) throws IOException {
		return FetchUrlText(new URL(url));
	}

	public static String FetchUrlText(URL url) throws IOException {
		URLConnection conn = url.openConnection();
		conn.connect();
		InputStream in = (InputStream) conn.getContent();
		Say("RESULT=" + in);

		String s = ReadAllText(in);
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
					break;
				case '>':
					sb.append("&gt;");
					break;
				case '&':
					sb.append("&amp;");
					break;
				case '"':
					sb.append("&quot;");
					break;
				case '\n':
					sb.append(lineBreaks ? "<br>" : "&#10;");
					break;
				default:
					sb.append(c);
				}
			} else {
				sb.append("&#" + (int) c + ";");
			}
		}
		return sb.toString();
	}


	public String makeUrl(String path, String ...kvkv) {
		String url = path + "?";
		for (int i = 0; i < kvkv.length; i+=2) {
			url += Fmt("&%s=%s", kvkv[i], UrlEncode(kvkv[i+1]));
		}
		return url;
	}

	/** Fmt calls String.format only if it has nonempty argument list */
	public static String Fmt(String fmt, Object... objects) {
		return objects.length > 0 ? String.format(fmt, objects) : fmt;
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

		public Ht(String fmt, Object...args) {
			this.sb = new StringBuffer(htmlEscape(Fmt(fmt, args)));
		}

		public String toString() {
			return sb.toString();
		}

		public Ht add(String fmt, Object...args) {
			sb.append(htmlEscape(Fmt(fmt, args)));
			return this;
		}

		public Ht add(Ht that) {
			sb.append(that.toString());
			return this;
		}

		static public Ht entity(String name) {
			Must(IsAlphaNum(name), "Bad entity: %s", name);
			Ht ht = new Ht();
			ht.sb.append(Fmt("&%s;", name));
			return ht;
		}
		static public Ht tag(Ht appendMe, String type, String[] args, Ht body) {
			Ht z = appendMe == null ? new Ht() : appendMe;
			assert htmlTagP.matcher(type).matches();
			Say("HT TAG TYPE: %s", type);
			z.sb.append(Fmt("<%s ", type));
			if (args != null) {
				for (int i = 0; i < args.length; i += 2) {
					assert htmlTagP.matcher(args[i]).matches();
					Say("HT TAG PARAM: %s -> {%s}", args[i], args[i+1]);
					z.sb.append(Fmt("%s=\"%s\" ", args[i],
							htmlEscape(args[i + 1])));
				}
			}
			z.sb.append(Fmt(">%s</%s\n>", body, type));
			return z;
		}

		static public Ht tag(Ht appendMe, String type, String[] args,
				String body) {
			return tag(appendMe, type, args, new Ht(body));
		}

		static public Ht tag(Ht appendMe, String type, String[] args) {
			return tag(appendMe, type, args, "");
		}
		
		public Ht addTag(String type, String[] args, Ht body) {
			return tag(this, type, args, body);
		}
		
		public Ht addTag(String type, String[] args, String body) {
			return tag(this, type, args, body);
		}
		
		public Ht addTag(String type, String[] args) {
			return tag(this, type, args);
		}
	}

	public static boolean IsAlphaNum(String s) {
		if (s == null) {
			return false;
		}
		final int n = s.length();
		if (n == 0) {
			return false;  // Empty string NOT OK.
		}
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

	public static boolean IsDecent(String s) {
		if (s == null) {
			return false;
		}
		final int n = s.length();
		if (n == 0) {
			return false;  // Empty string NOT OK.
		}
		for (int i = 0; i < n; i++) {
			char c = s.charAt(i);
			if ('0' <= c && c <= '9')
				continue;
			if ('a' <= c && c <= 'z')
				continue;
			if ('A' <= c && c <= 'Z')
				continue;
			if (c == '_' || c == '.' || c == '/')
				continue;
			return false;
		}
		return true;
	}
	
	public String[] Tail(String[] a) {
		final int n = a.length;
		if (n < 1) {
			Bad("Tail() on empty String[]");
		}
		String[] z = new String[n - 1];
		for (int i = 0; i < n - 1; i++) {
			z[i] = a[i + 1];
		}
		return z;
	}

	public static RuntimeException Bad(String msg, Object... args) {
		throw new RuntimeException("BAD: " + Fmt(msg, args));
	}

	public static void Say(String msg, Object... args) {
		System.err.println(Fmt("## " + msg, args));
	}

	public static void DontSay(String msg, Object... args) {
	}

	public static void Must(boolean cond, String msg, Object... args) {
		if (!(cond)) {
			throw Bad("Must Failed: " + msg, args);
		}
	}
	public static void Must(boolean cond) {
		if (!(cond)) {
			throw Bad("Must Failed.");
		}
	}
	public static void Must(boolean cond, Object x) {
		if (!(cond)) {
			throw Bad("Must Failed: (%s)", x);
		}
	}
	public static void MustNotBeNull(Object obj) {
		if (obj == null) {
			throw Bad("Must Failed: null.");
		}
	}

	// public static Charset utf8 = Charset.forName("utf-8");
	public static byte[] StringToUtf8(String s) {
		try {
			return s.getBytes("utf-8");
		} catch (UnsupportedEncodingException ex) {
			 throw new RuntimeException(ex.toString());
		}
		// return s.getBytes(utf8);
	}
	public static String Utf8ToString(byte[] b) {
		try {
			return new String(b, "utf-8");
		} catch (UnsupportedEncodingException ex) {
			 throw new RuntimeException(ex.toString());
		}
		// return new String(b, utf8);
	}

	public static String GetStackTrace(final Throwable e) {
		final StringBuilder sb = new StringBuilder();
		final OutputStream out = new OutputStream() {
			@Override
			public void write(int ch) throws IOException {
				sb.append((char) ch);
			}
		};
		final PrintStream ps = new PrintStream(out);
		e.printStackTrace(ps);
		return sb.toString();
	}
	
	public static class Progresser extends Yak {
		public void progress(float percent, String s, Object...args) {
			System.err.println(Fmt("[%6.1f] ", percent) + Fmt(s, args));
		}
	}
	
	public static class Logger extends Yak {
		public int verbosity;
		public void log(int level, String s, Object...args) {
			if (level <= this.verbosity) {
				System.err.println(tryFmt(s, args));
			}
		}
		public void show(int level, Throwable ex) {
			log(level, GetStackTrace(ex));
		}
		public final String tryFmt(String s, Object...args) {
			try {
				return Fmt(s, args);
			} catch (Exception ex) {  // Don't let exception prevent logging.
				return "Fmt Exception in Logger: " + ex + ": " + s;
			}
		}
	}
	
	public static abstract class FileIO {
		public abstract BufferedReader openTextFileInput(String filename) throws FileNotFoundException;
		public abstract PrintWriter openTextFileOutput(String filename, boolean worldly) throws IOException;
		public abstract DataInputStream openDataFileInput(String filename) throws FileNotFoundException;
		public abstract DataOutputStream openDataFileOutput(String filename) throws FileNotFoundException;
		
		public String[] listFiles() {
			File dot = new File(".");
			return dot.list();
		}

		public String readTextFile(String filename) {
			try {
				BufferedReader br = openTextFileInput(filename);
				StringBuilder sb = new StringBuilder();
				while (true) {
					String line = br.readLine();
					if (line == null)
						break;
					sb.append(line);
				}
				br.close();
				return sb.toString();
			} catch (IOException e) {
				throw new RuntimeException("Cannot readFile: " + filename, e);
			}
		}

		public void writeTextFile(String filename, String content, boolean worldly) {
			try {
				PrintWriter pw = openTextFileOutput(filename, worldly);
				pw.print(content);
				pw.flush();
				pw.close();
				if (pw.checkError()) {
					throw new IOException("checkError is true");
				}
			} catch (IOException e) {
				throw new RuntimeException("Cannot writeFile: " + filename, e);
			}
		}
		
		public DataInputStream openURandom() throws FileNotFoundException {
			return new DataInputStream(new FileInputStream("/dev/urandom"));
		}
	}
	
	public static class JavaFileIO extends FileIO {

		@Override
		public BufferedReader openTextFileInput(String filename) throws FileNotFoundException {
			return new BufferedReader(new FileReader(new File(filename)));
		}

		@Override
		public PrintWriter openTextFileOutput(String filename, boolean worldly) throws IOException {
			return new PrintWriter(new BufferedWriter(new FileWriter(filename)));
		}

		@Override
		public DataInputStream openDataFileInput(String filename) throws FileNotFoundException {
			return new DataInputStream(new FileInputStream(filename));
		}

		@Override
		public DataOutputStream openDataFileOutput(String filename) throws FileNotFoundException {
			return new DataOutputStream(new FileOutputStream(filename));
		}
	}
}

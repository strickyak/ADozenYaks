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
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.regex.Pattern;

public abstract class Yak {

	public static int ValueOfHexChar(char c) {
		if ('0' <= c && c <= '9') {
			return c - '0';
		}
		if ('A' <= c && c <= 'F') {
			return c - 'A' + 10;
		}
		if ('a' <= c && c <= 'f') {
			return c - 'a' + 10;
		}
		throw new IllegalArgumentException();
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
		String z = r.readLine();
		r.close();
		return z;
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

}

package yak.etc;

import java.io.File;
import java.util.HashMap;

import org.json.JSONObject;

public class Json extends Yak {
	public static class Parser {
		byte[] bb; // string to be parsed
		int len; // a.length
		int p = 0; // current position.

		public char token; // [ ] { } n(ull) t(rue) f(alse) s(tring) f(loat)
							// e(nd)
		public String str; // value if token s
		public Double flo; // value of token f

		public Parser(String s) {
			this.bb = StringToBytes(s);
			this.len = bb.length;
			Say("Parser len=%d %d: %s", len, s.length(), CurlyEncode(s));
			Say("Parser bb= %s", Show(bb));
			advance();
		}

		public void advance() {
			skipJunk();
			str = "";
			flo = 0.0;
			if (p >= len) {
				token = 'e';
				return;
			}
			final char c = (char) bb[p];
			switch (c) {
			case '{':
			case '}':
			case '[':
			case ']':
				token = c;
				step();
				break;
			case 'n':
				token = 'n';
				expect("null");
				break;
			case 't':
				token = 't';
				expect("true");
				break;
			case 'f':
				token = 'f';
				expect("false");
				break;
			case '"':
				token = 's';
				parseString();
				break;
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
			case '-':
				token = 'f';
				parseFloat();
				break;
			default:
				throw Bad("Bad char %d at %d", (int) c, p);
			}
		}

		void parseFloat() {
			StringBuilder sb = new StringBuilder();
			while (true) {
				final char c = (char) bb[p];
				if ('0' <= c && c <= '9' || c == '-' || c == '+' || c == '.'
						|| c == 'e' || c == 'E') {
					sb.append(c);
					step();
				} else {
					break;
				}
			}
			step();
			flo = Double.parseDouble(sb.toString());
		}

		void parseString() {
			step();
			StringBuilder sb = new StringBuilder();
			while (true) {
				char c = (char) bb[p];
				if (c == '"') {
					break;
				}
				if (c == '\\') {
					step();
					final char d = (char) bb[p];
					switch (d) {
					case '"':
					case '\\':
					case '/':
						c = d;
						break;
					case 'n':
						c = '\n';
						break;
					case 'r':
						c = '\r';
						break;
					case 'b':
						c = '\b';
						break;
					case 'f':
						c = '\f';
						break;
					case 't':
						c = '\t';
						break;
					case 'u':
						step();
						int h1 = ValueOfHexChar((char) bb[p]);
						step();
						int h2 = ValueOfHexChar((char) bb[p]);
						step();
						int h3 = ValueOfHexChar((char) bb[p]);
						step();
						int h4 = ValueOfHexChar((char) bb[p]);
						c = (char) ((h1 << 12) | (h2 << 8) | (h3 << 4) | h4);
						break;
					default:
						throw Bad("Backslash Escape %d", (int) d);
					}
				}
				sb.append(c);
				step();
			}
			step();
			str = sb.toString();
		}

		void expect(String w) {
			final int n = w.length();
			for (int i = 0; i < n; i++) {
				if (w.charAt(i) != bb[p + i]) {
					throw Bad("Json Parser expected %s got %d at %d", w, bb[p
							+ i], i);
				}
			}
			p += n;
		}

		void skipJunk() {
			while (p < len) {
				final char c = (char) bb[p];
				if (c <= ' ' || c == ',' || c == ':') {
					Say("skip %d", (int) c);
					step();
					continue;
				} else {
					Say("stop skip");
					break;
				}
			}
		}

		void step() {
			++p;
			if (p < len) {
				Say("Step [%3d] '%c'=%d", p, (char) bb[p], (int) bb[p]);
			} else {
				Say("Step EOS");
			}
		}
		
		public String takeStringValue() {
			Must(token == 's', token);
			String z = str;
			advance();
			Say("takeStringValue -> (%s)", z);
			return z;
		}
	}

	@SuppressWarnings("rawtypes")
	public static String Show(HashMap map) {
		if (map == null) {
			return "null";
		}
		StringBuffer sb = new StringBuffer();
		sb.append("{");
		for (Object k : map.keySet()) {
			sb.append(Fmt(" %s: %s,\n", Quote(k.toString()), Quote(map.get(k).toString())));
		}
		sb.append("}\n");
		return sb.toString();
	}
	
	public static String Quote(String s) {
		if (s == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("\"");
		final int n = s.length();
		for (int i = 0; i < n; i++) {
			char c = s.charAt(i);
			switch (c) {
			case '\"':
				sb.append("\\\"");
			case '\\':
				sb.append("\\\\");
			case '\b':
				sb.append("\\b");
			case '\f':
				sb.append("\\f");
			case '\n':
				sb.append("\\n");
			case '\r':
				sb.append("\\r");
			case '\t':
				sb.append("\\t");
			default:
				if (' ' <= c && c <= '~') {
					sb.append(c);
				} else {
					sb.append(Fmt("\\u%04x", (int) c));
				}
			}
		}
		sb.append("\"");
		return sb.toString();
	}

	public static void main(String[] a) {
		String filename = a[0];
		try {
			String s = ReadWholeFile(new File(filename));
			Parser parser = new Parser(s);
			while (parser.token != 'e') {
				Say("Token %d %c Float %f Str %s", (int) parser.token,
						parser.token, parser.flo, CurlyEncode(parser.str));
				parser.advance();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		Say("End.");
	}
}

package yak.etc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

public abstract class BaseServer extends Yak implements Runnable {
	int port;
	public Logger log;

	public BaseServer(int port, Logger logger) {
		this.port = port;
		this.log = logger == null ? new Logger() : logger;
	}

	public abstract Response handleRequest(Request req);

	public void run() {
		try {
			final ServerSocket serverSocket = new ServerSocket(port);
			log.log(0, "Listening on port " + port);
			while (true) {
				final Socket clientSocket = serverSocket.accept();

				Thread th = new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							handleConnection(clientSocket);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
				th.start();
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	// Use LATIN_1 to work around the char/byte problem.
	public static final Charset LATIN_1 = Charset.forName("ISO-8859-1");

	public void handleConnection(Socket clientSocket) throws IOException {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					clientSocket.getInputStream(), LATIN_1));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					clientSocket.getOutputStream()));
			Request r = new Request(reader);

			// Call overridden handleRequest(r).
			Response w = handleRequest(r);

			writer.write("HTTP/1.0 " + w.responseCode + " " + w.responseCode
					+ "\r\n");
			writer.write("Content-Type: " + w.contentType + "\r\n");
			writer.write("Content-Length: " + w.payload.length() + "\r\n");
			writer.write("\r\n");
			writer.write(w.payload);
			writer.close();
			reader.close();
		} finally {
			clientSocket.close();
		}
	}

	public static class Request extends Yak {
		static final Pattern START_HEADER = Pattern
				.compile("^([-A-Za-z0-9]+):(.*)$");
		static final Pattern CONTINUE_HEADER = Pattern.compile("^(\\s.*)$");
		public String verb;
		public String[] path;
		public HashMap<String, String> query = new HashMap<String, String>();
		String contentType;
		int contentLength; // Length in bytes, not chars.
		char[] content; // TODO: Should be byte[]
		
		public Request(String fakeRequest) {
			ParseQueryPiecesToMap(fakeRequest, this.query);
		}

		public Request(BufferedReader reader) throws IOException {
			// TODO -- BROKEN -- byte vs char probs. Might break on non-ASCII.
			String line0 = reader.readLine();
			System.err.println("GOT: " + line0);
			if (line0 == null) {
				return;  // TODO: WHEN DOES THIS OCCUR?
			}

			HashMap<String, String> headers = new HashMap<String, String>();
			String key = "None";
			String s;
			while ((s = reader.readLine()) != null) {
				System.err.println("Got: " + s);
				if (s.length() == 0) {
					break;
				}
				Matcher m = START_HEADER.matcher(s);
				if (m.matches()) {
					key = m.group(1).toLowerCase();
					headers.put(key, m.group(2));
				}
				m = CONTINUE_HEADER.matcher(s);
				if (m.matches()) {
					headers.put(key, headers.get(key) + s);
				}
			}

			contentType = headers.get("content-type");
			String contentLenStr = headers.get("content-length");
			contentLength = 0;

			// We only care about application/x-www-form-urlencoded, for now.
			if (contentType != null
					&& contentType.trim().toLowerCase()
							.equals("application/x-www-form-urlencoded")) {
				contentLength = Integer.parseInt(contentLenStr.trim());
			}

			System.err.println("CONTENT " + contentType + "#" + contentLenStr
					+ "#" + contentLength);

			// line0 splits on spaces, like
			// "VERB /a/b/c?h=480&w=640 VERSION"
			String[] words = line0.split(" ");
			this.verb = words[0];
			String[] pathAndQuery = words[1].split("\\?", 2);

			// Assume path does not need UrlDecode; any funny stuff goes in
			// query.
			path = pathAndQuery[0].substring(1).split("/");

			if (pathAndQuery.length == 2) {
				ParseQueryPiecesToMap(pathAndQuery[1], this.query);
			}

			if (contentLength > 0) {
				content = new char[contentLength];
				int countToGO = contentLength;
				while (countToGO > 0) {
					// TODO: Use byte[] not char[].
					countToGO -= reader.read(content,
							contentLength - countToGO, countToGO);
				}				
				ParseQueryPiecesToMap(new String(content), this.query);
			}
			
			System.err.println(Fmt("PATH : %s", Show(path)));
			for (String q : this.query.keySet()) {
				System.err.println(Fmt("QUERY : %s -> %s", CurlyEncode(q), CurlyEncode(query.get(q))));
			}
		}

		public static void ParseQueryPiecesToMap(String queryString, HashMap<String, String> map) {
			for (String queryPiece : queryString.split("\\&")) {
				String[] kv = queryPiece.split("=", 2);
				if (kv.length == 2) {
					map.put(UrlDecode(kv[0]), UrlDecode(kv[1]));
				}
			}
		}

		public String mustGetQuery(String q) {
			String z = query.get(q);
			if (z == null) {
				throw Bad("Missing getQuery: %s", q);
			}
			return z;
		}
		public String mustGetDecentQuery(String q) {
			String z = mustGetQuery(q);
			if (IsDecent(z)) {
				return z;
			} else {
				Bad("Bad mustGetDecentQuery: %s -> %s", CurlyEncode(q), CurlyEncode(z));
				return "";  // NOTREACHED
			}
		}
	}
 
	public static class Response extends Yak {
		int responseCode = 200;
		String contentType = "text/html";
		String payload = "";

		public Response(String payload) {
			this.payload = payload;
		}

		public Response(String payload, int responseCode, String contentType) {
			this.payload = payload;
			this.responseCode = responseCode;
			this.contentType = contentType;
		}
	}
}

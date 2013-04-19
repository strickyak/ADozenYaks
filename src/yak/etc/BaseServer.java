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

public abstract class BaseServer extends Yak implements Runnable {
	int port;

	public BaseServer(int port) {
		this.port = port;
	}

	public abstract Response handleRequest(Request req);

	public void run() {
		try {
			final ServerSocket serverSocket = new ServerSocket(port);
			System.err.println("Listening on port " + port);
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

		public Request(BufferedReader reader) throws IOException {
			// TODO -- BROKEN -- byte vs char probs. Might break on non-ASCII.
			String line0 = reader.readLine();

			HashMap<String, String> headers = new HashMap<String, String>();
			String key = "None";
			String s;
			while ((s = reader.readLine()) != null) {
				System.err.println("GOT: " + s);
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

			if (contentType != null && contentType.trim().toLowerCase()
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
				for (String queryPiece : pathAndQuery[1].split("\\&")) {
					String[] kv = queryPiece.split("=", 2);
					if (kv.length == 2) {
						this.query.put(UrlDecode(kv[0]), UrlDecode(kv[1]));
					}
				}
			}

			content = new char[contentLength];
			int countToGO = contentLength;
			while (countToGO > 0) {
				// TODO: Use byte[] not char[].
				countToGO -= reader.read(content, contentLength - countToGO,
						countToGO);
			}

			System.err.println("CONTENT: " + CurlyEncode(new String(content)));
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

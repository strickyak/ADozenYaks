package yak.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import android.util.Log;

import yak.etc.BaseServer;
import yak.etc.DH;
import yak.etc.BaseServer.Request;
import yak.etc.BaseServer.Response;
import yak.etc.Yak;

public class AppServer extends BaseServer {
	
	public static final int DEFAULT_PORT = 4004;
	private String appMagicWord;
	private String storagePath;
	private FileIO fileIO;
	
	private DH myDH = DH.RandomKey();
	
	public static void main(String[] args) {
		try {
			if (args.length != 1) {
				throw Bad("AppServer needs one arg, the magic word: ", Show(args));
			}
			String magic = args[0];
			if (!(IsAlphaNum(magic))) {
				throw Bad("magicWord must be alphaNum.");
			}

			new Thread(new StoreServer(StoreServer.DEFAULT_PORT, magic)).start();
			
			new AppServer(DEFAULT_PORT, magic,
					Fmt("http://localhost:%d/%s", StoreServer.DEFAULT_PORT, magic),
					null).run();
		} catch (Exception e) {
			System.err.println("CAUGHT: " + e);
			e.printStackTrace();
		}
	}

	public AppServer(int port, String appMagicWord, String storagePath, FileIO fileIO) {
		super(port);
		this.appMagicWord = appMagicWord;
		this.storagePath = storagePath;
		this.fileIO = (fileIO == null) ? new JavaFileIO() : fileIO;
		System.err.println(Fmt("Hello, this is AppServer on %d with %s", DEFAULT_PORT, appMagicWord));
		System.err.println(Fmt("Constructed storagePath=%s", DEFAULT_PORT, storagePath));
	}

	public Response handleRequest(Request req) {
		Say("Req %s", req);
		Say("AppServer handleRequest path= %s query= %s", Show(req.path), Show(req.query));
		
		if (req.path[0].equals("favicon.ico")) {
			return new Response("No favicon.ico here.", 404, "text/plain");
		}
		if (!(req.path[0].equals(appMagicWord))) {
			throw Bad("Bad Magic Word: %s", Show(req.path));
		}

		String verb = req.query.get("verb");
		
		String path = req.query.get("path");
		if (path != null) {
			while (path.length() > 0 && path.charAt(0) == '/') {
				path = path.substring(1);
			}
			verb = path;
		}
		
		String query = req.query.get("query");
		if (query != null) {
			req.ParseQueryPiecesToMap(query, req.query);
		}
		
		String user = req.query.get("u");
		String channel = req.query.get("c");
		String tnode = req.query.get("t");
		String latest = req.query.get("latest");
		String value = req.query.get("v");
		verb = (verb == null) ? "null" : verb;
		String z = "!";

		try {
			if (verb.equals("Boot")) {
				z = doVerbBoot();
			} else if (verb.equals("Chan")) {
				z = doVerbChan(channel);
			} else if (verb.equals("Show")) {
				z = doVerbShow(req.query);
			} else if (verb.equals("Rendez")) {
				z = doVerbRendez(req.query);
			} else if (verb.equals("Rendez2")) {
				z = doVerbRendez2(req.query);
			} else {
				throw new Exception("bad Verb: " + verb);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new Response("!\r\nERROR:\r\n" + e.getMessage(), 200,
					"text/plain");
		}

		return new Response(z, 200, "text/html");
	}

	private String doUri(String uri) throws IOException {
		String a = uri.toString();
		if (a.startsWith("yak12:")) {
			a = a.substring(6);
		}
		System.err.println("+++ doUri: <" + a);
		String z = htmlEscape("URI IS " + CurlyEncode(a));
		System.err.println("+++ doUri: >" + z);
		return z;
	}

	private String doVerbBoot() throws IOException {
		return UseStore("Boot", "");
	}

	private String doVerbChan(String channel) throws IOException {
		String[] tnodes = UseStore("list", "c=" + channel).split("\n");
		String z = Fmt("doVerbChan: c=%s; tnodes=%s", channel, Show(tnodes));
		z += "<br><dl>\n";
		for (String t : tnodes) {
			if (!(IsAlphaNum(t))) {
				throw Bad("listed tnode not alphanum: %s", Show(tnodes));
			}
			
			z += "<dt><b>" + t + "</b><br>\n";
			z += "<dd><pre>\n" + UseStore("fetch", Fmt("c=%s&t=%s", channel, t)) + "\n</pre>\n";
		}
		z += "</dl><p> OK.";
		return z;
	}
	

	private String doVerbRendez(HashMap<String,String> q) throws IOException {
			int myNewCode = DH.randomInt(89999) + 10000;
			return "Your code is " + myNewCode + "<P> Enter friend's code: <BR>"
					+ "<form method=GET action=/>"
							+ "<input type=text name=you><br>"
							+ "<input type=hidden name=me value=" + myNewCode + ">"
							+ "<input type=hidden name=verb value=Rendez2>"
					+ "<input type=submit>"
					+ "</form>";
	}
	
	private String doVerbRendez2(HashMap<String,String> q) throws IOException {
		String x = ReadUrl(Fmt("%s?f=Rendez&me=%s&you=%s&v=%s",
				storagePath, q.get("me"), q.get("you"), myDH.publicKey()));
		return new Ht("RENDEZ RESULT " + x).toString();
	}
	

	private String doVerbShow(HashMap<String,String> q) throws IOException {
		return new Ht(q.toString()).toString();
	}
	
	public void setStoragePath(String storagePath) {
		this.storagePath = storagePath;
	}
	
	public String UseStore(String verb, String args) throws IOException {
		if (storagePath.equals("")) {
			Bad("No storage Path in AppServer");
		}
		return ReadUrl(Fmt("%s?f=%s&%s", storagePath, verb, args));
	}
	
	public static class Sessions {
		HashMap<String, Session> dict = new HashMap<String, Session>();
		public synchronized Session get(String key) {
			return dict.get(key);
		}
		public synchronized void put(String key, Session ses) {
			dict.put(key, ses);
		}
	}

	public static class Session {
		public Profile.Self self;
	}
}

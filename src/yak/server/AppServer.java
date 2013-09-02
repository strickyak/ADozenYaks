package yak.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import org.apache.http.HttpEntity;

import android.util.Log;

import yak.etc.BaseServer;
import yak.etc.DH;
import yak.etc.BaseServer.Request;
import yak.etc.BaseServer.Response;
import yak.etc.Yak;

public class AppServer extends BaseServer {
	
	public static final int DEFAULT_PORT = 30331;
	private String appMagicWord;
	private String storagePath;
	private FileIO fileIO;
	
	private DH myDH = null;
	
	public static void main(String[] args) {
		try {
			if (args.length != 1) {
				throw Bad("AppServer needs one arg, the magic word: ", Show(args));
			}
			String magic = args[0];
			if (!(IsAlphaNum(magic))) {
				throw Bad("magicWord must be alphaNum.");
			}

			// new Thread(new StoreServer(StoreServer.DEFAULT_PORT, magic)).start();
			
			new AppServer(
					DEFAULT_PORT, magic,
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
	
	private static String param(Request req, String key) {
		String x = req.query.get(key);
		if (x == null) {
			Bad("Missing Query Parameter: %s", key); 
		}
		if (!IsAlphaNum(x)) {
			Bad("Query Parameter Not AlphaNum: %s", CurlyEncode(key));
		}
		return x;
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
				z = doVerbChan(param(req, "c"));
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
		return UseStore("Boot");
	}

	private String doVerbChan(String channel) throws IOException {
		String[] tnodes = UseStore("list", "c", channel).split("\n");
		String z = Fmt("doVerbChan: c=%s; tnodes=%s", channel, Show(tnodes));
		z += "<br><dl>\n";
		for (String t : tnodes) {
			if (!(IsAlphaNum(t))) {
				throw Bad("listed tnode not alphanum: %s", Show(tnodes));
			}
			
			z += "<dt><b>" + t + "</b><br>\n";
			z += "<dd><pre>\n" + UseStore("fetch", "c", channel, "t", t) + "\n</pre>\n";
		}
		z += "</dl><p> OK.";
		return z;
	}

    /** doVerbRendez tells our peer code and requests peer code of future pal. */
	private Ht doVerbRendez(HashMap<String,String> q) throws IOException {
			int myNewCode = DH.randomInt(8999) + 1000;
//			return "<form method=GET action=\"/\" >"
//			   		+   "Your code is " + myNewCode + "<P> Enter friend's code: <BR>"
//					+   "<input type=text name=you><br>"
//					+   "<input type=hidden name=me value=" + myNewCode + ">"
//					+   "<input type=hidden name=verb value=Rendez2>"
//					+   "<input type=submit>"
//					+ "</form>";
			Ht text = new Ht("Your code is " + myNewCode + "<P> Enter friend's code: <BR>");
			Ht body = new Ht();
			body.append("Your code is " + myNewCode);
			Ht.tag(body, "p", null);
			body.append("Enter new friend's code:");
			Ht.tag(body, "br", null);
			Ht.tag(body, "input", strings("type", "text", "name", "you"));
			Ht.tag(body, "br", null);

			Ht.tag(body, "input", strings("type", "hidden", "name", "me", "value", myNewCode));
			Ht.tag(body, "input", strings("type", "hidden", "name", "verb", "value", "Rendez2"));
			Ht.tag(body, "input", strings("type", "submit"));
			return Ht.tag(null, "form", strings(
					"method", "GET",
					"action", "/" + appMagicWord), body);
	}
	
	/** doVerbRendez2 does the DH Public Key Exchange. */
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
	
	public String UseStore(String verb, String ...kvkv) throws IOException {
		if (!storagePath.startsWith("htp://")) {
			Bad("Bad StoragePath in AppServer: %s", UrlEncode(storagePath));
		}
		String url = Fmt("%s?f=%s", storagePath, verb);
		for (int i = 0; i < kvkv.length; i+=2) {
			url += Fmt("&%s=%s", kvkv[i], UrlEncode(kvkv[i+1]));
		}
		return ReadUrl(url);
	}

	public Ht makeAppLink(String text, String verb, String ...kvkv) {
		String url = Fmt("/%s?f=%s", appMagicWord, verb);
		for (int i = 0; i < kvkv.length; i+=2) {
			url += Fmt("&%s=%s", kvkv[i], UrlEncode(kvkv[i+1]));
		}
		return Ht.tag(null, "a", strings("href", url), text);
	}
}

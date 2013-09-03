package yak.server;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import org.apache.http.HttpEntity;

import android.util.Log;

import yak.etc.BaseServer;
import yak.etc.Bytes;
import yak.etc.DH;
import yak.etc.Hash;
import yak.etc.BaseServer.Request;
import yak.etc.BaseServer.Response;
import yak.etc.Yak.Ht;
import yak.etc.Yak;
import yak.server.Proto.Friend;
import yak.server.Proto.Persona;
import yak.server.Proto.Room;

public class AppServer extends BaseServer {
	
	public static final int DEFAULT_PORT = 30331;
	private String appMagicWord;
	private String storagePath;
	private FileIO fileIO;
	private Persona persona;
	
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
	
	private String action() {
		return "/" + appMagicWord;
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
				z = doVerbRendez(req.query).toString();
			} else if (verb.equals("Rendez2")) {
				z = doVerbRendez2(req.query).toString();
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
			String myNewCode = "" + DH.randomInt(899) + 100;  // Choose from 100..999
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
	private Ht doVerbRendez2(HashMap<String,String> q) throws IOException {
		String mine = Fmt("DH %s %s HD", persona.name, persona.dhpub);
		String x = UseStore("Rendez", 
				"me", q.get("me"), 
				"you", q.get("you"), 
				"v", myDH.publicKey().toString());
		if (x==null || x.length()==0 || x.charAt(0)=='!') {
			return new Ht("Peering failed.  Go back and try again.");
		}
		String[] w = x.split(x, ' ');
		if (w.length < 4) {
			Bad("Received peering value too short: [%d] %s", w.length, x);
		}
		if (!IsAlphaNum(w[0]) || !IsAlphaNum(w[1]) || !IsAlphaNum(w[2]) || !IsAlphaNum(w[3])) {
			Bad("Received peering values not alphanum: %s", x);
		}
		if (!w[0].equals("DH") || !w[3].equals("HD")) {
			Bad("Not magic numbers: %s, %s", w[0], w[3]);
		}
		String theirName = w[1];
		String theirPub = w[2];
		Friend f = findFriend(theirName);
		if (f != null) {
			// TODO: handle ambiguous names.
			Bad("Already have a friend named %s", theirName);
		}
		DH theirDH = new DH(theirPub);
		DH mutualDH = myDH.mutualKey(theirDH);
		String check = new Hash(mutualDH.toString()).asShortString();
		
		Ht z = new Ht(Fmt("Confirm peering with name '%s'.", theirName));
		Ht.tag(z, "p", null);
		z.append(Fmt("For your security, make sure that you BOTH got checksum '%s'.", check));
		Ht.tag(z, "p", null);
		
		Ht inputs = new Ht("");
		Ht.tag(inputs, "input", strings("type", "hidden", "name", "name", "value", theirName));
		Ht.tag(inputs, "input", strings("type", "hidden", "name", "pub", "value", theirPub));
		Ht.tag(inputs, "input", strings("type", "hidden", "name", "verb", "value", "Rendez3"));
		Ht.tag(inputs, "input", strings("type", "submit", "name", "name", "value", theirName));
		
		Ht.tag(z, "form", strings("method", "GET", "action", action()), inputs);
		return z;
	}

	/** doVerbRendez3 actually saves the confirmed friend. */
	private Ht doVerbRendez3(HashMap<String,String> q) throws IOException {
		String theirName = q.get("name");
		String theirPub = q.get("pub");
		Friend f = findFriend(theirName);
		if (f != null) {
			// TODO: handle ambiguous names.
			Bad("Already have a friend named %s", theirName);
		}
		f = new Friend();
		f.name = theirName;
		f.dhpub = theirPub;
		f.alias = "";
		f.hash = new Hash(theirPub).asShortString();
		persona.friend.add(f);
		savePersona();
		
		return new Ht(Fmt("Saved new friend: %s", theirName));
	}
	private void savePersona() {
		Bytes b = new Bytes();
		Proto.PicklePersona(persona, b);
		try {
			DataOutputStream dos = fileIO.openDataFileOutput(Fmt("dozen_%s.txt", persona.name));
			dos.write(b.arr, b.off, b.len);
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
			Bad("Cannot save persona: %s", e);
		}
	}
	private void loadPersona(String name) {
		try {
			DataInputStream dis = fileIO.openDataFileInput(Fmt("dozen_%s.txt", name));
			int avail = dis.available();
			byte[] b = new byte[avail];
			dis.readFully(b);
			dis.close();
			persona = Proto.UnpicklePersona(new Bytes(b));
		} catch (IOException e) {
			e.printStackTrace();
			Bad("Cannot load persona: %s", e);
		}
	}
	private Friend findFriend(String sought) {
		// TODO: disambiguate with alias or hash
		for (Friend f : persona.friend) {
			if (f.name.equals(sought)) {
				return f;
			}
		}
		return null;
	}

	private Room findRoom(String sought) {
		String[] w = sought.split("@");
		if (w.length == 2) {
			return findRoom(w[0], w[1]);
		} else {
			return null;
		}
	}
	private Room findRoom(String soughtRoom, String soughtFriend) {
		Friend f = findFriend(soughtFriend);
		if (f != null) {
			// TODO: disambiguate with alias or hash
			for (Room r : f.room) {
				if (r.name.equals(soughtRoom)) {
					return r;
				}
			}
		}
		return null;
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

package yak.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;

import yak.etc.BaseServer;
import yak.etc.BaseServer.Request;
import yak.etc.BaseServer.Response;
import yak.etc.Yak;

public class AppServer extends BaseServer {
	
	public static final int DEFAULT_PORT = 30333;
	
	public static void main(String[] args) {
		try {
			// Run StoreServer in background.
			new Thread(new StoreServer(30333)).start();
			
			// Run TheSerer in main thread.
			new AppServer(DEFAULT_PORT).run();
			// "Boot" initialize the storage.
			Thread.sleep(1 * 1000); // Wait 1 sec, first.
			ReadUrl(fmt("http://localhost:%d/?f=boot", StoreServer.DEFAULT_PORT));
		} catch (Exception e) {
			System.err.println("CAUGHT: " + e);
			e.printStackTrace();
		}
	}

	public AppServer(int port) {
		super(port);
		// Log.i("AppServer", fmt("AppServer Constructed on port %s", port));
	}

	public Response handleRequest(Request req) {
		
		String z = "{REQ PATH=" + Show(req.path) + " QUERY=" + Show(req.query)
				+ "}";
		System.err.println(z);

		if (req.path[0].equals("favicon.ico")) {
			return new Response("No favicon.ico here.", 404, "text/plain");
		}

		String uri = req.query.get("uri");
		String verb = req.query.get("f");
		String user = req.query.get("u");
		String channel = req.query.get("c");
		String tnode = req.query.get("t");
		String latest = req.query.get("latest");
		String value = req.query.get("value");
		verb = (verb == null) ? "null" : verb;

		try {
			if (verb.equals("boot")) {
				z = doVerbBoot();
			} else if (verb.equals("chan")) {
				z = doVerbChan(channel);
			} else if (uri != null) {
				z = doUri(uri);
			} else {
				throw new Exception("bad Verb: " + verb);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new Response("ERROR:\r\n" + e.getMessage(), 200,
					"text/plain");
		}

		return new Response(z, 200, "text/html");
	}

	private String doUri(String uri) throws IOException {
		String a = uri.toString();
		System.err.println("+++ doUri: <" + a);
		String z = htmlEscape("URI IS " + CurlyEncode(a));
		System.err.println("+++ doUri: >" + z);
		return z;
	}

	private String doVerbBoot() throws IOException {
		return UseStore("boot", "");
	}

	private String doVerbChan(String channel) throws IOException {
		String[] tnodes = UseStore("list", "c=" + channel).split("\n");
		String z = fmt("doVerbChan: c=%s; tnodes=%s", channel, Show(tnodes));
		z += "<br><dl>\n";
		for (String t : tnodes) {
			z += "<dt><b>" + t + "</b><br>\n";
			z += "<dd><pre>\n" + UseStore("fetch", fmt("c=%s&t=%s", channel, t)) + "\n</pre>\n";
		}
		z += "</dl><p> OK.";
		return z;
	}
	
	public String UseStore(String verb, String args) throws IOException {
		return ReadUrl(fmt("http://%s:%s/?f=%s&%s",
				StoreServer.DEFAULT_HOST, StoreServer.DEFAULT_PORT, verb, args));
	}
}

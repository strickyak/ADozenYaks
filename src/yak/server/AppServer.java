package yak.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import yak.etc.BaseServer;
import yak.etc.BaseServer.Request;
import yak.etc.BaseServer.Response;
import yak.etc.Yak;

public class AppServer extends BaseServer {
	
	public static void main(String[] args) {
		try {
			System.err.println("Hello lmnop.");
			// new StoreServer(9999).run();

			String s = ReadUrl("file:///tmp/date");
			System.err.println("RESULT = `" + s + "`");

			// Run StoreServer in background.
			new Thread(new StoreServer(9998)).start();
			
			// Run TheSerer in main thread.
			new AppServer(9999).run();
			ReadUrl("http://localhost:9998/?f=boot");
		} catch (IOException e) {
			System.err.println("CAUGHT: " + e);
			e.printStackTrace();
		}
	}

	public AppServer(int port) {
		super(port);
	}

	public Response handleRequest(Request req) {
		
		String z = "{REQ PATH=" + Show(req.path) + " QUERY=" + Show(req.query)
				+ "}";
		System.err.println(z);

		if (req.path[0].equals("favicon.ico")) {
			return new Response("No favicon.ico here.", 404, "text/plain");
		}

		String verb = req.query.get("f");
		String user = req.query.get("u");
		String channel = req.query.get("c");
		String tnode = req.query.get("t");
		String latest = req.query.get("latest");
		String value = req.query.get("value");

		try {
			if (verb.equals("boot")) {
				z = doVerbBoot();
			} else if (verb.equals("chan")) {
				z = doVerbChan(channel);
			} else {
				throw new Exception("bad Verb: " + verb);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new Response("ERROR:\r\n" + e.getMessage(), 200,
					"text/plain");
		}

		return new Response(z, 200, "text/plain");
	}

	private String doVerbBoot() throws IOException {
		return UseStore("boot", "");
	}

	private String doVerbChan(String channel) throws IOException {
		String[] tnodes = UseStore("list", "c=" + channel).split("\n");
		return "TODO -- WORK HERE";
	}
	
	public String UseStore(String verb, String args) throws IOException {
		return ReadUrl("http;//localhost:9998/?f=" + verb + "&" + args);
	}
}

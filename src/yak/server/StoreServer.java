package yak.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.HashMap;

import yak.etc.BaseServer;

public class StoreServer extends BaseServer {
	public static final int DEFAULT_PORT = 30332;
	public static final String DEFAULT_HOST = "yak.net";
	
	public String magicWord;
	public Rendez rendez = new Rendez();

	public static void main(String[] args) throws IOException {
		System.err.println("Hello, StoreServer! ");
		if (args.length != 1) {
			throw Bad("StoreServer needs one arg, the magic word: ", Show(args));
		}
		String magicWord = args[0];
		if (!(isAlphaNum(magicWord))) {
			throw Bad("magicWord must be alphaNum.");
		}
		new StoreServer(DEFAULT_PORT, magicWord).run();
	}

	public StoreServer(int port, String magicWord) {
		super(port);
		this.magicWord = magicWord;
		System.err.println(fmt("Hello, this is StoreServer on %d with %s", DEFAULT_PORT, magicWord));
	}

	public Response handleRequest(Request req) {
		Say("StoreServer handleRequest path= %s query= %s", Show(req.path), Show(req.query));
		
		if (req.path[0].equals("favicon.ico")) {
			return new Response("No favicon.ico here.", 404, "text/plain");
		}
		
		if (!(req.path[0].equals(magicWord))) {
			throw Bad("Bad Magic Word: (%s) %s", magicWord, Show(req.path));
		}

		String verb = req.getAlphaNumQuery("f");
		String z = "MU";
		
		try {
			if (verb.equals("fetch")) {
				z = doVerbFetch(req);
			} else if (verb.equals("list")) {
				z = doVerbList(req);
			} else if (verb.equals("create")) {
				z = doVerbCreate(req);
			} else if (verb.equals("boot")) {
				z = doVerbBoot(req);
			} else if (verb.equals("rendez")) {
				z = doVerbRendez(req);
			} else {
				throw Bad("Bad verb: " + verb);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new Response("ERROR:\r\n" + e.getMessage(), 200,
					"text/plain");
		}

		return new Response(z, 200, "text/plain");
	}

	public String doVerbList(Request req) {
		String channel = req.getAlphaNumQuery("c");
		// TODO: latest;
		System.err.printf("LIST << channel: %s, latest: %s \n", channel, "TODO");
		File chanDir = new File(String.format("data/%s/", channel));
		String[] inodes = chanDir.list();
		if (inodes == null) {
			throw new RuntimeException(String.format(
					"Channel %s does not exist.", channel));
		}
		String z = Join(inodes, "\n");
		System.err.printf("LIST >> %s\n", z);
		return z;
	}

	public String doVerbFetch(Request req) throws IOException {
		String channel = req.getAlphaNumQuery("c");
		String tnode = req.getAlphaNumQuery("t");
		
		File chanDir = new File(new File("data"), channel);
		File tnodeFile = new File(chanDir, tnode);
		return ReadWholeFile(tnodeFile);
	}

	public String doVerbCreate(Request req) throws IOException {
		String channel = req.getAlphaNumQuery("c");
		String tnode = req.getAlphaNumQuery("t");
		String value = req.getAlphaNumQuery("value");

		File chanDir = new File(new File("data"), channel);
		chanDir.mkdirs();
		File tnodeFile = new File(chanDir, tnode);
		WriteWholeFile(tnodeFile, value);
		return "OK";
	}
	
	public String doVerbBoot(Request req) throws IOException {
		String z = "BOOTING: ";
		
		z += " # " + doVerbCreate(new Request("c=777&t=101&value=first"));
		z += " # " + doVerbCreate(new Request("c=777&t=102&value=second"));
		z += " # " + doVerbCreate(new Request("c=777&t=103&value=third"));
		z += " # " + doVerbCreate(new Request("c=888&t=104&value=fourth"));
		z += " # " + doVerbCreate(new Request("c=888&t=105&value=fifth"));
		
		return z + "\n\n... BOOTED.";
	}
	
	public String doVerbRendez(Request req) throws IOException {
		String me = req.getAlphaNumQuery("me");
		String you = req.getAlphaNumQuery("you");
		String value = req.getAlphaNumQuery("value");
		Rendez.Card theirs = rendez.waitForPeer(me, you, value);
		if (theirs == null) {
			return "!";  // Indicate failure.
		} else {
			return theirs.value;
		}
	}
	
	public static class Rendez {
		// Try: http://yak.net:30332/MagicYak?f=rendez&me=111&you=222&value=one
		// And: http://yak.net:30332/MagicYak?f=rendez&me=222&you=111&value=two
		public static class Card {
			public String me;
			public String you;
			public String value;
			public long timeout;
		}

		public Card waitForPeer(String me, String you, String value) {
			collectGarbage();
			Card mine = new Card();
			mine.me = me;
			mine.you = you;
			mine.value = value;
			mine.timeout = new Date().getTime() + 60*1000;
			put(mine);
			for (int i = 0; i < 15; i++) {
				try {
					Thread.sleep(1*1000);
				} catch (InterruptedException e) {
					throw Bad("waitForPeer interruption: %s", e);
				}
				Card theirs = get(you, me);
				if (theirs != null) {
					return theirs;
				}
			}
			return null;
		}

		private HashMap<String, Card> peers = new HashMap<String, Card>();
		
		private synchronized void put(Card card) {
			Say("PUT CARD timeout=%d me=%s you=%s value=%s", card.timeout, card.me, card.you, card.value);
			peers.put(card.me + " " + card.you, card);
		}
		
		private synchronized Card get(String me, String you) {
			Card z =  peers.get(me + " " + you);
			if (z == null) {
				Say("GET CARD (%s %s) -> NULL", me, you);
			} else {
				Say("GET CARD timeout=%d me=%s you=%s value=%s", z.timeout, z.me, z.you, z.value);
			}
			return z;
		}
		
		private synchronized void collectGarbage() {
			long now = new Date().getTime();
			Say("GC at %d", now);
			for (String key : peers.keySet()) {
				Card card = peers.get(key);
				Say("... CARD timeout=%d me=%s you=%s value=%s", card.timeout - now, card.me, card.you, card.value);
				if (card.timeout < now) {
					peers.remove(key);
				}
			}
		}
	}
}

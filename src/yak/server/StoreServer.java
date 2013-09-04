package yak.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
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
		

		String magic = "magic";
		int port = DEFAULT_PORT;
		for (int i = 0; i < args.length; i += 2) {
			if (args[i].equals("-p")) {
				port = Integer.parseInt(args[i+1]);
			} else if (args[i].equals("-m")) {
				magic = args[i+1];
			} else {
				Bad("Unknown arg: %s", args[i]);
			}
		}
		if (port < 1024) {
			Bad("Port under 1024: %d", port);
		}
		if (!(IsAlphaNum(magic))) {
			throw Bad("magicWord must be alphaNum.");
		}
		if (!(IsAlphaNum(magic))) {
			throw Bad("magicWord must be alphaNum.");
		}
		new StoreServer(port, magic).run();
	}

	public StoreServer(int port, String magicWord) {
		super(port);
		this.magicWord = magicWord;
		System.err.println(Fmt("Hello, this is StoreServer on %d with %s", DEFAULT_PORT, magicWord));
	}

	public Response handleRequest(Request req) {
		Say("StoreServer handleRequest path= %s query= %s", Show(req.path), Show(req.query));
		
		if (req.path[0].equals("favicon.ico")) {
			return new Response("No favicon.ico here.", 404, "text/plain");
		}
		
		if (!(req.path[0].equals(magicWord))) {
			throw Bad("Bad Magic Word: (%s) %s", magicWord, Show(req.path));
		}

		String verb = req.mustGetDecentQuery("f");
		String z = "MU";
		
		try {
			if (verb.equals("Fetch")) {
				z = doVerbFetch(req);
			} else if (verb.equals("List")) {
				z = doVerbList(req);
			} else if (verb.equals("Create")) {
				z = doVerbCreate(req);
			} else if (verb.equals("Boot")) {
				z = doVerbBoot(req);
			} else if (verb.equals("Rendez")) {
				z = doVerbRendez(req);
			} else {
				throw Bad("Bad verb: " + verb);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new Response("ERROR in StoreServer.handleRequest:\r\n" + e.getMessage(), 200,
					"text/plain");
		}

		return new Response(z, 200, "text/plain");
	}

	public String doVerbList(Request req) {
		String channel = req.mustGetDecentQuery("c");
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
		String channel = req.mustGetDecentQuery("c");
		String tnode = req.mustGetDecentQuery("t");
		
		File chanDir = new File(new File("data"), channel);
		File tnodeFile = new File(chanDir, tnode);
		return ReadWholeTextFile(tnodeFile);
	}

	public String doVerbCreate(Request req) throws IOException {
		String channel = req.mustGetDecentQuery("c");
		String tnode = req.mustGetDecentQuery("t");
		String value = req.mustGetDecentQuery("v");

		File chanDir = new File(new File("data"), channel);
		chanDir.mkdirs();
		File tnodeFile = new File(chanDir, tnode);
		WriteWholeTextFile(tnodeFile, value);
		return "OK";
	}
	
	public String doVerbBoot(Request req) throws IOException {
		String z = "BOOTING: ";
		
		z += " # " + doVerbCreate(new Request("c=777&t=101&v=first"));
		z += " # " + doVerbCreate(new Request("c=777&t=102&v=second"));
		z += " # " + doVerbCreate(new Request("c=777&t=103&v=third"));
		z += " # " + doVerbCreate(new Request("c=888&t=104&v=fourth"));
		z += " # " + doVerbCreate(new Request("c=888&t=105&v=fifth"));
		
		return z + "\n\n... BOOTED.";
	}
	
	public String doVerbRendez(Request req) throws IOException {
		String me = req.mustGetDecentQuery("me");
		String you = req.mustGetDecentQuery("you");
		String value = req.mustGetDecentQuery("v");
		Rendez.Card theirs = rendez.waitForPeer(me, you, value);
		if (theirs == null) {
			return "!";  // Indicate failure.
		} else {
			return theirs.value;
		}
	}
	
	public static class Rendez {
		private static final int NUM_SECS_TO_WAIT = 10;
		private static final int NUM_SECS_TILL_GC = 60;
		
		// Try: http://yak.net:30332/MagicYak?f=Rendez&me=111&you=222&v=one
		// And: http://yak.net:30332/MagicYak?f=Rendez&me=222&you=111&v=two
		// $ cd ADozenYaks;  java  -classpath $PWD/bin/classes  yak.server.AppServer  MagicYak
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
			mine.timeout = new Date().getTime() + NUM_SECS_TILL_GC*1000;
			put(mine);
			for (int i = 0; i < NUM_SECS_TO_WAIT; i++) {
				try {
					Thread.sleep(1*1000);  // one second.
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
			Say("PUT CARD timeout=%d me=%s you=%s v=%s", card.timeout, card.me, card.you, card.value);
			peers.put(card.me + " " + card.you, card);
		}
		
		private synchronized Card get(String me, String you) {
			Card z =  peers.get(me + " " + you);
			if (z == null) {
				Say("GET CARD (%s %s) -> NULL", me, you);
			} else {
				Say("GET CARD timeout=%d me=%s you=%s v=%s", z.timeout, z.me, z.you, z.value);
			}
			return z;
		}
		
		private synchronized void collectGarbage() {
			long now = new Date().getTime();
			Say("GC at %d", now);
			ArrayList<String> garbage = new ArrayList<String>(); 
			for (String key : peers.keySet()) {
				Card card = peers.get(key);
				Say("... CARD timeout=%d me=%s you=%s v=%s", card.timeout - now, card.me, card.you, card.value);
				if (card.timeout < now) {
					garbage.add(key);
				}
			}
			for (String key : garbage) {
				peers.remove(key);
			}
		}
	}
}

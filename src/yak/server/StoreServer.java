package yak.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import yak.etc.BaseServer;

public class StoreServer extends BaseServer {
	public static final int DEFAULT_PORT = 30332;

	public static void main(String[] args) throws IOException {
		System.err.println("Hello, World");
		new StoreServer(9998).run();
	}

	public StoreServer(int port) {
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
			if (verb.equals("fetch")) {
				z = doVerbFetch(channel, tnode);
			} else if (verb.equals("list")) {
				z = doVerbList(channel, latest);
			} else if (verb.equals("create")) {
				z = doVerbCreate(channel, tnode, value, user);
			} else if (verb.equals("boot")) {
				z = doVerbBoot();
			} else {
				throw new IllegalArgumentException("Bad verb: " + verb);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new Response("ERROR:\r\n" + e.getMessage(), 200,
					"text/plain");
		}

		return new Response(z, 200, "text/plain");
	}

	public String doVerbList(String channel, String latest) {
		System.err
				.printf("LIST << channel: %s, latest: %s \n", channel, latest);
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

	public String doVerbFetch(String channel, String tnode) throws IOException {
		if (channel == null) {
			throw new RuntimeException("NULL CHANNEL!");
		}
		if (tnode == null) {
			throw new RuntimeException("NULL TNODE!");
		}
		File chanDir = new File(new File("data"), channel);
		File tnodeFile = new File(chanDir, tnode);
		return ReadWholeFile(tnodeFile);
	}

	public String doVerbCreate(String channel, String tnode, String value,
			String user) throws IOException {
		if (channel == null) {
			throw new RuntimeException("NULL CHANNEL!");
		}
		if (tnode == null) {
			throw new RuntimeException("NULL TNODE!");
		}
		if (value == null) {
			throw new RuntimeException("NULL VALUE!");
		}

		File chanDir = new File(new File("data"), channel);
		chanDir.mkdirs();
		File tnodeFile = new File(chanDir, tnode);
		WriteWholeFile(tnodeFile, value);
		return "OK";
	}
	
	public String doVerbBoot() throws IOException {
		String z = "BOOTING: ";
		z += " # " + doVerbCreate("777", "101", "first", "nobody");
		z += " # " + doVerbCreate("777", "102", "second", "nobody");
		z += " # " + doVerbCreate("777", "103", "third", "nobody");
		z += " # " + doVerbCreate("888", "104", "fourth", "nobody");
		z += " # " + doVerbCreate("888", "105", "fifth", "nobody");
		return z + "\n\n... BOOTED.";
	}
}

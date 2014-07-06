package yak.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import yak.etc.BaseServer;
import yak.etc.Bytes;
import yak.etc.DH;
import yak.etc.Hash;
import yak.server.Proto;

public class AppServer extends BaseServer {

	public static final int DEFAULT_PORT = 30331;
	private String appMagicWord;
	private String storagePath;
	private FileIO fileIO;
	private Progresser progresser;
	private Proto.Persona persona;

	private DH mySec = null;

	public AppServer(
			int port, String appMagicWord, String storagePath,
			FileIO fileIO, Logger logger, Progresser progresser) {
		super(port, logger);
		this.appMagicWord = appMagicWord;
		this.storagePath = storagePath;
		this.fileIO = fileIO;
		this.progresser = progresser;
		log.log(2, "Hello, this is AppServer on port=%d with magic=%s", DEFAULT_PORT, appMagicWord);
		log.log(2, "Constructed storagePath=%s", storagePath);
	}

	private String action() {
		return "/" + appMagicWord;
	}

	public Response handleRequest(Request req) {
		return new Handlers(req).response;
	}
	
	class Handlers {
		Response response;
		Request req;

		public Handlers(Request req) {
			this.req = req;
			log.log(2, "Req %s", req);
			log.log(2, "AppServer handleRequest path= %s query= %s", Show(req.path), Show(req.query));

			if (req.path.length == 1 && req.path[0].equals("favicon.ico")) {
				response = new Response("! No favicon.ico here.", 404, "text/plain");
				return;
			}
			if (!(req.path[0].equals(appMagicWord))) {
				throw Bad("Bad Magic Word: %s", Show(req.path));
			}

			String verb = req.query.get("verb");

			// Strange hack.  Is it used?  Unbundle the query "query" and add its parts.
			String query = req.query.get("query");
			if (query != null) {
				Request.ParseQueryPiecesToMap(query, req.query);
			}

			String desiredPersonaName = req.query.get("persona");
			if (desiredPersonaName != null && !desiredPersonaName.equals("")) {
				if (persona == null || persona.name != desiredPersonaName) {
					loadPersona(desiredPersonaName);
				}
			}

			Ht z = null;
			try {
				if (verb == null || verb.equals("") || verb.equals("Top")) {
					z = doTop();
				} else if (verb.equals("Persona")) {
					z = doVerbPersona(req.query);
				} else if (verb.equals("MakePersona")) {
					z = doVerbMakePersona(req.query);
				} else if (verb.equals("ViewFriend")) {
					z = doVerbViewFriend(req.query);
				} else if (verb.equals("PostToFriend")) {
					z = doVerbPostToFriend(req.query);
				} else if (verb.equals("Rendez")) {
					z = doVerbRendez(req.query);
				} else if (verb.equals("Rendez2")) {
					z = doVerbRendez2(req.query);
				} else if (verb.equals("Rendez3")) {
					z = doVerbRendez3(req.query);
				} else {
					throw new Exception("bad Verb: " + verb);
				}
			} catch (Exception e) {
				String trace = GetStackTrace(e);
				e.printStackTrace();
				response = new Response("!\r\nERROR:\r\n" + trace, 200,
						"text/plain");
				return;
			}

			z.addTag("hr", null);
			addBreadcrumbs(z, req);
			response = new Response(z.toString(), 200, "text/html");
		}
		
		private void addBreadcrumbs(Ht z, Request req) {
			z.add(CRUMB);
			z.add(CRUMB);
			z.add(CRUMB);
			z.add(makeAppLink("TOP", "Top"));

			String pers = req.query.get("persona");
			if (pers != null && pers.length() > 0) {
				z.add(CRUMB);
				z.add(makeAppLink(pers, "Persona", "persona", pers));

				String fname = req.query.get("fname");
				if (fname != null && fname.length() > 0) {
					z.add(CRUMB);
					z.add(makeAppLink(fname, "ViewFriend", "persona", pers, "fname", fname));
				}
			}
		}

		private Ht doTop() {
			log.log(1, "LISTING FILES");
			String[] files = fileIO.listFiles();
			Pattern p = Pattern.compile("([a-z][a-z0-9]*)\\.[Pp][Pp][Bb]");
			ArrayList<String> personaList = new ArrayList<String>();
			
			for (String f : files) {
				log.log(2, "File: %s", f);
				Matcher m = p.matcher(f);
				if (m.matches()) {
					log.log(2, "Matched: %s", m.group(1));
					personaList.add(m.group(1));
				}
			}
			
			// Convert to array personae, and sort.
			String[] personae = new String[personaList.size()];
			personaList.toArray(personae);
			Arrays.sort(personae);

			Ht z = new Ht();
			if (personae.length == 0) {
				z.add("No personas have been created.");
			} else {
				z.add("Pick which persona to use:");
				Ht choices = new Ht();
				for (String name : personae) {
					choices.addTag("li", null, makeAppLink(name, "Persona", "persona", name));
				}
				z.addTag("ul", null, choices);
			}
			z.addTag("hr", null);

			z.add("Make a new persona named:");
			z.addTag("input", strings("type", "text", "name", "name"));
			z.addTag("input", strings("type", "hidden", "name", "verb", "value", "MakePersona"));
			z.addTag("input", strings("type", "submit"));

			return Ht.tag(null, "form", strings("method", "GET", "action", action()), z);
		}

		private Ht doVerbMakePersona(HashMap<String, String> q) {
			String name = q.get("name");

			DH sec = DH.RandomKey();
			persona = new Proto.Persona();
			persona.name = name;
			persona.dhsec = sec.toString();
			persona.dhpub = sec.publicKey().toString();
			persona.hash = new Hash(persona.dhpub).asShortString();
			savePersona();

			Ht z = new Ht(Fmt("Saved Persona '%s'.", name));
			z.addTag("p", null);
			z.add(makeAppLink("View Persona: " + name, "Persona", "persona", name));
			return z;
		}

		private Ht doVerbPersona(HashMap<String, String> q) {
			Ht choices = new Ht();
			choices.addTag("li", null, makeAppLink("Add a friend (Peering Ceremony)", "Rendez"));

			for (Proto.Room r : persona.room) {
				choices.addTag("li", null, makeAppLink("Your room: " + r.name, "ViewRoom", "rname", r.name + "@" + persona.name));
			}
			for (Proto.Friend f : persona.friend) {
				choices.addTag("li", null, makeAppLink("Your friend: " + f.name, "ViewFriend", "fname", f.name));

				for (Proto.Room r : f.room) {
					choices.addTag("li", null, makeAppLink("Your friend's room: " + r.name + "@" + f.name, "ShowRoom", "rname", r.name + "@" + f.name));
				}
			}
			Ht z = new Ht();
			z.add("Using your persona: " + persona.name);
			z.addTag("ul", null, choices);
			z.addTag("hr", null);
			z.add("Persona Protobuf: %s", persona).addTag("hr", null);

			return z;
		}

		private String mutual(Proto.Friend f) {
			Must(f.dhpub != null, "Null dhpub for friend: %s", f.name);
			Must(f.dhpub.length() > 0, "Empty dhpub for friend: %s", f.name);
			if (f.dhmut == null || f.dhmut.length() == 0) {
				f.dhmut = mySec.mutualKey(new DH(f.dhpub)).toString();
			}
			return f.dhmut;
		}

		private String friendChannelId(Proto.Friend f) {
			String z = new Hash(mutual(f), "FriendChannelId").asMediumString();
			log.log(2, "friendChannelId: me=%s friend=%s chanId=%s", persona.name, f.name, z);
			return z;
		}

		private Hash friendChannelKey(Proto.Friend f) {
			Hash z = new Hash(mutual(f), "FriendChannelKey");
			log.log(2, "friendChannelKey: me=%s friend=%s chanKey=%s", persona.name, f.name, z);
			return z;
		}

		private void postTextToFriendChannel(Proto.Friend f, String message) throws IOException {
			Proto.TextMessage msg = new Proto.TextMessage();
			msg.text = Fmt("(From %s) %s", persona.name, message);
			Bytes b = msg.pickle();

			String chanId = friendChannelId(f);
			Hash chanKey = friendChannelKey(f);

			Bytes encrypted = chanKey.encryptBytes(b);
			log.log(2, "Plain message = %s", HexEncode(b));
			log.log(2, "Encrypted message = %s", HexEncode(encrypted));

			Date now = new Date();
			String tnode = "" + now.getTime();
			String raw = UseStore("Create", "c", chanId, "t", tnode, "v", HexEncode(encrypted));
			if (raw != null && raw.length() > 0 && raw.charAt(0) == '!') {
				throw Bad("postMessageToFriendChannel ERROR: %s", raw);
			}
		}

		private Ht doVerbPostToFriend(HashMap<String,String> q) throws IOException {
			String fname = q.get("fname");
			String data = q.get("data");

			Must(fname != null, "doVerbViewFriend: Missing fname param");
			Must(IsDecent(fname), "doVerbViewFriend: Bad fname param: %s", fname);

			Proto.Friend f = findFriend(fname);
			Must(f != null, "doVerbViewFriend: Cannot find friend %s", fname);

			postTextToFriendChannel(f, data);

			Ht page = new Ht();
			page.add("Posted message.");
			page.addTag("p", null);
			page.add(makeAppLink("Top", "Top"));
			return page;
		}

		private Ht doVerbViewFriend(HashMap<String,String> q) throws IOException {
			String fname = q.get("fname");
			Must(fname != null, "doVerbViewFriend: Missing fname param");
			Must(IsDecent(fname), "doVerbViewFriend: Bad fname param: %s", fname);

			Proto.Friend f = findFriend(fname);
			Must(f != null, "doVerbViewFriend: Cannot find friend %s", fname);

			String chanId = friendChannelId(f);
			Hash chanKey = friendChannelKey(f);

			Ht page = listChannel(Fmt("Friend '%s'", fname), chanId, chanKey);
			page.addTag("hr", null);

			Ht formGuts = new Ht();
			formGuts.addTag("textarea", strings("name", "data"));
			formGuts.addTag("br", null);
			formGuts.addTag("input", strings("type", "submit"));
			formGuts.addTag("input", strings("type", "hidden", "name", "fname", "value", fname));
			formGuts.addTag("input", strings("type", "hidden", "name", "verb", "value", "PostToFriend"));

			page.addTag("form", strings("method", "POST", "action", action()), formGuts);
			return page;
		}
		
		private Ht listChannel(String title, String channel, Hash chanKey) throws IOException {
			String[] tnodes = UseStore("List", "c", channel).split("\n");
			Arrays.sort(tnodes);

			log.log(2, "listChannel: title=%s, id=%s, tnodes= %s", title, channel, Show(tnodes));

			Ht items = new Ht();
			for (String t : tnodes) {
				if (!(IsAlphaNum(t))) {
					throw Bad("listed tnode not alphanum: %s", Show(tnodes));
				}

				String raw = UseStore("Fetch", "c", channel, "t", t);
				log.log(2, "raw=%s", CurlyEncode(raw));
				Bytes b = HexDecodeIgnoringJunk(raw);
				log.log(2, "Encrypted=%s", HexEncode(b));
				Bytes plain = chanKey.decryptBytes(b);
				String plain_hex = HexEncode(plain);
				log.log(2, "Plain=%s", plain_hex);
				Proto pb = Proto.Unpickle(plain);

				long millis = Long.parseLong(t);
				Date date = new Date(millis);
				
				String display = Fmt("[%s] %s [%s=%d]: ", millis, date, pb.getClass().getName(), pb.classId());
				switch (pb.classId()) {
				case Proto.IdTextMessage: {
					Proto.TextMessage tm = (Proto.TextMessage) pb;
					display += Fmt("(%d) %s", tm.direction, tm.text);
				}
				break;
				case Proto.IdAutoMessage: {
					Proto.AutoMessage tm = (Proto.AutoMessage) pb;
					display += Fmt("(%d) [kind=%d] %s", tm.direction, tm.kind, tm.text);
				}
				break;
				}
				display += " ---- " + pb;
				display += " ---- " + plain_hex;

				items.addTag("li", null, display);
			}

			Ht page = new Ht();
			page.add(Fmt("Channel: %s", title));
			page.addTag("p", null);
			page.addTag("ul", null, items);
			return page;
		}

		/** doVerbRendez tells our peer code and requests peer code of future pal. */
		private Ht doVerbRendez(HashMap<String,String> q) throws IOException {
			String myNewCode = Integer.toString(DH.randomInt(899) + 100);  // Choose from 100..999
			Ht body = new Ht();
			body.add("Your code is " + myNewCode);
			Ht.tag(body, "p", null);
			body.add("Enter new friend's code:");
			Ht.tag(body, "br", null);
			Ht.tag(body, "input", strings("type", "text", "name", "you"));
			Ht.tag(body, "br", null);

			Ht.tag(body, "input", strings("type", "hidden", "name", "me", "value", myNewCode));
			Ht.tag(body, "input", strings("type", "hidden", "name", "verb", "value", "Rendez2"));
			Ht.tag(body, "input", strings("type", "submit"));
			return Ht.tag(null, "form", strings(
					"method", "GET",
					"action", action()), body);
		}

		/** doVerbRendez2 does the DH Public Key Exchange. */
		private Ht doVerbRendez2(HashMap<String,String> q) throws IOException {
			Proto.DhRequest myDhR = new Proto.DhRequest();
			myDhR.name = persona.name;
			myDhR.dhpub = persona.dhpub;
			Bytes myBytes = myDhR.pickle();
			String mine = DecentlyEncode(myBytes);

			String received = UseStore("Rendez", "me", q.get("me"), "you", q.get("you"), "v", mine);
			if (received==null || received.length()==0 || received.charAt(0)=='!') {
				return new Ht("Peering failed.  Go back and try again.");
			}
			
			Proto.DhRequest theirDhR = Proto.UnpickleDhRequest(DecentlyDecode(received));
			
			Proto.Friend oldFriend = findFriend(theirDhR.name);
			if (oldFriend != null) {
				// TODO: handle ambiguous names.
				throw Bad("Already have a friend named %s", theirDhR.name);
			}
			DH theirDH = new DH(theirDhR.dhpub);
			DH mutualDH = mySec.mutualKey(theirDH);
			String check = new Hash(mutualDH.toString()).asShortString();

			Ht z = new Ht(Fmt("Confirm peering with name '%s'.", theirDhR.name));
			z.addTag("p", null);
			z.add(Fmt("To be certain whom you peered with, make sure that you BOTH got checksum '%s'.", check));
			z.addTag("p", null);

			Ht inputs = new Ht("");
			inputs.addTag("input", strings("type", "hidden", "name", "name", "value", theirDhR.name));
			inputs.addTag("input", strings("type", "hidden", "name", "pub", "value", theirDhR.dhpub));
			Ht.tag(inputs, "input", strings("type", "hidden", "name", "verb", "value", "Rendez3"));
			Ht.tag(inputs, "input", strings("type", "submit", "name", "submit", "value", "Confirm " + check));

			z.addTag("form", strings("method", "GET", "action", action()), inputs);
			z.addTag("hr", null);
			z.add("My DH PUB: %s", myDhR.dhpub).addTag("br", null);
			z.add("Their DH PUB: %s", theirDhR.name).addTag("br", null);
			z.add("Mutual DH: %s", mutualDH).addTag("br", null);
			z.add("My Persona: %s", persona).addTag("br", null);
			return z;
		}

		/** doVerbRendez3 actually saves the confirmed friend. */
		private Ht doVerbRendez3(HashMap<String,String> q) throws IOException {
			String theirName = q.get("name");
			String theirPub = q.get("pub");
			Proto.Friend f = findFriend(theirName);
			if (f != null) {
				// TODO: handle ambiguous names.
				Bad("Already have a friend named %s", theirName);
			}
			f = new Proto.Friend();
			f.name = theirName;
			f.dhpub = theirPub;
			f.dhmut = mutual(f);
			f.alias = "";
			f.hash = new Hash(theirPub).asShortString();
			persona.friend.add(f);
			savePersona();
			postTextToFriendChannel(f,
					Fmt("Friend Channel Created (by '%s' with '%s').", persona.name, f.name));

			Ht page = new Ht();
			page.add("Saved new friend: %s", theirName);
			page.addTag("p", null);
			page.add(makeAppLink("Top", "Top"));
			return page;
		}
		private void savePersona() {
			Bytes b = persona.pickle();
			try {
				DataOutputStream dos = fileIO.openDataFileOutput(Fmt("%s.ppb", persona.name));
				dos.write(b.arr, b.off, b.len);
				dos.close();
			} catch (IOException e) {
				e.printStackTrace();
				Bad("Cannot save persona: %s", e);
			}
		}
		private void loadPersona(String name) {
			try {
				DataInputStream dis = fileIO.openDataFileInput(Fmt("%s.ppb", name));
				int avail = dis.available();
				byte[] b = new byte[avail];
				dis.readFully(b);
				dis.close();
				persona = Proto.UnpicklePersona(new Bytes(b));
				mySec = new DH(persona.dhsec);
			} catch (IOException e) {
				e.printStackTrace();
				Bad("Cannot load persona: %s", e);
			}
		}
		private Proto.Friend findFriend(String sought) {
			// TODO: disambiguate with alias or hash
			for (Proto.Friend f : persona.friend) {
				if (f.name.equals(sought)) {
					return f;
				}
			}
			return null;
		}

		private Proto.Room findRoom(String sought) {
			String[] w = sought.split("@");
			if (w.length == 2) {
				return findRoom(w[0], w[1]);
			} else {
				throw Bad("Bad room name: %s", sought);
			}
		}
		private Proto.Room findRoom(String soughtRoom, String soughtFriend) {
			Proto.Friend f = findFriend(soughtFriend);
			if (f != null) {
				// TODO: disambiguate with alias or hash
				for (Proto.Room r : f.room) {
					if (r.name.equals(soughtRoom)) {
						return r;
					}
				}
			}
			return null;
		}

		public void setStoragePath(String storagePath) {
			AppServer.this.storagePath = storagePath;
		}

		public String UseStore(String verb, String ...kvkv) throws IOException {
			if (!storagePath.startsWith("http://")) {
				Bad("Bad StoragePath in AppServer: %s", CurlyEncode(storagePath));
			}
			String url = Fmt("%s?f=%s", storagePath, verb);
			for (int i = 0; i < kvkv.length; i+=2) {
				url += Fmt("&%s=%s", kvkv[i], UrlEncode(kvkv[i+1]));
			}
			log.log(2, "UseStore: %s", url);
			return FetchUrlText(url);
		}

		public Ht makeAppLink(String text, String verb, String ...kvkv) {
			if (persona != null) {
				boolean setPersona = true;
				MustNotBeNull(persona.name);
				// Make sure we're not overriding another 'persona' key.
				for (int i = 0; i < kvkv.length; i += 2) {
					if (kvkv[i].equals("persona")) {
						setPersona = false;
					}
				}
				if (setPersona) {
					kvkv = PushBack(kvkv, "persona", persona.name);
				}
			}
			String url = makeUrl(action(), PushBack(kvkv, "verb", verb));
			log.log(2, "makeAppLink: %s", url);
			return Ht.tag(null, "a", strings("href", url), text);
		}
	}
	
	public static final Ht nbsp = Ht.entity("nbsp");
	public static final String CRUMB = " ▶ ";
}

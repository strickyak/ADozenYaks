package yak.server;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import yak.etc.Json;
import yak.etc.Yak;

public class Profile extends Yak {

	public static class Item extends Yak {
		String name;

		public Item(String name) {
			this.name = name;
		}

		public Item(Json.Parser j) {
			unmarshal(j);
		}

		void unmarshalField(Json.Parser j, String key) {
			if (j.str == "name") {
				this.name = j.takeStringValue();
			} else {
				Bad("json key %s", j.str);
			}
		}

		final void unmarshal(Json.Parser j) {
			Must(j.token == '{', j.token);
			j.advance();
			while (j.token != '}') {
				Must(j.token == 's', j.token);
				String key = j.str;
				j.advance();
				unmarshalField(j, key);
			}
			j.advance();
		}
	}

	public static class Persona extends Item {
		public Self self;
		public HashMap<String, Friend> friends = new HashMap<String, Friend>();

		public Persona(String name) {
			super(name);
		}

		public Persona(Json.Parser j) {
			super(j);
		}

		@Override
		void unmarshalField(Json.Parser j, String key) {
			if (key == "self") {
				self = new Self(j);
			} else if (key == "friends") {
				Must(j.token == '[', j.token);
				j.advance();
				while (j.token != ']') {
					Friend friend = new Friend(j);
					friends.put(friend.name, friend);
				}
			} else {
				super.unmarshal(j);
			}
		}
	}

	public static class Friend extends Item {
		String dhpub;
		String dhid;
		String dhmut;
		String hub; // TODO: hubs, plural.

		public HashMap<String, Room> rooms = new HashMap<String, Room>();

		public Friend(Json.Parser j) {
			super(j);
		}

		@Override
		void unmarshalField(Json.Parser j, String key) {

			if (j.str == "dhpub") {
				this.dhpub = j.takeStringValue();
			} else if (j.str == "dhid") {
				this.dhid = j.takeStringValue();
			} else if (j.str == "dhmut") {
				this.dhmut = j.takeStringValue();
			} else if (j.str == "hub") {
				this.hub = j.takeStringValue();
			} else if (key == "rooms") {
				Must(j.token == '[', j.token);
				j.advance();
				while (j.token != ']') {
					Room room = new Room(j);
					rooms.put(room.name, room);
				}
			} else {
				super.unmarshalField(j, key);
			}
		}

	}

	public static class Self extends Friend {
		String dhsec;

		public Self(Json.Parser js) {
			super(js);
			unmarshal(js);
		}

		@Override
		void unmarshalField(Json.Parser j, String key) {
			if (key == "dhsec") {
				this.dhsec = j.takeStringValue();
			} else {
				super.unmarshalField(j, key);
			}
		}
	}

	public static class Room extends Item {
		public Room(Json.Parser j) {
			super(j);
			unmarshal(j);
		}
		@Override
		void unmarshalField(Json.Parser j, String key) {
			if (key == "TODO") {
				// TODO members.
			} else {
				super.unmarshalField(j, key);
			}
		}
	}

	public static void main(String[] a) {
		String filename = a[0];
		try {
			String s = ReadWholeFile(new File(filename));
			Json.Parser json = new Json.Parser(s);
			Persona prof = new Persona(json);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

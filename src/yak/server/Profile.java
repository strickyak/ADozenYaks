package yak.server;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
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
			if (key == "name") {
				this.name = j.takeStringValue();
			} else {
				Bad("json key %s", key);
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

	public static class Member extends Item {
		String dhpub;
		String hub; // TODO: hubs, plural.

		public Member(Json.Parser j) {
			super(j);
		}

		@Override
		void unmarshalField(Json.Parser j, String key) {

			if (j.str == "dhpub") {
				this.dhpub = j.takeStringValue();
			} else if (j.str == "hub") {
				this.hub = j.takeStringValue();
			} else {
				super.unmarshalField(j, key);
			}
		}

	}

	public static class Friend extends Member {
		String dhmut;

		public HashMap<String, Room> rooms = new HashMap<String, Room>();

		public Friend(Json.Parser j) {
			super(j);
		}

		@Override
		void unmarshalField(Json.Parser j, String key) {
			if (j.str == "dhmut") {
				this.dhmut = j.takeStringValue();
			} else if (key == "rooms") {
				Must(j.token == '[', j.token);
				j.advance();
				while (j.token != ']') {
					Room room = new Room(j);
					if (rooms.containsKey(room.name)) {
						throw Bad("Friend <%s> already contains room <%s>",
								name, room.name);
					}
					rooms.put(room.name, room);
				}
			} else {
				super.unmarshalField(j, key);
			}
		}

	}

	public static class Self extends Friend {
		String dhsec;
		public HashMap<String, Friend> friends = new HashMap<String, Friend>();

		public Self(Json.Parser js) {
			super(js);
		}

		@Override
		void unmarshalField(Json.Parser j, String key) {
			if (key == "dhsec") {
				this.dhsec = j.takeStringValue();
			} else if (key == "friends") {
				Must(j.token == '[', j.token);
				j.advance();
				while (j.token != ']') {
					Friend friend = new Friend(j);
					if (friends.containsKey(friend.name)) {
						throw Bad("Already contains friend <%s>", friend.name);
					}
					friends.put(friend.name, friend);
				}
			} else {
				super.unmarshalField(j, key);
			}
		}
	}

	public static class Room extends Item {
		HashMap<String, Member> members;

		public Room(Json.Parser j) {
			super(j);
		}

		@Override
		void unmarshalField(Json.Parser j, String key) {
			if (key == "members") {
				Must(j.token == '[', j.token);
				j.advance();
				while (j.token != ']') {
					Member member = new Member(j);
					if (members.containsKey(member.name)) {
						throw Bad("Already contains member <%s>", member.name);
					}
					members.put(member.name, member);
				}
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
			Self self = new Self(json);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

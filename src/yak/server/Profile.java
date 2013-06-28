package yak.server;

import java.io.File;
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
			if (key.equals("_")) {
				;  // Ignore the "_" keys.
			} else if (key.equals("name")) {
				this.name = j.takeStringValue();
			} else {
				throw Bad("json key (%s) for %s", key, this.getClass().getName());
			}
		}

		final void unmarshal(Json.Parser j) {
			Must(j.token == '{', j.token);
			j.advance();
			while (j.token != '}') {
				Must(j.token == 's', j.token);
				String key = j.str;
				j.advance();
				Say("BEGIN { unmarshalField: (%s) <= %s", key, this);
				unmarshalField(j, key);
				Say("} END unmarshalField: (%s) => %s", key, this);
			}
			j.advance();
		}
		
		String innerJson() {
			return fmt(" \"_\": %s,  \"name\": %s, ", Json.Quote(this.getClass().getName()), Json.Quote(name));
		}
		@Override
		public String toString() {
			return fmt("{ %s }\n", innerJson());
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
			if (key.equals("dhpub")) {
				this.dhpub = j.takeStringValue();
			} else if (key.equals("hub")) {
				this.hub = j.takeStringValue();
			} else {
				super.unmarshalField(j, key);
			}
		}
		@Override
		String innerJson() {
			return super.innerJson() + fmt(
					" \"dhpub\": %s, \"hub\": %s, ",
					Json.Quote(dhpub),
					Json.Quote(hub));
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
			if (j.str.equals("dhmut")) {
				this.dhmut = j.takeStringValue();
			} else if (key.equals("rooms")) {
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
		@Override
		String innerJson() {
			return super.innerJson() + fmt(
					" \"dhmut\": %s, \"rooms\": %s, ",
					Json.Quote(dhmut),
					Json.Show(rooms));
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
			if (key.equals("dhsec")) {
				this.dhsec = j.takeStringValue();
			} else if (key.equals("friends")) {
				Must(j.token == '[', j.token);
				j.advance();
				while (j.token != ']') {
					Friend friend = new Friend(j);
					Say("friend = %s", friend);
					Say("friends = %s", Json.Show(friends));
					if (friends.containsKey(friend.name)) {
						throw Bad("Already contains friend <%s>", friend.name);
					}
					friends.put(friend.name, friend);
				}
			} else {
				super.unmarshalField(j, key);
			}
		}
		@Override
		String innerJson() {
			return super.innerJson() + fmt(
					" \"dhsec\": %s, \"friends\": %s, ",
					Json.Quote(dhsec),
					Json.Show(friends));
		}
	}

	public static class Room extends Item {
		HashMap<String, Member> members;

		public Room(Json.Parser j) {
			super(j);
		}

		@Override
		void unmarshalField(Json.Parser j, String key) {
			if (key.equals("members")) {
				Must(j.token == '[', j.token);
				j.advance();
				while (j.token != ']') {
					Member member = new Member(j);
					if (members.containsKey(member.name)) {
						throw Bad("Room (%s) already contains member (%s)", name, member.name);
					}
					members.put(member.name, member);
				}
			} else {
				super.unmarshalField(j, key);
			}
		}
		@Override
		String innerJson() {
			return super.innerJson() + fmt(
					" \"members\": %s, ",
					Json.Show(members));
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

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
			return Fmt(" \"_\": %s,  \"name\": %s, ", Json.Quote(this.getClass().getName()), Json.Quote(name));
		}
		@Override
		public String toString() {
			return Fmt("{ %s }\n", innerJson());
		}
	}

	public static class Member extends Item {
		String dhpub;
		String hub; // TODO: hubs, plural.

		public Member(Json.Parser j) {
			super(j);
		}

		public Member(String name) {
			super(name);
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
			return super.innerJson() + Fmt(
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

		public Friend(String name) {
			super(name);
		}

		@Override
		void unmarshalField(Json.Parser j, String key) {
			if (j.str.equals("dhmut")) {
				this.dhmut = j.takeStringValue();
			} else if (key.equals("rooms")) {
				unmarshalRoomList(j, rooms);
			} else {
				super.unmarshalField(j, key);
			}
		}

		@Override
		String innerJson() {
			return super.innerJson() + Fmt(
					" \"dhmut\": %s, \"rooms\": %s, ",
					Json.Quote(dhmut),
					Json.Show(rooms));
		}
	}

	public static class Self extends Friend {
		String dhsec;
		public HashMap<String, Friend> friends = new HashMap<String, Friend>();

		public Self(String name) {
			super(name);
		}
		public Self(Json.Parser js) {
			super(js);
		}

		@Override
		void unmarshalField(Json.Parser j, String key) {
			if (key.equals("dhsec")) {
				this.dhsec = j.takeStringValue();
			} else if (key.equals("friends")) {
				unmarshalFriendsList(j, friends);
			} else {
				super.unmarshalField(j, key);
			}
		}
		@Override
		String innerJson() {
			return super.innerJson() + Fmt(
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
				unmarshalMembersList(j, members);
			} else {
				super.unmarshalField(j, key);
			}
		}
		@Override
		String innerJson() {
			return super.innerJson() + Fmt(
					" \"members\": %s, ",
					Json.Show(members));
		}
	}
	
	protected static void unmarshalRoomList(Json.Parser j, HashMap<String, Room> map) {
		Must(j.token == '[', j.token);
		j.advance();
		while (j.token != ']') {
			Room room = new Room(j);
			if (map.containsKey(room.name)) {
				throw Bad("Friend already contains room <%s>",
						room.name);
			}
			map.put(room.name, room);
		}
	}

	protected static void unmarshalFriendsList(Json.Parser j, HashMap<String, Friend> map) {
		Must(j.token == '[', j.token);
		j.advance();
		while (j.token != ']') {
			Friend friend = new Friend(j);
			Say("friend = %s", friend);
			Say("friends = %s", Json.Show(map));
			if (map.containsKey(friend.name)) {
				throw Bad("Already contains friend <%s>", friend.name);
			}
			map.put(friend.name, friend);
		}
	}

	protected static void unmarshalMembersList(Json.Parser j, HashMap<String, Member> map) {
		Must(j.token == '[', j.token);
		j.advance();
		while (j.token != ']') {
			Member member = new Member(j);
			if (map.containsKey(member.name)) {
				throw Bad("Room already contains member (%s)", member.name);
			}
			map.put(member.name, member);
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

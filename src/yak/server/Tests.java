package yak.server;

import yak.etc.Bytes;
import yak.etc.Hash;
import yak.etc.Yak;
import yak.server.Proto.Friend;
import yak.server.Proto.Room;

public class Tests extends Yak {
	
	public void testVarInt() {
		Bytes b = new Bytes();
		b.appendProtoInt(1, 100);
		b.appendProtoInt(2, 10000);
		b.appendProtoInt(3, 1000000);
		b.appendProtoInt(4, -10);
		b.appendProtoInt(5, -1000000);
		System.err.print(b.showProto());
		
		assertEquals(1 << 3, b.popVarInt());
		assertEquals(b.popVarInt(), 100);
		
		assertEquals(2 << 3, b.popVarInt());
		assertEquals(b.popVarInt(), 10000);
		
		assertEquals(3 << 3, b.popVarInt());
		assertEquals(b.popVarInt(), 1000000);
		
		assertEquals(4 << 3, b.popVarInt());
		assertEquals(b.popVarInt(), -10);
		
		assertEquals(5 << 3, b.popVarInt());
		assertEquals(b.popVarInt(), -1000000);
		
		assertEquals(0, b.len);
	}

	public void testEncrypt() {
		Hash key = new Hash("This is the key.");
		Bytes plain = new Bytes(StringToBytes("plaintext"));
		Bytes encrypted = key.encryptBytes(plain);
		Bytes decrypted = key.decryptBytes(encrypted);
		assertEquals(true, Bytes.equalsBytes(plain.asByteArray(), decrypted.asByteArray()));
	}

	public void testFriend() {
		Friend f = new Friend();
		f.name = "Eric";
		f.hash = "123abc";
		Room r1 = new Room();
		r1.name = "Lobby";
		r1.member.add("Alice");
		r1.member.add("Bob");
		f.room.add(r1);
		Room r2 = new Room();
		r2.name = "Hole";
		r2.member.add("Xander");
		r2.member.add("Yves");
		r2.member.add("Zoe");
		f.room.add(r2);
		
		Bytes b = new Bytes();
		Proto.PickleFriend(f, b);

		Yak.Say("=" + b);
		Yak.Say("=" + b.showProto());
		
		Friend g = Proto.UnpickleFriend(b);
		assertEquals("Eric", g.name);
		assertEquals("123abc", g.hash);
		
		assertEquals(2, g.room.size());
		assertEquals("Lobby", g.room.get(0).name);
		assertEquals("Hole", g.room.get(1).name);
		
		assertEquals(2, g.room.get(0).member.size());
		assertEquals(3, g.room.get(1).member.size());
		assertEquals("Alice", g.room.get(0).member.get(0));
		assertEquals("Bob", g.room.get(0).member.get(1));
		assertEquals("Zoe", g.room.get(1).member.get(2));
	}
	
	public void assertEquals(Object a, Object b) {
		if (!a.equals(b)) {
			throw new RuntimeException(Yak.Fmt("Assertion Failure: assertEquals:\na=%s\nb=%s", a, b));
		} else {
			Yak.Say("OK: " + b);
		}
	}
	
	public static void main(String[] args) {
		new Tests().testFriend();
		new Tests().testEncrypt();
		new Tests().testVarInt();
	}
}



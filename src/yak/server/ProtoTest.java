package yak.server;

import yak.etc.Bytes;
import yak.etc.Yak;
import yak.server.Proto.Friend;
import yak.server.Proto.Room;
import junit.framework.TestCase;

public class ProtoTest extends TestCase {

	public void testFriend() {
		Friend f = new Friend();
		f.name = "Eric";
		f.hash = 888;
		Room r1 = new Room();
		r1.name = "Lobby";
		r1.member.add("Alice");
		r1.member.add("Bob");
		f.room.add(r1);
		Room r2 = new Room();
		r1.name = "Hole";
		r1.member.add("Xander");
		r1.member.add("Yves");
		f.room.add(r2);
		
		Bytes b = new Bytes();
		Proto.PickleFriend(f, b);
		
		System.err.println("=" + b);
		
		Friend g = Proto.UnpickleFriend(b);
		assertEquals("Eric", g.name);
	}
	
	public static void main(String[] args) {
		new ProtoTest().testFriend();
	}
}



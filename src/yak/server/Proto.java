package yak.server;

import java.util.ArrayList;
import yak.etc.Bytes;
import yak.etc.Yak;

public class Proto extends Yak {

  public static class Friend {
    String alias;
    String contact;
    String dhmut;
    String dhpub;
    String hash;
    ArrayList<String> hub = new ArrayList<String>();
    String name;
    String remark;
    ArrayList<Room> room = new ArrayList<Room>();
  }
  public static class Message {
    String action;
    String body;
    int direction;
  }
  public static class Persona extends Friend {
    String alias;
    String contact;
    String dhmut;
    String dhpub;
    String dhsec;
    ArrayList<Friend> friend = new ArrayList<Friend>();
    String hash;
    ArrayList<String> hub = new ArrayList<String>();
    String name;
    String remark;
    ArrayList<Room> room = new ArrayList<Room>();
  }
  public static class Room {
    ArrayList<String> member = new ArrayList<String>();
    String name;
    String title;
  }
  public static void PickleFriend (Friend p, Bytes b) {
    if (p.name != null) b.appendProtoString (1, p.name);
    if (p.hash != null) b.appendProtoString (2, p.hash);
    if (p.alias != null) b.appendProtoString (3, p.alias);
    if (p.dhpub != null) b.appendProtoString (4, p.dhpub);
      for (int i = 0; i < p.hub.size(); i++) {
    b.appendProtoString (5, p.hub.get(i));
      } // next i
    if (p.contact != null) b.appendProtoString (6, p.contact);
    if (p.remark != null) b.appendProtoString (7, p.remark);
      for (int i = 0; i < p.room.size(); i++) {
    if (p.room.get(i) != null) {
      Bytes b2 = new Bytes();
      PickleRoom (p.room.get(i), b2);
	  b.appendProtoBytes (8, b2);
    }
      } // next i
    if (p.dhmut != null) b.appendProtoString (11, p.dhmut);
  }
  public static void PickleMessage (Message p, Bytes b) {
    b.appendProtoInt (1, p.direction);
    if (p.action != null) b.appendProtoString (2, p.action);
    if (p.body != null) b.appendProtoString (3, p.body);
  }
  public static void PicklePersona (Persona p, Bytes b) {
    if (p.name != null) b.appendProtoString (1, p.name);
    if (p.hash != null) b.appendProtoString (2, p.hash);
    if (p.alias != null) b.appendProtoString (3, p.alias);
    if (p.dhpub != null) b.appendProtoString (4, p.dhpub);
      for (int i = 0; i < p.hub.size(); i++) {
    b.appendProtoString (5, p.hub.get(i));
      } // next i
    if (p.contact != null) b.appendProtoString (6, p.contact);
    if (p.remark != null) b.appendProtoString (7, p.remark);
      for (int i = 0; i < p.room.size(); i++) {
    if (p.room.get(i) != null) {
      Bytes b2 = new Bytes();
      PickleRoom (p.room.get(i), b2);
	  b.appendProtoBytes (8, b2);
    }
      } // next i
    if (p.dhsec != null) b.appendProtoString (9, p.dhsec);
      for (int i = 0; i < p.friend.size(); i++) {
    if (p.friend.get(i) != null) {
      Bytes b2 = new Bytes();
      PickleFriend (p.friend.get(i), b2);
	  b.appendProtoBytes (10, b2);
    }
      } // next i
    if (p.dhmut != null) b.appendProtoString (11, p.dhmut);
  }
  public static void PickleRoom (Room p, Bytes b) {
    if (p.name != null) b.appendProtoString (1, p.name);
    if (p.title != null) b.appendProtoString (2, p.title);
      for (int i = 0; i < p.member.size(); i++) {
    b.appendProtoString (3, p.member.get(i));
      } // next i
  }
public static Friend UnpickleFriend (Bytes b) {
  Friend z = new Friend ();
  while (b.len > 0) {
    int code = b.popVarInt();
    System.err.println(Fmt("Code %d:%d", code >> 3, code & 7));
    switch (code) {
      case (3 << 3) | 2: { z.alias = b.popVarString(); }
        break;
      case (6 << 3) | 2: { z.contact = b.popVarString(); }
        break;
      case (11 << 3) | 2: { z.dhmut = b.popVarString(); }
        break;
      case (4 << 3) | 2: { z.dhpub = b.popVarString(); }
        break;
      case (2 << 3) | 2: { z.hash = b.popVarString(); }
        break;
      case (5 << 3) | 2: { z.hub.add(b.popVarString()); }
        break;
      case (1 << 3) | 2: { z.name = b.popVarString(); }
        break;
      case (7 << 3) | 2: { z.remark = b.popVarString(); }
        break;
      case (8 << 3) | 2: {
         Bytes b2 = b.popVarBytes();
         Room p2 = UnpickleRoom (b2);
         z.room.add(p2);
      } // end case
        break;
      default:
        throw new RuntimeException("Bad tag code in Friend: " + code); 
    }  // end switch
  }  // end while
  return z;
}  // end Unpickle Friend
public static Message UnpickleMessage (Bytes b) {
  Message z = new Message ();
  while (b.len > 0) {
    int code = b.popVarInt();
    System.err.println(Fmt("Code %d:%d", code >> 3, code & 7));
    switch (code) {
      case (2 << 3) | 2: { z.action = b.popVarString(); }
        break;
      case (3 << 3) | 2: { z.body = b.popVarString(); }
        break;
      case (1 << 3) | 0: { z.direction = b.popVarInt(); }
        break;
      default:
        throw new RuntimeException("Bad tag code in Message: " + code); 
    }  // end switch
  }  // end while
  return z;
}  // end Unpickle Message
public static Persona UnpicklePersona (Bytes b) {
  Persona z = new Persona ();
  while (b.len > 0) {
    int code = b.popVarInt();
    System.err.println(Fmt("Code %d:%d", code >> 3, code & 7));
    switch (code) {
      case (3 << 3) | 2: { z.alias = b.popVarString(); }
        break;
      case (6 << 3) | 2: { z.contact = b.popVarString(); }
        break;
      case (11 << 3) | 2: { z.dhmut = b.popVarString(); }
        break;
      case (4 << 3) | 2: { z.dhpub = b.popVarString(); }
        break;
      case (9 << 3) | 2: { z.dhsec = b.popVarString(); }
        break;
      case (10 << 3) | 2: {
         Bytes b2 = b.popVarBytes();
         Friend p2 = UnpickleFriend (b2);
         z.friend.add(p2);
      } // end case
        break;
      case (2 << 3) | 2: { z.hash = b.popVarString(); }
        break;
      case (5 << 3) | 2: { z.hub.add(b.popVarString()); }
        break;
      case (1 << 3) | 2: { z.name = b.popVarString(); }
        break;
      case (7 << 3) | 2: { z.remark = b.popVarString(); }
        break;
      case (8 << 3) | 2: {
         Bytes b2 = b.popVarBytes();
         Room p2 = UnpickleRoom (b2);
         z.room.add(p2);
      } // end case
        break;
      default:
        throw new RuntimeException("Bad tag code in Persona: " + code); 
    }  // end switch
  }  // end while
  return z;
}  // end Unpickle Persona
public static Room UnpickleRoom (Bytes b) {
  Room z = new Room ();
  while (b.len > 0) {
    int code = b.popVarInt();
    System.err.println(Fmt("Code %d:%d", code >> 3, code & 7));
    switch (code) {
      case (3 << 3) | 2: { z.member.add(b.popVarString()); }
        break;
      case (1 << 3) | 2: { z.name = b.popVarString(); }
        break;
      case (2 << 3) | 2: { z.title = b.popVarString(); }
        break;
      default:
        throw new RuntimeException("Bad tag code in Room: " + code); 
    }  // end switch
  }  // end while
  return z;
}  // end Unpickle Room
}

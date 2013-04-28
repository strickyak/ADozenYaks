package yak.server;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import yak.etc.Json;
import yak.etc.Yak;

//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;

public class Profile extends Yak {

		public String pName;
		public String dhsec;
		public String hub;
		
		public Self self;
		public HashMap<String,Friend> friends = new HashMap<String, Profile.Friend>();
		public HashMap<String,Room> rooms = new HashMap<String, Profile.Room>();

		public Profile(String profileName) {
			this.pName = profileName;
		}
		
		public Profile(Json.Parser j)  {
			unmarshal(j);
		}
		
		void unmarshal(Json.Parser j)  {
			Must(j.token == '{', j.token);
			j.advance();
			while (j.token != '}') {
				Must(j.token == 's', j.token);
				if (j.str == "name") {
					this.pName = j.takeStringValue();
				} else if (j.str == "dhsec") {
					this.dhsec = j.takeStringValue();
				} else if (j.str == "hub") {
					this.hub = j.takeStringValue();
				} else if (j.str == "friends") {
					j.advance();
					Must(j.token == '[', j.token);
					j.advance();
					while (j.token != ']') {
						Friend friend = new Friend(j);
						friends.put(friend.name, friend);
					}
				} else if (j.str == "rooms") {

					j.advance();
					Must(j.token == '[', j.token);
					j.advance();
					while (j.token != ']') {
						Room room = new Room(j);
						rooms.put(room.name, room);
					}
				
				} else {
					throw Bad(j.str);
				}
		
			}
//			pName = j.getString("name");
//			self = new Self(j.getJSONObject("self"));
//			JSONArray jff = j.getJSONArray("friends");
//			final int nf = jff.length();
//			friends.clear();
//			for (int i = 0; i < nf; i++) {
//				Json.Parser jf = jff.getJSONObject(i);
//				Friend f = new Friend(jf);
//				if (friends.containsKey(f.name)) {
//					throw Bad("Already has friend named '%s'", f.name);
//				}
//				friends.put(f.name, f);
//			}
//			JSONArray jrr = j.getJSONArray("rooms");
//			final int nr = jrr.length();
//			rooms.clear();
//			for (int i = 0; i < nr; i++) {
//				Json.Parser jr = jrr.getJSONObject(i);
//				Room r = new Room(jr);
//				if (rooms.containsKey(r.name)) {
//					throw Bad("Already has room named '%s'", r.name);
//				}
//				rooms.put(r.name, r);
//			}
		}
		
		public class Item extends Yak {
			String name;
			
			public Item() {
				
			}
			public Item(Json.Parser j)  {
				unmarshal(j);
			}

			void unmarshal(Json.Parser j)  {
//				pName = j.getString("name");
			}
		}

		public class Friend extends Item {
			String dhpub;
			String dhmut;
			String[] hubs;
			
			public Friend(Json.Parser jf)  {
				super(jf);
				unmarshal(jf);
			}


			void unmarshal(Json.Parser j)  {
				//pName = j.getString("name");
			}
		}
		public class Self extends Friend {
			String dhsec;
			
			public Self(Json.Parser js)  {
				super(js);
				unmarshal(js);
			}


			void unmarshal(Json.Parser j)  {
				//pName = j.getString("name");
			}
		}
		public class Room extends Item {
			public Room(Json.Parser jr)  {
				super(jr);
				unmarshal(jr);
			}


			void unmarshal(Json.Parser j)  {
				//pName = j.getString("name");
			}
		}
		public static void main(String[] a) {
			String filename = a[0];
			try {
			String s = ReadWholeFile(new File(filename));
			Json.Parser json = new Json.Parser(s);
			Profile prof = new Profile(json);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
}

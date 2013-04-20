package yak.server;

import java.util.HashMap;

import yak.etc.Yak;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Profile extends Yak {

		public String pName;
		
		public Self self;
		public HashMap<String,Friend> friends = new HashMap<String, Profile.Friend>();
		public HashMap<String,Room> rooms = new HashMap<String, Profile.Room>();

		public Profile(String profileName) {
			this.pName = profileName;
		}
		
		public Profile(JSONObject j) throws JSONException {
			unmarshal(j);
		}
		
		void unmarshal(JSONObject j) throws JSONException {
			pName = j.getString("name");
			self = new Self(j.getJSONObject("self"));
			JSONArray jff = j.getJSONArray("friends");
			final int nf = jff.length();
			friends.clear();
			for (int i = 0; i < nf; i++) {
				JSONObject jf = jff.getJSONObject(i);
				Friend f = new Friend(jf);
				if (friends.containsKey(f.name)) {
					throw new Bad("Already has friend named '%s'", f.name);
				}
				friends.put(f.name, f);
			}
			JSONArray jrr = j.getJSONArray("rooms");
			final int nr = jrr.length();
			rooms.clear();
			for (int i = 0; i < nr; i++) {
				JSONObject jr = jrr.getJSONObject(i);
				Room r = new Room(jr);
				if (rooms.containsKey(r.name)) {
					throw new Bad("Already has room named '%s'", r.name);
				}
				rooms.put(r.name, r);
			}
		}
		
		public class Item extends Yak {
			String name;
			
			public Item() {
				
			}
			public Item(JSONObject j) throws JSONException {
				unmarshal(j);
			}

			void unmarshal(JSONObject j) throws JSONException {
				pName = j.getString("name");
			}
		}

		public class Friend extends Item {
			String dhpub;
			String dhmut;
			String[] hubs;
			
			public Friend(JSONObject jf) throws JSONException {
				super(jf);
				unmarshal(jf);
			}


			void unmarshal(JSONObject j) throws JSONException {
				//pName = j.getString("name");
			}
		}
		public class Self extends Friend {
			String dhsec;
			
			public Self(JSONObject js) throws JSONException {
				super(js);
				unmarshal(js);
			}


			void unmarshal(JSONObject j) throws JSONException {
				//pName = j.getString("name");
			}
		}
		public class Room extends Item {
			public Room(JSONObject jr) throws JSONException {
				super(jr);
				unmarshal(jr);
			}


			void unmarshal(JSONObject j) throws JSONException {
				//pName = j.getString("name");
			}
		}
}

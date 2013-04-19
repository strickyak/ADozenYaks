package yak.dozen;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import yak.etc.Yak;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout.LayoutParams;

public class Yak12Activity extends Activity {
	

	Context mainContext;
	ServerAccess access = new ServerAccess("http://192.168.8.252:9999/?");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setContentView(R.layout.activity_yak12);
		this.mainContext = this;
		
//		String[] numbers = new String[] {"One", "Two", "Three"};
//		DemoListView v = new DemoListView(mainContext, numbers);
//		setContentView(v);
		
		// setContentView(R.layout.activity_main);
		

		Intent intent = getIntent();
		Uri uri = intent.getData();
		Bundle extras = intent.getExtras();
		String path = uri == null ? "/" : uri.getPath();
		String query = uri == null ? "" : uri.getQuery();

		display(path, query, extras, savedInstanceState); 
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.yak12, menu);
		return true;
	}

	
	////////////////////////////////////////
	////////////////////////////////////////
	

	static final int NumRandomBitsPerDHKey = 1535;
	
	static final String Rfc3526Modulus1536Bits =
	  "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
      "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
      "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
      "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
      "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D" +
      "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F" +
      "83655D23DCA3AD961C62F356208552BB9ED529077096966D" +
      "670C354E4ABC9804F1746C08CA237327FFFFFFFFFFFFFFFF";



	////////////////////////////////////////
	////////////////////////////////////////
	

	private void display(String path, String query, Bundle extras,
			Bundle savedInstanceState) {
		try {
			Log.i("antti", "PATH=" + path);
			String[] words = path.split("/");
			Log.i("antti", "words.LEN=" + words.length);
			String verb = "";
			if (words.length > 1) {
				verb = words[1];
			}

			Log.i("antti", "=============== VERB =" + verb);
			if (verb.equals("list")) {
				String[] labels = extras.getString("items").split(";");
				displayList(labels);
//				 } else if (verb.equals("channel")) {
//				 displayChannel(words[2]);
//				 } else if (verb.equals("create")) {
//				 displayCreateInode();
			} else if (verb.equals("rendez")) {
				displayRendezvous(words[2]);
			} else if (verb.equals("dhdemo")) {
				displayDHDemo();
			} else if (verb.equals("web")) {
				displayWeb((String) extras.get("html"));
			} else if (verb.equals("Channel777")) {
				displayChannel777();
			} else {
				displayDefault();
			}
		} catch (Exception e) {
			e.printStackTrace();
			displayWeb(Yak.htmlEscape(e.toString()));
		}
	}

	private void displayDefault() {
		String[] numbers = new String[] {"One", "Two", "Three", "Channel", "Rendezvous", "dhdemo",
				"Channel777"};
		DemoListView v = new DemoListView(mainContext, numbers);
		setContentView(v);
	}

	private void displayWeb(String html) {
		DemoWebView v = new DemoWebView(mainContext, html);
		setContentView(v);
	}

	private void displayChannel777() throws ClientProtocolException, IOException {
		String html = access.displayChannel777();
		displayWeb(html);
	}
//		private void displayChannel(String chanKey) {
//		StringBuilder sb = new StringBuilder();
//		String[] inodes = null;
//		try {
//			inodes = this.access.fetchInodes(chanKey);
//		} catch (ClientProtocolException e) {
//			Log.i("antti", e.getMessage());
//		} catch (IOException e) {
//			Log.i("antti", e.getMessage());
//		}
//		sb.append(this.renderInodes(inodes));
//		sb.append("<p><a href=\"/create\">Create</a></p>");
//		
//		DemoWebView v = new DemoWebView(mainContext, sb.toString());
//		setContentView(v);
//	}
//	
//	private String renderInodes(String[] inodes) {
//		StringBuilder sb = new StringBuilder();
//		for (String inode : inodes) {
//			sb.append("<p>");
//			sb.append(inode);
//			sb.append("</p>");
//		}
//		return sb.toString();
//	}

//	public void displayCreateInode() {
//		final EditText ed = new EditText(this);
//
//		ed.setInputType(InputType.TYPE_CLASS_TEXT
//				| InputType.TYPE_TEXT_FLAG_MULTI_LINE
//				| InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE
//				| InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
//		
//		final LayoutParams widgetParams = new LayoutParams(
//				ViewGroup.LayoutParams.FILL_PARENT,
//				ViewGroup.LayoutParams.FILL_PARENT, 1.0f);
//		
//		ed.setLayoutParams(widgetParams);
//		ed.setTextAppearance(this, R.style.teletype);
//		ed.setBackgroundColor(Color.BLACK);
//		ed.setGravity(Gravity.TOP);
//		ed.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_INSET);
//		ed.setVerticalFadingEdgeEnabled(true);
//		ed.setVerticalScrollBarEnabled(true);
//		ed.setOnKeyListener(new OnKeyListener() {
//			public boolean onKey(View v, int keyCode, KeyEvent event) {
//				// If the event is a key-down event on the "enter"
//				// button
//				// if ((event.getAction() == KeyEvent.ACTION_DOWN)
//				// &&
//				// (keyCode == KeyEvent.KEYCODE_ENTER)) {
//				// // Perform action on key press
//				// Toast.makeText(TerseActivity.this, ed.getText(),
//				// Toast.LENGTH_SHORT).show();
//				// return true;
//				// }
//				return false;
//			}
//		});
//
//		Button btn = new Button(this);
//		btn.setText("Send");
//		btn.setOnClickListener(new OnClickListener() {
//			public void onClick(View v) {
//				
//				return;
//			}
//		});
//
//		LinearLayout linear = new LinearLayout(this);
//		linear.setOrientation(LinearLayout.VERTICAL);
//		linear.addView(btn);
//		linear.addView(ed);
//		setContentView(linear);
//	}
	
	public void displayRendezvous(String myId) {
		DemoWebView v = new DemoWebView(mainContext, "");
		v.loadUrl("file:///android_asset/redez_start.html");
		setContentView(v);
	}
	
	public void displayDHDemo() {  // DH DEMO
		BigInteger g = new BigInteger("2");
		BigInteger m = new BigInteger(Rfc3526Modulus1536Bits, 16);

		SecureRandom rand = new SecureRandom();
		BigInteger secA = new BigInteger(NumRandomBitsPerDHKey, rand);
		BigInteger secB = new BigInteger(NumRandomBitsPerDHKey, rand);
		// Each raises g to the secret key to get the public key.
		BigInteger pubA = g.modPow(secA, m);
		BigInteger pubB = g.modPow(secB, m);
		// A learns pubB; B learns pubA.
		BigInteger mutualA = pubB.modPow(secA, m);  // A can compute.
		BigInteger mutualB = pubA.modPow(secB, m);  // B can compute.
		// Those mutual keys should be equal.
		BigInteger mutualDiff = mutualA.subtract(mutualB);
		
		String html = "<html><body><ul>";
		html += "<li> secA = " + secA;
		html += "<li> secB = " + secB;
		html += "<li> pubA = " + pubA;
		html += "<li> pubB = " + pubB;
		html += "<li> mutualA = " + mutualA;
		html += "<li> mutualB = " + mutualB;
		html += "<li> mutualDiff = " + mutualDiff;
		html += "<li> len(mutual) = " + mutualA.toString().length() + " decimal digits";
		
		DemoWebView v = new DemoWebView(mainContext, html);
		setContentView(v);
	}
	
	// Provides access to the storage.
	public class ServerAccess {
		
		String baseUrl;
		
		public ServerAccess(String baseUrl) {
			this.baseUrl = baseUrl;
		}
		
//		// Fetch the inodes from storage.
//		public String[] fetchInodes(String chanKey) throws ClientProtocolException, IOException {
//			String scanUrl = this.baseUrl + "?f=&u=0&c=" + chanKey;
//			String scan = null;
//			
//			Log.i("antti", "~~~Store Scan Url: " + scanUrl);
//			scan = this.getUrl(scanUrl);
//			Log.i("antti", "~~~Store Scan Reply: " + scan);
//			
//			String[] inodeKeys = scan.split("\n");
//			int n = inodeKeys.length;
//			String[] inodes = new String[n];
//			
//			Log.i("antti", "~~~Store found " + n + " number of inode keys to fetch.");
//			for (int i = 0; i < n; i++) {
//				String fetchUrl = this.baseUrl + "?f=fetch&u=0&c=" + chanKey + "&i=" + inodeKeys[i];
//				
//				Log.i("antti", "~~~Store Fetch Url: " + fetchUrl);
//				inodes[i] = this.getUrl(fetchUrl);
//				Log.i("antti", "~~~Store Fetch Reply: " + inodes[i]);
//			}
//			
//			return inodes;
//		}
//		
//		private void createInode(String chanKey, String inode, String value, String user) throws ClientProtocolException, IOException {
//			String createUrl = this.baseUrl + "?f=create&u=0&c=" + chanKey + "&i=" + inode + "&value=" + value + "&u=" + user;
//			getUrl(createUrl);
//		}

		public String displayChannel777() throws ClientProtocolException, IOException {
			return getUrl(baseUrl + "f=chan&c=777");
		}

		private String getUrl(String url) throws ClientProtocolException, IOException {
			HttpClient httpclient = new DefaultHttpClient();
		    HttpResponse response = httpclient.execute(new HttpGet(url));
		    StatusLine statusLine = response.getStatusLine();
		    if(statusLine.getStatusCode() == HttpStatus.SC_OK){
		        ByteArrayOutputStream out = new ByteArrayOutputStream();
		        response.getEntity().writeTo(out);
		        out.close();
		        String responseString = out.toString();
		        return responseString;
		    } else {
		        //Closes the connection.
		        response.getEntity().getContent().close();
		        throw new IOException(statusLine.getReasonPhrase());
		    }
		}
	}

	private void displayList(String[] labels) {
		DemoListView v = new DemoListView(mainContext, labels);
		setContentView(v);
	}

//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.main, menu);
//		return true;
//	}

	public abstract class AListView extends ListView {

		Context context;
		String[] labels;

		public AListView(Context context, final String[] labels) {
			super(context);
			this.context = context;
			this.labels = labels;
			
			this.setAdapter(new ArrayAdapter<String>(context,
					R.layout.list_item, labels));

			this.setLayoutParams(FILL);
			this.setTextFilterEnabled(true);
			
			this.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					onClick(arg2, labels[arg2]);
				}
			});
		}
		
		protected abstract void onClick(int index, String label);
	}
	
	public class DemoListView extends AListView {

		public DemoListView(Context context, String[] labels) {
			super(context, labels);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void onClick(int index, String label) {
			if (label == "Channel") {
				startChannel("555");
			} else if (label == "dhdemo") {
				startDHDemo();
			} else if (label == "Channel777") {
				startChannel777();
			} else if (label == "Rendezvous") {
				SecureRandom random = null;
				try {
					random = SecureRandom.getInstance("SHA1PRNG");
				} catch (NoSuchAlgorithmException e) {
					Log.i("antti", e.getMessage());
				}
				int mytempid = random.nextInt();
				startRendezvous(String.valueOf(mytempid));
			} else {
				String html = "GOT {" + label + "}.";
				startWeb(html);
			}
//			DemoWebView v = new DemoWebView(context, html);
//			setContentView(v);
		}
	
	}
	
	public abstract class AWebView extends WebView {

		public AWebView(Context context, String html) {
			super(context);

			this.loadDataWithBaseURL("terse://terse",
					html, "text/html", "UTF-8", null);
			

			// this.setWebChromeClient(new WebChromeClient());
			this.getSettings().setBuiltInZoomControls(true);
			// this.getSettings().setJavaScriptEnabled(true);
			this.getSettings().setDefaultFontSize(18);
			this.getSettings().setNeedInitialFocus(true);
			this.getSettings().setSupportZoom(true);
			this.getSettings().setSaveFormData(true);
			

			this.setWebViewClient(new WebViewClient() {
				@Override
				public boolean shouldOverrideUrlLoading(WebView view,
						String url) {
					return onClickLink(url);
				}
			});
		}
		protected abstract boolean onClickLink(String url);
	}
	public class DemoWebView extends AWebView {

		public DemoWebView(Context context, String html) {
			super(context, html);
		}

		protected boolean onClickLink(String url) {
			URI uri = URI.create("" + url);
			String path = uri.getPath();
			String query = uri.getQuery();
			
			startMain(path, query);
			
			return true;
		}	
	}

	void startList(String[] labels) {
		String z = "";
		for (String s : labels) {
			z = z + labels + ";";
		}
		startMain("/list", null, "items", z);
	}
	void startWeb(String html) {
		startMain("/web", null, "html", html);
	}
	
	void startChannel(String chanKey) {
		startMain("/channel/" + chanKey, null);
	}
	
	void startRendezvous(String myId) {
		startMain("/rendez/" + myId, null);
	}
	
	void startDHDemo() {
		startMain("/dhdemo", null);
	}
	
	void startChannel777() {
		startMain("/Channel777", null);
	}

	void startMain(String actPath, String actQuery, String... extrasKV) {
		Uri uri = new Uri.Builder().scheme("terse").path(actPath)
				.encodedQuery(actQuery).build();
		Intent intent = new Intent("android.intent.action.MAIN", uri);
		intent.setClass(getApplicationContext(), Yak12Activity.class);
		for (int i = 0; i < extrasKV.length; i += 2) {
			intent.putExtra((String)extrasKV[i], extrasKV[i + 1]);
		}
		startActivity(intent);
	}

	LayoutParams FILL = new LayoutParams(LayoutParams.FILL_PARENT,
			LayoutParams.FILL_PARENT);
	
}

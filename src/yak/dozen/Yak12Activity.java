package yak.dozen;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import yak.etc.DH;
import yak.etc.Hash;
import yak.etc.Yak;
import yak.etc.Yak.FileIO;
import yak.etc.Yak.JavaFileIO;
import yak.server.AppServer;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout.LayoutParams;

public class Yak12Activity extends Activity {

	static AppServer server;
	static Thread serverThread;
	static String appMagic = new Hash(DH.RandomKey().toString()).toString();

	AppCaller appCaller = new AppCaller(Yak.Fmt("http://localhost:%d/%s?",
			AppServer.DEFAULT_PORT, appMagic));;
	Context yakContext = this;
	Handler yakHandler = new Handler();
	FileIO fileIO;
	
	public Yak12Activity() {
		Log.i("yak12", "###### CTOR: " + this);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i("yak12", "###### onCreate: " + this);
		super.onCreate(savedInstanceState);
		fileIO = new AndroidFileIO();

		// Start embedded App Server, if it is not yet started.
		if (serverThread == null) {
			server = new AppServer(AppServer.DEFAULT_PORT, appMagic, "TEMPORARY", fileIO);
			serverThread = new Thread(server);
			serverThread.start();
			Yak.sleepSecs(0.1);
			
			initializeStoragePath();
		}

		Intent intent = getIntent();
		Uri uri = intent.getData();
		Bundle extras = intent.getExtras();
		String path = uri == null ? "/" : uri.getPath();
		String query = uri == null ? "" : uri.getQuery();
		query = (query == null) ? "" : query;

		try {
			Log.i("antti", "..PATH=" + path);
			Log.i("antti", "...QUERY=" + query);
			String[] words = path.split("/");
			String command = "";
			if (words.length > 1) {
				command = words[1];
			}
			HashMap<String, String> queryMap = new HashMap<String, String>();
			if (query.contains("verb=")) {
				handleNewVerb(query);
			} else if (command.equals("dhdemo")) {
				handleDHDemo();
			} else if (command.equals("Channel777")) {
				handleChannel777();
			} else if (command.equals("Config")) {
				handleConfig();
			} else if (command.equals("URandom")) {
				handleURandom();
			} else if (command.equals("")) {
				handleDefault();
			} else {
				Yak.Bad("BAD LAUNCH URI: " + uri);
			}
		} catch (Exception e) {
			e.printStackTrace();
			toast(e.toString());
			displayText("handleYak12Intent CAUGHT EXCEPTION:\n\n"
					+ e.toString());
		}
	}

	public void initializeStoragePath() {
		String storagePath = "";
		try {
			storagePath = fileIO.readTextFile("config.txt");
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		server.setStoragePath(storagePath);
		
	}

	public void toast(String message) {
		Log.i("toast", message);
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.yak12, menu);
		return true;
	}

	// /////////////////////////////////////////////////

	private void handleChannel777() throws ClientProtocolException, IOException {
		appCaller.handleChannel777();
	}
	private void handleURandom() throws ClientProtocolException, IOException {
		String s = "";
		try {
			DataInputStream br = fileIO.openURandom();
			for (int i = 0; i < 32; i++) {
				s += " " + br.readLong();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			s = ex.toString();
		}
		displayText("URandom: " + s);
	}

	private void handleOther(Uri uri) throws ClientProtocolException, IOException {
		appCaller.handleOther(uri);
	}

	private void handleNewVerb(String query) throws ClientProtocolException, IOException {
		appCaller.handleNewVerb(query);
	}

	private void handleDefault() {
		displayList(new String[] { "Config", "*Rendez", "One", "Two", "Three", "Channel",
				"Rendezvous", "dhdemo", "Channel777", "Channel0", "URandom" });
	}

	private void handleConfig() {
		String text = "";
		try {
			text = fileIO.readTextFile("config.txt");
		} catch (Exception e) {
			toast(e.toString());
		}
		if (text.equals("")) {
			text = "http://";
		}
		AnEditView editor = new AnEditView(text) {
			@Override
			public void onSave(String newText) {
				fileIO.writeTextFile("config.txt", newText, false);
				server.setStoragePath(newText);
				toast("Saved Config");
				startIntent("/", null);
			}
		};
		setContentView(editor);
	}
	
	class AndroidFileIO extends Yak.FileIO {

		@Override
		public BufferedReader openTextFileInput(String filename) throws FileNotFoundException {
			FileInputStream fis = Yak12Activity.this.openFileInput(filename);
			InputStreamReader isr = new InputStreamReader(fis);
			return new BufferedReader(isr);
		}

		@Override
		public PrintWriter openTextFileOutput(String filename, boolean worldly) throws IOException {
			FileOutputStream fos = Yak12Activity.this.openFileOutput(filename,
					worldly ? (Context.MODE_WORLD_READABLE | Context.MODE_WORLD_WRITEABLE) : Context.MODE_PRIVATE);
			return new PrintWriter(new PrintStream(fos));
		}

		@Override
		public DataInputStream openDataFileInput(String filename) throws FileNotFoundException {
			FileInputStream fis = Yak12Activity.this.openFileInput(filename);
			return new DataInputStream(fis);
		}

		@Override
		public DataOutputStream openDataFileOutput(String filename) throws FileNotFoundException {
			FileOutputStream fos = Yak12Activity.this.openFileOutput(filename, Context.MODE_PRIVATE);
			return new DataOutputStream(fos);
		}
	}

	// //////////////////////////////////////////////////

	private void displayList(String[] labels) {
		AListView v = new AListView(labels);
		setContentView(v);
	}

	private void displayText(String s) {
		setContentView(new ATextView(s));
	}

	private void displayWeb(final String html) {
		Log.i("displayWeb", "Running TEXT-WEB POST");
		WebView v = new AWebView(html);
		// ATextView v = new ATextView(html);
		setContentView(v);
	}

	public void handleDHDemo() { // DH DEMO
		DH secA = DH.RandomKey();
		DH secB = DH.RandomKey();
		// Each raises g to the secret key to get the public key.
		DH pubA = secA.publicKey();
		DH pubB = secB.publicKey();
		// A learns pubB; B learns pubA.
		DH mutualA = secA.mutualKey(pubB); // A can compute.
		DH mutualB = secB.mutualKey(pubA); // B can compute.
		// Those mutual keys should be equal.
		BigInteger mutualDiff = mutualA.big.subtract(mutualB.big);

		Hash key = new Hash("mumble");
		String plain = "I wish I were an Oscar Mayer Wiener\000.";
		String encr = key.oldEncrypt(plain, 31415);
		String recover = key.oldDecrypt(encr, 31415);

		String html = "<html><body><ul>";
		html += "<li> secA = " + secA;
		html += "<li> secB = " + secB;
		html += "<li> pubA = " + pubA;
		html += "<li> pubB = " + pubB;
		html += "<li> mutualA = " + mutualA;
		html += "<li> mutualB = " + mutualB;
		html += "<li> mutualDiff = " + mutualDiff;
		html += "<li> len(mutual) = " + mutualA.toString().length()
				+ " hex digits";

		html += "<li> plain = " + plain.length() + ": "
				+ Yak.CurlyEncode(plain);
		html += "<li> encr = " + encr.length() + ": " + encr;
		html += "<li> recover = " + recover.length() + ": "
				+ Yak.CurlyEncode(recover);

		AWebView v = new AWebView(html);
		setContentView(v);
	}

	// Does HTTP GETs to the App Server.
	public class AppCaller extends Yak {

		String baseUrl;

		public AppCaller(String baseUrl) {
			this.baseUrl = baseUrl;
		}
		
		public void handleNewVerb(String query) throws ClientProtocolException,
				IOException {
			getUrlAndDisplay(baseUrl + "&" + query);
		}
		
		public void handleOther(Uri uri) throws ClientProtocolException,
				IOException {
			String q = (uri.getQuery() == null) ? "" : uri.getQuery();
			getUrlAndDisplay(baseUrl + "&" + UrlEncode(q));
		}
		
		public void handleChannel777() throws ClientProtocolException,
				IOException {
			getUrlAndDisplay(baseUrl + "verb=chan&c=777");
		}

		/** Details of getUrl in bg, and fill in view in UI Thread. */
		public void getUrlAndDisplay(final String url)
				throws ClientProtocolException, IOException {
			final AVerticalView vert = new AVerticalView();
			ATextView tv = new ATextView(Yak.CurlyEncode(url));
			tv.setTextColor(Color.WHITE);
			vert.addView(tv);
			setContentView(vert);

			new Thread() { // A background thread.
				@Override
				public void run() {
					String html = null;
					try {
						Log.i("getUrlAndDisplay", "<<< bg: " + CurlyEncode(url));
						html = getUrl(url);
					} catch (Exception e) {
						e.printStackTrace();
						html = "getUrlAndDisplay ERROR:<br>"
								+ htmlEscape(e.toString());
					}
					final String finalHtml = html;

					Log.i("getUrlAndDisplay", ">>> html: "
							+ CurlyEncode(finalHtml));

					yakHandler.post(new Runnable() {
						@Override
						public void run() {
							Log.i("Posting", CurlyEncode(finalHtml));
							vert.addView(new AWebView(finalHtml));
						}
					});
				}
			}.start();
		}

		/** Details of client HTTP GET; expecting only 200 or error. */
		private String getUrl(String url) throws ClientProtocolException,
				IOException {
			HttpClient httpclient = new DefaultHttpClient();
			Log.i("getUrl", "< " + url);
			HttpResponse response = httpclient.execute(new HttpGet(url));
			StatusLine statusLine = response.getStatusLine();
			Log.i("getUrl", "> " + statusLine.getStatusCode());
			if (statusLine.getStatusCode() == 200) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				response.getEntity().writeTo(out);
				out.close();
				String responseString = out.toString();
				Log.i("getUrl", ">> " + CurlyEncode(responseString));
				return responseString;
			} else {
				// Closes the connection.
				response.getEntity().getContent().close();
				Log.i("getUrl",
						">>BAD>> " + CurlyEncode(statusLine.getReasonPhrase()));
				throw new IOException(statusLine.getReasonPhrase());
			}
		}
	}

	public class AListView extends ListView {

		String[] labels;

		public AListView(final String[] labels) {
			super(yakContext);
			this.labels = labels;
			Log.i("AListView", "=== CTOR");

			this.setAdapter(new ArrayAdapter<String>(yakContext,
					R.layout.list_item, labels));

			this.setLayoutParams(FILL);
			this.setTextFilterEnabled(true);

			this.setOnItemClickListener(new ClickListener());
		}

		private class ClickListener implements OnItemClickListener {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int index,
					long arg3) {
				final String label = labels[index];
				
				if (label.length() > 0 && label.charAt(0) == '*') {
					startStarVerb(label.substring(1));
				} else if (label == "Channel") {
					startChannel("555");
				} else if (label == "dhdemo") {
					startDHDemo();
//				} else if (label == "Channel777") {
//					startChannel777();
//				} else if (label == "Channel0") {
//					startChannel0();
//				} else if (label == "Rendezvous") {
//					startRendezvous();
				} else {
					startIntent("/" + label, "xyz=789");
				}
			}
		}
	}

	public class AWebView extends WebView {

		@TargetApi(Build.VERSION_CODES.ECLAIR_MR1)
		public AWebView(String html) {
			super(yakContext);
			Log.i("AWebView", "=== CTOR");

			this.loadDataWithBaseURL("terse://terse", html, "text/html",
					"UTF-8", null);

			// this.setWebChromeClient(new WebChromeClient());
			this.getSettings().setBuiltInZoomControls(true);
			// this.getSettings().setJavaScriptEnabled(true);
			this.getSettings().setDefaultFontSize(18);
			this.getSettings().setNeedInitialFocus(true);
			this.getSettings().setSupportZoom(true);
			this.getSettings().setSaveFormData(true);

			this.setWebViewClient(new WebViewClient() {
				@Override
				public boolean shouldOverrideUrlLoading(WebView view, String url) {
					return onClickLink(url);
				}
			});
			
			// https://code.google.com/p/android/issues/detail?id=7189
			this.setOnTouchListener(new View.OnTouchListener() {
		           @Override
		           public boolean onTouch(View v, MotionEvent event) {
		               switch (event.getAction()) {
		                   case MotionEvent.ACTION_DOWN:
		                   case MotionEvent.ACTION_UP:
		                       if (!v.hasFocus()) {
		                           v.requestFocus();
		                       }
		                       break;
		               }
		               return false;
		           }
		       });
		}

		protected boolean onClickLink(String url) {
			URI uri = URI.create("" + url);
			String path = uri.getPath();
			String query = uri.getQuery();

			startIntent(path, query);

			return true;
		}
	}

	public class ATextView extends TextView {
		public ATextView(String text) {
			super(yakContext);
			Log.i("ATextView", yakContext.toString() + "===  CTOR: " + Yak.CurlyEncode(text));
			this.setText(text);
			this.setBackgroundColor(Color.BLACK);
			this.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
			this.setTextColor(Color.YELLOW);
		}
	}

	public abstract class AnEditView extends LinearLayout {
		public AnEditView(String text) {
			super(yakContext);
			Log.i("AnEditView", yakContext.toString() + "===  CTOR: " + Yak.CurlyEncode(text));
			
			final EditText editor = new EditText(yakContext);
			editor.setText(text);
			editor.setBackgroundColor(Color.BLACK);
			editor.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
			editor.setTextColor(Color.GREEN);
			
			final Button btn = new Button(yakContext);
			btn.setText("Save");
			btn.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					onSave(editor.getText().toString());
				}
			});

			this.setOrientation(LinearLayout.VERTICAL);
			this.addView(btn);
			this.addView(editor);
		}
			
		public abstract void onSave(String text);
	}

	public class AVerticalView extends LinearLayout {
		public AVerticalView() {
			super(yakContext);
			Log.i("VerticalView", yakContext.toString() + "=== CTOR");
			this.setOrientation(LinearLayout.VERTICAL);
		}

		@Override
		public void addView(View view) {
			Log.i("VerticalView", yakContext.toString() + "=== addView: " + view);
			super.addView(view);
		}
	}

	// Activity Starters

	void startList(String[] labels) {
		String z = "";
		for (String s : labels) {
			z = z + labels + ";";
		}
		startIntent("/list", null, "items", z);
	}

	void startStarVerb(String verb) {
		startIntent("/", "verb=" + verb);
	}

	void startChannel(String chanKey) {
		startIntent("/channel/" + chanKey, null);
	}

//	void startRendezvous() {
//		startIntent("/Rendez", null);
//	}

	void startDHDemo() {
		startIntent("/dhdemo", null);
	}

//	void startChannel777() {
//		startIntent("/Channel777", null);
//	}
//
//	void startChannel0() {
//		startIntent("/Channel0", null);
//	}

	void startIntent(String actPath, String actQuery, String... extrasKV) {
		Uri uri = new Uri.Builder().scheme("yak12").path(actPath)
				.encodedQuery(actQuery).build();
		Intent intent = new Intent("android.intent.action.MAIN", uri);
		intent.setClass(getApplicationContext(), Yak12Activity.class);
		for (int i = 0; i < extrasKV.length; i += 2) {
			intent.putExtra((String) extrasKV[i], extrasKV[i + 1]);
		}

		startActivity(intent);
	}

	// ////////////////////////////////////
	// Other Activity Events.

	protected void onStart() {
		Log.i("yak12", "###### onStart" + this);
		super.onStart();
	}

	protected void onRestart() {
		Log.i("yak12", "###### onRestart " + this);
		super.onRestart();
	}

	protected void onResume() {
		Log.i("yak12", "###### onResume " + this);
		super.onResume();
	}

	protected void onPause() {
		Log.i("yak12", "###### onPause " + this);
		super.onPause();
	}

	protected void onStop() {
		Log.i("yak12", "###### onStop " + this);
		super.onStop();
	}

	protected void onDestroy() {
		Log.i("yak12", "###### onDestroy " + this);
		super.onDestroy();
	}

	@Override
	public void setContentView(View view) {
		Log.i("yak12", "@@@@@@@ setContentView:" + view + " " + this);
		super.setContentView(view);
	}

	// //////////////////////////////
	// Constants.

	LayoutParams FILL = new LayoutParams(LayoutParams.FILL_PARENT,
			LayoutParams.FILL_PARENT);

}

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
import yak.etc.Yak.Logger;
import yak.server.AppServer;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas.VertexMode;
import android.graphics.Color;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout.LayoutParams;

public class Yak12Activity extends Activity {

	static AppServer server;
	static Thread serverThread;
	
	// A magic string that other apps on the device will not guess.
	static String appMagic = new Hash(DH.RandomKey().toString()).asMediumString();

	AppCaller appCaller = new AppCaller(Yak.Fmt("http://localhost:%d/%s?",
			AppServer.DEFAULT_PORT, appMagic));;
	Context yakContext = this;
	Handler yakHandler = new Handler();
	private AppLogger log = new AppLogger(2);
	private AppProgresser progresser = new AppProgresser();
	
	public Yak12Activity() {
		log.log(1, "###### CTOR: " + this);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log.log(1, "###### onCreate: " + this);
		super.onCreate(savedInstanceState);

		// Start embedded App Server, if it is not yet started.
		if (serverThread == null) {
			server = new EmbedAppServer(
					AppServer.DEFAULT_PORT,
					appMagic,
					"http://yak.net:30332/YakButter",
					new AndroidFileIO(),
					log,
					progresser);
			serverThread = new Thread(server);
			serverThread.start();
			Yak.sleepSecs(0.2);
		}

		Intent intent = getIntent();
		Uri uri = intent.getData();
		String query = uri == null ? "" : uri.getQuery();
		query = (query == null) ? "" : query;
		String path = uri == null ? "/" : uri.getPath();
		log.log(1, "QUERY: <%s>", query);
		log.log(1, "PATH: <%s>", path);

		try {
			if (path.equals("/@Help")) {
				setContentView(new ATextView("There is no help"));
			} else if (path.equals("/@Log")) {
				AVerticalView v = new AVerticalView();
				v.addView(new ATextView("Log:\n" + getLogAsText()));
				
				ScrollView scrollv = new ScrollView(this);
				scrollv.addView(v);
				setContentView(scrollv);
			} else {
				handle(query);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			displayText("handleYak12Intent CAUGHT EXCEPTION:\n\n"
					+ e.toString());
		}
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		// return true;
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.top_menu:
			launchIntent("");
			return true;
		case R.id.help_menu:
			launchIntent("@Help");
			return true;
		case R.id.log_menu:
			launchIntent("@Log");
			return true;
		}
		return false;
	}

	private void handle(String query) throws ClientProtocolException, IOException {
		appCaller.handle(query);
	}

	private void displayText(String s) {
		setContentView(new ATextView(s));
	}

	private void displayWeb(final String html) {
		log.log(1, "displayWeb");
		WebView v = new AWebView(html);
		setContentView(v);
	}

	// Does HTTP GETs to the App Server.
	public class AppCaller extends Yak {

		String baseUrl;

		public AppCaller(String baseUrl) {
			this.baseUrl = baseUrl;
		}
		
		public void handle(String query) throws ClientProtocolException,
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
			ATextView tv = new ATextView(url);
			tv.setTextColor(Color.GRAY);
			vert.addView(tv);
			setContentView(vert);

			new Thread() { // A background thread.
				@Override
				public void run() {
					String html = null;
					try {
						log.log(2, "<<< bg: " + url);
						html = getUrl(url);
					} catch (Exception e) {
						e.printStackTrace();
						html = "getUrlAndDisplay ERROR:<br>"
								+ htmlEscape(e.toString());
					}
					final String finalHtml = html;

					// log.log(2, ">>> html: " + CurlyEncode(finalHtml));

					yakHandler.post(new Runnable() {
						@Override
						public void run() {
							// log.log(2, CurlyEncode(finalHtml));
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
			log.log(2, "getUrl < " + url);
			HttpResponse response = httpclient.execute(new HttpGet(url));
			StatusLine statusLine = response.getStatusLine();
			log.log(2, "getUrl > " + statusLine.getStatusCode());
			if (statusLine.getStatusCode() == 200) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				response.getEntity().writeTo(out);
				out.close();
				String responseString = out.toString();
				log.log(2, "getUrl >> " + responseString);
				return responseString;
			} else {
				// Closes the connection.
				response.getEntity().getContent().close();
				log.log(2, "getUrl >>BAD>> " + statusLine.getReasonPhrase());
				throw new IOException(statusLine.getReasonPhrase());
			}
		}
	}

	public class AWebView extends WebView {

		@TargetApi(Build.VERSION_CODES.ECLAIR_MR1)
		public AWebView(String html) {
			super(yakContext);
			log.log(1, "AWebView === CTOR");

			this.loadDataWithBaseURL(
					"http://localhost:" + AppServer.DEFAULT_PORT + "/", html, "text/html",
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
			
			this.setOnCreateContextMenuListener(Yak12Activity.this);
			
		}

		protected boolean onClickLink(String url) {
			URI uri = URI.create(url);
			String query = uri.getQuery();
			launchIntent(query);
			return true;
		}
	}

	public class ATextView extends TextView {
		public ATextView(String text) {
			super(yakContext);
			// log.log(1, "ATextView ===  CTOR: " + Yak.CurlyEncode(text));
			this.setText(text);
			this.setBackgroundColor(Color.BLACK);
			this.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
			this.setTextColor(Color.YELLOW);

			this.setOnCreateContextMenuListener(Yak12Activity.this);
		}
	}

	public class AVerticalView extends LinearLayout {
		public AVerticalView() {
			super(yakContext);
			log.log(1, "VerticalView === CTOR");
			this.setOrientation(LinearLayout.VERTICAL);
			this.setOnCreateContextMenuListener(Yak12Activity.this);
		}

		@Override
		public void addView(View view) {
			log.log(1, "VerticalView === addView: " + view);
			super.addView(view);
		}
	}

	void launchIntent(String actQuery) {
		Uri uri;
		if (actQuery.startsWith("@")) {
			// @ marks a path internal to the Activity.
			uri = new Uri.Builder().scheme("yak12").path("/" + actQuery).build();
		} else {
			// Otherwise it is a query for the AppServer.
			uri = new Uri.Builder().scheme("yak12").path("/")
					.encodedQuery(actQuery).build();
		}
		Intent intent = new Intent("android.intent.action.MAIN", uri);
		intent.setClass(getApplicationContext(), Yak12Activity.class);

		startActivity(intent);
	}

	// Activity Event Hooks
	
	@Override protected void onStart() {
		log.log(1, "###### onStart" + this);
		super.onStart();
	}

	@Override protected void onRestart() {
		log.log(1, "###### onRestart " + this);
		super.onRestart();
	}

	@Override protected void onResume() {
		log.log(1, "###### onResume " + this);
		super.onResume();
	}

	@Override protected void onPause() {
		log.log(1, "###### onPause " + this);
		super.onPause();
	}

	@Override protected void onStop() {
		log.log(1, "###### onStop " + this);
		super.onStop();
	}

	@Override protected void onDestroy() {
		log.log(1, "###### onDestroy " + this);
		super.onDestroy();
	}

	@Override public void setContentView(View view) {
		log.log(1, "@@@@@@@ setContentView:" + view + " " + this);
		super.setContentView(view);
	}
	
	static class EmbedAppServer extends AppServer {
		
		public EmbedAppServer(int port, String appMagicWord,
				String storagePath, FileIO fileIO, Logger logger,
				Progresser progresser) {
			super(port, appMagicWord, storagePath, fileIO, logger, progresser);
		}
	}
	
	class AndroidFileIO extends FileIO {

		public String[] listFiles() {
			return Yak12Activity.this.fileList();
		}
		
		@Override
		public BufferedReader openTextFileInput(String filename)
				throws FileNotFoundException {
			FileInputStream x = openFileInput(filename);
			return new BufferedReader(new InputStreamReader(x));
		}

		@Override
		public PrintWriter openTextFileOutput(String filename, boolean worldly)
				throws IOException {
			Yak.Must(!worldly);  // Don't ever be public.
			FileOutputStream x = openFileOutput(filename, MODE_PRIVATE);
			return new PrintWriter(x);
		}

		@Override
		public DataInputStream openDataFileInput(String filename)
				throws FileNotFoundException {
			FileInputStream x = openFileInput(filename);
			return new DataInputStream(x);
		}

		@Override
		public DataOutputStream openDataFileOutput(String filename)
				throws FileNotFoundException {
			FileOutputStream x = openFileOutput(filename, MODE_PRIVATE);
			return new DataOutputStream(x);
		}
	}
	
	// AppLogger.
	
	static final int LOG_LEN = 200;
	static String[] logs = new String[LOG_LEN];
	static int logs_next;
	
	public void addLog(String s) {
		logs[logs_next] = s;
		++logs_next;
		logs_next %= LOG_LEN;
	}
	
	public String[] getLog() {
		String[] z = new String[LOG_LEN];
		for (int i = 0; i < LOG_LEN; i++) {
			int j = (logs_next - i - 1 + 2*LOG_LEN) % LOG_LEN;
			z[i] = logs[j];
		}
		return z;
	}
	
	public String getLogAsText() {
		String[] a = getLog();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < a.length; i++) {
			if (a[i] != null) {
				sb.append(Yak.Fmt("\n\n%d.   ", i));
				sb.append(a[i]);
			}
		}
		return sb.toString();
	}
	
	public class AppLogger extends Logger {
		public AppLogger(int verbosity) {
			this.verbosity = verbosity;
		}
		@Override
		public void log(int level, String s, Object...args) {
			String z = tryFmt(s, args);
			Log.i("12yak_" + level, z);
			if (level <= this.verbosity) {
				addLog(z);
			}
		}
	}
	
	public class AppProgresser extends Yak.Progresser {
		public void progress(float percent, String s, Object...args) {
			String msg = Fmt("[%6.1f] ", percent) + Fmt(s, args);
			log.log(1, msg);
			setContentView(new ATextView(msg));
		}
	}
	
	// Constants.

	LayoutParams FILL = new LayoutParams(LayoutParams.FILL_PARENT,
			LayoutParams.FILL_PARENT);

}

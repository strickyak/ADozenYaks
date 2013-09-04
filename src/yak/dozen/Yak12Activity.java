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
	
	// A magic string that other apps on the device will not guess.
	static String appMagic = new Hash(DH.RandomKey().toString()).asMediumString();

	AppCaller appCaller = new AppCaller(Yak.Fmt("http://localhost:%d/%s?",
			AppServer.DEFAULT_PORT, appMagic));;
	Context yakContext = this;
	Handler yakHandler = new Handler();
	
	public Yak12Activity() {
		Log.i("yak12", "###### CTOR: " + this);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i("yak12", "###### onCreate: " + this);
		super.onCreate(savedInstanceState);

		// Start embedded App Server, if it is not yet started.
		if (serverThread == null) {
			server = new AppServer(AppServer.DEFAULT_PORT, appMagic, "http://yak.net:30332/TODO", null);
			serverThread = new Thread(server);
			serverThread.start();
			Yak.sleepSecs(0.2);
		}

		Intent intent = getIntent();
		Uri uri = intent.getData();
		String query = uri == null ? "" : uri.getQuery();
		query = (query == null) ? "" : query;

		try {
			
			handle(query);
			
		} catch (Exception e) {
			e.printStackTrace();
			displayText("handleYak12Intent CAUGHT EXCEPTION:\n\n"
					+ e.toString());
		}
	}

	private void handle(String query) throws ClientProtocolException, IOException {
		appCaller.handle(query);
	}

	private void displayText(String s) {
		setContentView(new ATextView(s));
	}

	private void displayWeb(final String html) {
		Log.i("displayWeb", "displayWeb");
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
			URI uri = URI.create(url);
			String query = uri.getQuery();
			startIntent(query);
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

	void startIntent(String actQuery) {
		Uri uri = new Uri.Builder().scheme("yak12").path("/")
				.encodedQuery(actQuery).build();
		Intent intent = new Intent("android.intent.action.MAIN", uri);
		intent.setClass(getApplicationContext(), Yak12Activity.class);

		startActivity(intent);
	}

	// Activity Event Hooks
	
	@Override protected void onStart() {
		Log.i("yak12", "###### onStart" + this);
		super.onStart();
	}

	@Override protected void onRestart() {
		Log.i("yak12", "###### onRestart " + this);
		super.onRestart();
	}

	@Override protected void onResume() {
		Log.i("yak12", "###### onResume " + this);
		super.onResume();
	}

	@Override protected void onPause() {
		Log.i("yak12", "###### onPause " + this);
		super.onPause();
	}

	@Override protected void onStop() {
		Log.i("yak12", "###### onStop " + this);
		super.onStop();
	}

	@Override protected void onDestroy() {
		Log.i("yak12", "###### onDestroy " + this);
		super.onDestroy();
	}

	@Override public void setContentView(View view) {
		Log.i("yak12", "@@@@@@@ setContentView:" + view + " " + this);
		super.setContentView(view);
	}

	// Constants.

	LayoutParams FILL = new LayoutParams(LayoutParams.FILL_PARENT,
			LayoutParams.FILL_PARENT);

}

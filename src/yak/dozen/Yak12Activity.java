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
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
	private DozenLogger log = new DozenLogger(2);
	private DozenProgresser progresser = new DozenProgresser();
	
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
			Yak.SleepSecs(0.2);
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
								+ HtmlEscape(e.toString());
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
			this.getSettings().setDefaultFontSize(20);  // was 18.
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
		
		public AndroidFileIO() {
			super(".");
		}

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
	
	public class DozenLogger extends Logger {
		public DozenLogger(int verbosity) {
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
	
	public class DozenProgresser extends Yak.Progresser {
		public void progress(float percent, String s, Object...args) {
			String msg = Fmt("[%6.1f] ", percent) + Fmt(s, args);
			log.log(1, msg);
			setContentView(new ATextView(msg));
		}
	}
	
	// Constants.

	LayoutParams FILL = new LayoutParams(LayoutParams.FILL_PARENT,
			LayoutParams.FILL_PARENT);
	

	//////////////////////////////////////////////////////////////////////////////////////////////
	// Notification Experiments
	//////////////////////////////////////////////////////////////////////////////////////////////
	//     http://developer.android.com/reference/android/app/Service.html#onCreate()
	//////////////////////////////////////////////////////////////////////////////////////////////
	
//	void foo() {
//		Context context = Yak12Activity.this;
//		
//		context = context.getApplicationContext();
//		
//		NotificationManager notificationManager = (NotificationManager) context
//		    .getSystemService(NOTIFICATION_SERVICE);
//		
//		
//		Notification updateComplete = new Notification();
//		updateComplete.icon = android.R.drawable.stat_notify_sync;
//		updateComplete.tickerText = context
//		    .getText(R.string.notification_title);
//		updateComplete.when = System.currentTimeMillis();
//	}
	
	/** Messenger for communicating with service. */
	Messenger mService = null;
	/** Flag indicating whether we have called bind on the service. */
	boolean mIsBound;
	/** Some text view we are using to show state information. */
	TextView mCallbackText;

	/**
	 * Handler of incoming messages from service.
	 */
	class IncomingHandler extends Handler {
	    @Override
	    public void handleMessage(Message msg) {
	        switch (msg.what) {
	            case MessengerService.MSG_SET_VALUE:
	                mCallbackText.setText("Received from service: " + msg.arg1);
	                break;
	            default:
	                super.handleMessage(msg);
	        }
	    }
	}

	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(new IncomingHandler());

	/**
	 * Class for interacting with the main interface of the service.
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className,
	            IBinder service) {
	        // This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  We are communicating with our
	        // service through an IDL interface, so get a client-side
	        // representation of that from the raw service object.
	        mService = new Messenger(service);
	        mCallbackText.setText("Attached.");

	        // We want to monitor the service for as long as we are
	        // connected to it.
	        try {
	            Message msg = Message.obtain(null,
	                    MessengerService.MSG_REGISTER_CLIENT);
	            msg.replyTo = mMessenger;
	            mService.send(msg);

	            // Give it some value as an example.
	            msg = Message.obtain(null,
	                    MessengerService.MSG_SET_VALUE, this.hashCode(), 0);
	            mService.send(msg);
	        } catch (RemoteException e) {
	            // In this case the service has crashed before we could even
	            // do anything with it; we can count on soon being
	            // disconnected (and then reconnected if it can be restarted)
	            // so there is no need to do anything here.
	        }

	        // As part of the sample, tell the user what happened.
	        Toast.makeText(binding, R.string.remote_service_connected,
	                Toast.LENGTH_SHORT).show();
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        mService = null;
	        mCallbackText.setText("Disconnected.");

	        // As part of the sample, tell the user what happened.
	        Toast.makeText(binding, R.string.remote_service_disconnected,
	                Toast.LENGTH_SHORT).show();
	    }
	};

	void doBindService() {
	    // Establish a connection with the service.  We use an explicit
	    // class name because there is no reason to be able to let other
	    // applications replace our component.
	    bindService(new Intent(binding, 
	            MessengerService.class), mConnection, Context.BIND_AUTO_CREATE);
	    mIsBound = true;
	    mCallbackText.setText("Binding.");
	}

	void doUnbindService() {
	    if (mIsBound) {
	        // If we have received the service, and hence registered with
	        // it, then now is the time to unregister.
	        if (mService != null) {
	            try {
	                Message msg = Message.obtain(null,
	                        MessengerService.MSG_UNREGISTER_CLIENT);
	                msg.replyTo = mMessenger;
	                mService.send(msg);
	            } catch (RemoteException e) {
	                // There is nothing special we need to do if the service
	                // has crashed.
	            }
	        }

	        // Detach our existing connection.
	        unbindService(mConnection);
	        mIsBound = false;
	        mCallbackText.setText("Unbinding.");
	    }
	}

	Context binding = Yak12Activity.this;  // In example, was Binding.this.
}

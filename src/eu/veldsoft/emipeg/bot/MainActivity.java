package eu.veldsoft.emipeg.bot;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Bot screen.
 * 
 * @author Todor Balabanov
 */
public class MainActivity extends Activity {
	/**
	 * Web pages states.
	 */
	private enum WebPageState {
		LOGGED_OUT, LOGGED_IN, BEFORE_SEARCH, SEARCH_DONE, MESSAGE_BOX, PROFILE_SELECTED, MESSAGE_SENT,
	};

	/**
	 * Pseudo-random number generator.
	 */
	private static final Random PRNG = new Random();

	/**
	 * Track bot states.
	 */
	private WebPageState state = WebPageState.LOGGED_OUT;

	/**
	 * Web browser view component.
	 */
	WebView browser = null;

	/**
	 * Check user id for female profiles.
	 */
	private int idToCheck = 0;

	/**
	 * Minimum user id to check.
	 */
	private int minId = 1;

	/**
	 * Maximum user id to check.
	 */
	private int maxId = 10000000;

	/**
	 * {@inheritDoc}
	 */
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		/*
		 * Start from the last id which was checked.
		 */
		idToCheck = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getInt("id", 1);
		minId = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getInt("min", 1);
		maxId = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getInt("max", 100000000);

		((EditText) findViewById(R.id.min_id)).setText("" + minId);
		((EditText) findViewById(R.id.max_id)).setText("" + maxId);

		/*
		 * Load next profile to check.
		 */
		Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (browser == null) {
							return;
						}

						if (state != WebPageState.BEFORE_SEARCH) {
							return;
						}

						idToCheck = minId + PRNG.nextInt(maxId - minId + 1);

						SharedPreferences.Editor editor = getSharedPreferences(MainActivity.class.getName(),
								MODE_PRIVATE).edit();
						editor.putInt("id", idToCheck);
						editor.putInt("min", minId);
						editor.putInt("max", maxId);
						editor.commit();

						((TextView) findViewById(R.id.id_to_check)).setText("id = " + idToCheck);

						browser.loadUrl("http://mobile.gepime.com/?id=" + idToCheck);

						state = WebPageState.PROFILE_SELECTED;
					}
				});

			}
		}, 0, 1000, TimeUnit.MILLISECONDS);

		browser = (WebView) findViewById(R.id.browser);
		browser.getSettings()
				.setUserAgentString("Mozilla/5.0 (X11; U; Linux i686; en-US;rv:1.9.0.4) Gecko/20100101 Firefox/4.0");
		browser.getSettings().setJavaScriptEnabled(true);
		browser.getSettings().setDomStorageEnabled(true);

		browser.setWebViewClient(new WebViewClient() {
			public void onPageFinished(WebView view, String url) {
				/*
				 * Set user name, password and login.
				 */
				if (state == WebPageState.LOGGED_OUT) {
					view.loadUrl(
							"javascript:{var uselessvar = document.getElementById('rememberme').checked = 'true';}");
					view.loadUrl(
							"javascript:{var uselessvar = document.getElementById('u2').value = 'todorb3@abv.bg';}");
					view.loadUrl("javascript:{var uselessvar = document.getElementById('p2').value = 'todorb3';}");
					view.loadUrl("javascript:{var uselessvar = document.getElementById('login_button').click();}");

					state = WebPageState.LOGGED_IN;
				} else if (state == WebPageState.LOGGED_IN) {
					state = WebPageState.BEFORE_SEARCH;
				} else if (state == WebPageState.PROFILE_SELECTED) {
					state = WebPageState.BEFORE_SEARCH;
				}
			}
		});

		browser.loadUrl("http://mobile.gepime.com/");
	}
}

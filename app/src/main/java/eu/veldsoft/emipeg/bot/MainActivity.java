package eu.veldsoft.emipeg.bot;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Random;

/**
 * Bot screen.
 *
 * @author Todor Balabanov
 */
public class MainActivity extends Activity {
	/**
	 * Pseudo-random number generator.
	 */
	private static final Random PRNG = new Random();

	/**
	 * Web browser view component.
	 */
	WebView browser = null;

	/**
	 * Is running flag.
	 */
	private boolean running = false;

	/**
	 * Track bot states.
	 */
	private WebPageState state = WebPageState.LOGGED_OUT;

	/**
	 * Track user gender.
	 */
	private UserGender gender = UserGender.NONE;

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
	private int maxId = Integer.MAX_VALUE;

	/**
	 * Minimum user id found.
	 */
	private int minFoundId = Integer.MAX_VALUE;

	/**
	 * Maximum user id found.
	 */
	private int maxFoundId = Integer.MIN_VALUE;

	/**
	 * Show test point toast.
	 *
	 * @param number Number of the test point.
	 */
	private void debug(int number) {
		//Toast.makeText(MainActivity.this, "Test point " + number + " ...", Toast.LENGTH_SHORT).show();
	}

	/**
	 * Load URL address in the web browser web view.
	 *
	 * @param url Address to load.
	 * @param time Milliseconds to wait before loading.
	 */
	private void loadUrl(final String url, final long time) {
		/*
		 * Wait for a while before to proceed.
		 */
		new CountDownTimer(time, time) {
			public void onFinish() {
				browser.loadUrl(url);
			}
			public void onTick(long millisUntilFinished) {}
		}.start();
	}

	/**
	 * Load URL address in the web browser web view.
	 *
	 * @param url Address to load.
	 */
	private void loadUrl(String url) {
		browser.loadUrl(url);
	}

	/**
	 * Select random id for checking.
	 */
	private void randomId() {
		idToCheck = Integer.valueOf(((EditText) findViewById(R.id.id_to_check)).getText().toString());
		minId = Integer.valueOf(((EditText) findViewById(R.id.min_id)).getText().toString());
		maxId = Integer.valueOf(((EditText) findViewById(R.id.max_id)).getText().toString());

		idToCheck = minId + PRNG.nextInt(maxId - minId + 1);
		((EditText) findViewById(R.id.id_to_check)).setText("" + idToCheck);

		SharedPreferences.Editor editor = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).edit();
		editor.putInt("id", idToCheck);
		editor.putInt("min", minId);
		editor.putInt("max", maxId);
		editor.commit();
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		/*
		 * Load login page and stop bot running.
		 */
		((Button) findViewById(R.id.open)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				running = false;
				loadUrl("https://wwww.gepime.com/", 50);
			}
		});

		/*
		 * Handle start and stop of the bot.
		 */
		((Switch) findViewById(R.id.running)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton button, boolean isChecked) {
				running = isChecked;
				if (running == true) {
					randomId();
					state = WebPageState.BEFORE_SEARCH;
				}
				loadUrl("https://wwww.gepime.com/", 50);
			}
		});

		/*
		 * Start from the last id which was checked.
		 */
		idToCheck = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getInt("id", 1);
		minId = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getInt("min", 1);
		maxId = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getInt("max", Integer.MAX_VALUE);
		minFoundId = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getInt("min_found", Integer.MAX_VALUE);
		maxFoundId = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getInt("max_found", Integer.MIN_VALUE);

		((EditText) findViewById(R.id.min_id)).setText("" + minId);
		((EditText) findViewById(R.id.max_id)).setText("" + maxId);
		((TextView) findViewById(R.id.min_found_id)).setText(" " + minFoundId);
		((TextView) findViewById(R.id.max_found_id)).setText(" " + maxFoundId);

		browser = (WebView) findViewById(R.id.browser);
		browser.getSettings()
				  .setUserAgentString("Mozilla/5.0 (X11; U; Linux i686; en-US;rv:1.9.0.4) Gecko/20100101 Firefox/4.0");
		browser.getSettings().setJavaScriptEnabled(true);
		browser.getSettings().setDomStorageEnabled(true);

		/*
		 * Obtain inner HTML text.
		 */
		browser.addJavascriptInterface(new Object() {
			@JavascriptInterface
			public void showHTML(final String html) {
				MainActivity.this.runOnUiThread(new Runnable() {
					public void run() {
						/*
						 * Manage logged in or logged out conditions.
						 */
						if (running == false) {
							return;
						}

						if (html.contains("Изход") && state == WebPageState.LOGGED_OUT) {
							debug(1);
							state = WebPageState.LOGGED_IN;
						}

						if (html.contains("Съобщението е изпратено успешно")) {
							debug(2);
							randomId();
							state = WebPageState.MESSAGE_SENT;
						}

						if (html.contains("Потребители излъчващи се на живо")) {
							debug(3);
							state = WebPageState.BEFORE_SEARCH;
						}

						/*
						 * Different types of profiles or blocked users.
						 */
						if (html.contains("			Жена на ")) {
							/*
							 * Keep track of the min and max ids found only for female profiles.
							 */
							if(idToCheck > maxFoundId) {
								maxFoundId = idToCheck;
							}
							if(idToCheck < minFoundId) {
								minFoundId = idToCheck;
							}
							((TextView) findViewById(R.id.min_found_id)).setText(" " + minFoundId);
							((TextView) findViewById(R.id.max_found_id)).setText(" " + maxFoundId);
							SharedPreferences.Editor editor = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).edit();
							editor.putInt("min_found", minFoundId);
							editor.putInt("max_found", maxFoundId);
							editor.commit();

							debug(4);
							gender = UserGender.FEMALE;
							state = WebPageState.PROFILE_SELECTED;
						} else if (html.contains("			Мъж на ")) {
							debug(5);
							gender = UserGender.MALE;
							state = WebPageState.BEFORE_SEARCH;
						} else if (html.contains("			Двойка (Ж+Ж) на ")) {
							debug(6);
							gender = UserGender.NONE;
							state = WebPageState.BEFORE_SEARCH;
						} else if (html.contains("			Двойка (М+М) на ")) {
							debug(7);
							gender = UserGender.NONE;
							state = WebPageState.BEFORE_SEARCH;
						} else if (html.contains("			Двойка (М+Ж) на ")) {
							debug(8);
							gender = UserGender.NONE;
							state = WebPageState.BEFORE_SEARCH;
						} else if (html.contains("ограничение на профила")) {
							debug(9);
							gender = UserGender.NONE;
							state = WebPageState.BEFORE_SEARCH;
						} else if (html.contains("Заключен профил")) {
							debug(10);
							gender = UserGender.NONE;
							state = WebPageState.BEFORE_SEARCH;
						} else if (html.contains("Изтрит профил")) {
							debug(11);
							gender = UserGender.NONE;
							state = WebPageState.BEFORE_SEARCH;
						}

						/*
						 * Set user name, password and login.
						 */
						if (state == WebPageState.LOGGED_OUT) {
							debug(12);
							loadUrl(
									  "javascript:{var uselessvar = document.getElementById('rememberme').checked = 'true';}");
							loadUrl("javascript:{var uselessvar = document.getElementById('u2').value = '';}");
							loadUrl("javascript:{var uselessvar = document.getElementById('p2').value = '';}");
							loadUrl(
									  "javascript:{var uselessvar = document.getElementById('login_button').click();}");

							state = WebPageState.LOGGED_IN;
							loadUrl("https://wwww.gepime.com/", 50);
						} else if (state == WebPageState.LOGGED_IN) {
							debug(13);
							state = WebPageState.BEFORE_SEARCH;
							loadUrl("https://wwww.gepime.com/", 50);
						} else if (state == WebPageState.BEFORE_SEARCH) {
							debug(14);
							randomId();
							loadUrl("https://www.gepime.com/?id=" + idToCheck, 50);
						} else if (state == WebPageState.PROFILE_SELECTED) {
							debug(15);
							loadUrl(
									  "javascript:{var uselessvar = document.getElementById('pm-input-content').value = 'Здравей.'; profilePMSend('Профил - Нов разговор');}");
							state = WebPageState.BEFORE_SEARCH;

							loadUrl("https://www.gepime.com/?id=" + idToCheck, 10000);
						} else if (state == WebPageState.MESSAGE_SENT) {
							debug(16);
							state = WebPageState.BEFORE_SEARCH;
							loadUrl("https://wwww.gepime.com/", 50);
						} else {
							debug(17);
							state = WebPageState.BEFORE_SEARCH;
							loadUrl("https://wwww.gepime.com/", 50);
						}
					}
				});
			}
		}, "HTMLViewer");

		browser.setWebViewClient(new WebViewClient() {
			public void onPageFinished(WebView view, String url) {
				/*
				 * Load inner HTML text.
				 */
				view.loadUrl(
						  "javascript:HTMLViewer.showHTML('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");
			}
		});
	}

	/**
	 * Web pages states.
	 */
	private enum WebPageState {
		LOGGED_OUT, LOGGED_IN, BEFORE_SEARCH, SEARCH_DONE, MESSAGE_BOX, PROFILE_SELECTED, MESSAGE_SENT,
	}

	/**
	 * User profile gender set.
	 */
	private enum UserGender {
		NONE, MALE, FEMALE
	}
}

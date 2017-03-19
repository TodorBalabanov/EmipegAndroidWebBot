package eu.veldsoft.emipeg.bot;

import java.util.Random;

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
	 * User profile gender set.
	 */
	private enum UserGender {
		NONE, MALE, FEMALE
	}

	/**
	 * Pseudo-random number generator.
	 */
	private static final Random PRNG = new Random();

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
				browser.loadUrl("https://wwww.gepime.com/");
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
				browser.loadUrl("https://wwww.gepime.com/");
			}
		});

		/*
		 * Start from the last id which was checked.
		 */
		idToCheck = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getInt("id", 1);
		minId = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getInt("min", 1);
		maxId = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getInt("max", 100000000);

		((EditText) findViewById(R.id.min_id)).setText("" + minId);
		((EditText) findViewById(R.id.max_id)).setText("" + maxId);

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
							// Toast.makeText(MainActivity.this, "Test point 1
							// ...", Toast.LENGTH_SHORT).show();
							state = WebPageState.LOGGED_IN;
						}

						if (html.contains("Съобщението е изпратено успешно")) {
							// Toast.makeText(MainActivity.this, "Test point 2
							// ...", Toast.LENGTH_SHORT).show();
							randomId();
							state = WebPageState.MESSAGE_SENT;
						}

						if (html.contains("Потребители излъчващи се на живо")) {
							// Toast.makeText(MainActivity.this, "Test point 2
							// ...", Toast.LENGTH_SHORT).show();
							randomId();
							state = WebPageState.BEFORE_SEARCH;
						}

						/*
						 * Different types of profiles or blocked users.
						 */
						if (html.contains("			Жена на ")) {
							// Toast.makeText(MainActivity.this, "Test point 3
							// ...", Toast.LENGTH_SHORT).show();
							gender = UserGender.FEMALE;
							state = WebPageState.PROFILE_SELECTED;
						} else if (html.contains("			Мъж на ")) {
							// Toast.makeText(MainActivity.this, "Test point 4
							// ...", Toast.LENGTH_SHORT).show();
							randomId();
							gender = UserGender.MALE;
							state = WebPageState.BEFORE_SEARCH;
						} else if (html.contains("			Двойка (Ж+Ж) на ")) {
							// Toast.makeText(MainActivity.this, "Test point 5
							// ...", Toast.LENGTH_SHORT).show();
							randomId();
							gender = UserGender.NONE;
							state = WebPageState.BEFORE_SEARCH;
						} else if (html.contains("			Двойка (М+М) на ")) {
							// Toast.makeText(MainActivity.this, "Test point 6
							// ...", Toast.LENGTH_SHORT).show();
							randomId();
							gender = UserGender.NONE;
							state = WebPageState.BEFORE_SEARCH;
						} else if (html.contains("			Двойка (М+Ж) на ")) {
							// Toast.makeText(MainActivity.this, "Test point 7
							// ...", Toast.LENGTH_SHORT).show();
							randomId();
							gender = UserGender.NONE;
							state = WebPageState.BEFORE_SEARCH;
						} else if (html.contains("ограничение на профила")) {
							// Toast.makeText(MainActivity.this, "Test point 8
							// ...", Toast.LENGTH_SHORT).show();
							randomId();
							gender = UserGender.NONE;
							state = WebPageState.BEFORE_SEARCH;
						} else if (html.contains("Заключен профил")) {
							// Toast.makeText(MainActivity.this, "Test point 8
							// ...", Toast.LENGTH_SHORT).show();
							randomId();
							gender = UserGender.NONE;
							state = WebPageState.BEFORE_SEARCH;
						} else if (html.contains("Изтрит профил")) {
							// Toast.makeText(MainActivity.this, "Test point 8
							// ...", Toast.LENGTH_SHORT).show();
							randomId();
							gender = UserGender.NONE;
							state = WebPageState.BEFORE_SEARCH;
						}

						/*
						 * Set user name, password and login.
						 */
						if (state == WebPageState.LOGGED_OUT) {
							// Toast.makeText(MainActivity.this, "Test point 9
							// ...", Toast.LENGTH_SHORT).show();
							browser.loadUrl(
									"javascript:{var uselessvar = document.getElementById('rememberme').checked = 'true';}");
							browser.loadUrl("javascript:{var uselessvar = document.getElementById('u2').value = '';}");
							browser.loadUrl("javascript:{var uselessvar = document.getElementById('p2').value = '';}");
							browser.loadUrl(
									"javascript:{var uselessvar = document.getElementById('login_button').click();}");

							state = WebPageState.LOGGED_IN;
							browser.loadUrl("https://wwww.gepime.com/");
						} else if (state == WebPageState.LOGGED_IN) {
							// Toast.makeText(MainActivity.this, "Test point 10
							// ...", Toast.LENGTH_SHORT).show();
							state = WebPageState.BEFORE_SEARCH;
							browser.loadUrl("https://wwww.gepime.com/");
						} else if (state == WebPageState.BEFORE_SEARCH) {
							// Toast.makeText(MainActivity.this, "Test point 8
							// ...", Toast.LENGTH_SHORT).show();
							browser.loadUrl("https://www.gepime.com/?id=" + idToCheck);
						} else if (state == WebPageState.PROFILE_SELECTED) {
							// Toast.makeText(MainActivity.this, "Test point 11
							// ...", Toast.LENGTH_SHORT).show();
							browser.loadUrl(
									"javascript:{var uselessvar = document.getElementById('pm-input-content').value = 'Здравей.'; profilePMSend('Профил - Нов разговор');}");
							randomId();
							state = WebPageState.BEFORE_SEARCH;

							/*
							 * Wait for a while before to proceed.
							 */
							new CountDownTimer(1000, 1000) {
								public void onFinish() {
									browser.loadUrl("https://www.gepime.com/?id=" + idToCheck);
								}

								public void onTick(long millisUntilFinished) {
								}
							}.start();
						} else if (state == WebPageState.MESSAGE_SENT) {
							// Toast.makeText(MainActivity.this, "Test point 12
							// ...", Toast.LENGTH_SHORT).show();
							state = WebPageState.BEFORE_SEARCH;
							browser.loadUrl("https://wwww.gepime.com/");
						} else {
							// Toast.makeText(MainActivity.this, "Test point 13
							// ...", Toast.LENGTH_SHORT).show();
							state = WebPageState.BEFORE_SEARCH;
							browser.loadUrl("https://wwww.gepime.com/");
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
}

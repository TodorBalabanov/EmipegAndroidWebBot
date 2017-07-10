package eu.veldsoft.emipeg.bot;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
	}

	/**
	 * User profile gender set.
	 */
	private enum UserGender {
		NONE, MALE, FEMALE
	}

	/**
	 * Request code for the settings activity.
	 */
	private static final int SETTINGS_ACTIVITY = 0x01;

	/**
	 * Pseudo-random number generator.
	 */
	private static final Random PRNG = new Random();

	/**
	 * Name of the file used to store ids.
	 */
	private static final String IDS_FILE_NAME = "ids.bin";

	/**
	 * Web browser view component.
	 */
	WebView browser = null;

	/**
	 * Moment in time to wake up if there is a blocking.
	 */
	private long wakeupAt;

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
	 * Time out after message send.
	 */
	private int timeout = 5000;

	/**
	 * Time out for wake-up after blocking.
	 */
	private int wakeup = 20000;

	/**
	 * Message send flag.
	 */
	private boolean messageSend = false;

	/**
	 * Friendship send flag.
	 */
	private boolean friendshipSend = false;

	/**
	 * Set of ids found.
	 */
	private Set<Integer> ids = new HashSet<Integer>();

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
	 * @param url  Address to load.
	 * @param time Milliseconds to wait before loading.
	 */
	private void loadUrl(final String url, final long time) {
		if (running == false) {
			return;
		}

		/*
		 * Wait for a while before to proceed.
		 */
		new CountDownTimer(time, time) {
			public void onFinish() {
				browser.loadUrl(url);
			}

			public void onTick(long millisUntilFinished) {
			}
		}.start();
	}

	/**
	 * Load URL address in the web browser web view.
	 *
	 * @param url Address to load.
	 */
	private void loadUrl(String url) {
		if (running == false) {
			return;
		}

		browser.loadUrl(url);
	}

	/**
	 * Select random id for checking.
	 */
	private void randomId() {
		idToCheck = Integer.valueOf(((EditText) findViewById(R.id.id_to_check)).getText().toString());

		idToCheck = minId + PRNG.nextInt(maxId - minId + 1);
		((EditText) findViewById(R.id.id_to_check)).setText("" + idToCheck);

		SharedPreferences.Editor editor = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).edit();
		editor.putInt("id", idToCheck);
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
		 * Load ids found in previous sessions.
		 */
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(getFilesDir() + IDS_FILE_NAME));
			ids = (HashSet<Integer>) in.readObject();
			in.close();
		} catch (IOException e) {
		} catch (ClassNotFoundException e) {
		}

		idToCheck = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getInt("id", 1);
		minId = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getInt("min", 1);
		maxId = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getInt("max", Integer.MAX_VALUE);
		minFoundId = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getInt("min_found", Integer.MAX_VALUE);
		maxFoundId = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getInt("max_found", Integer.MIN_VALUE);
		timeout = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getInt("timeout", 0);
		wakeup = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getInt("wakeup", Integer.MAX_VALUE);
		messageSend = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getBoolean("message_send", false);
		friendshipSend = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getBoolean("friendship_send", false);

		/*
		 * Check for wakeup event.
		 */
		wakeupAt = System.currentTimeMillis() + wakeup;
		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate
				  (new Runnable() {
					  public void run() {
						  if(running == false) {
							  return;
						  }

						  if(System.currentTimeMillis() > wakeupAt) {
							  loadUrl("https://wwww.gepime.com/");
						  }
					  }
				  }, 0, 1, TimeUnit.MINUTES);

		/*
		 * Prepare web browser object.
		 */
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

						wakeupAt = System.currentTimeMillis() + wakeup;

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
							loadUrl("https://wwww.gepime.com/");
						} else if (state == WebPageState.LOGGED_IN) {
							debug(13);
							randomId();
							state = WebPageState.BEFORE_SEARCH;
							loadUrl("https://wwww.gepime.com/");
						} else if (state == WebPageState.BEFORE_SEARCH) {
							debug(14);
							state = WebPageState.SEARCH_DONE;
							loadUrl("https://www.gepime.com/?id=" + idToCheck, 100);
						} else if (state == WebPageState.SEARCH_DONE) {
							/*
							 * Different types of profiles or blocked users.
							 */
							if (html.contains("			Жена на ")) {
							/*
							 * Keep track of the min and max ids found only for female profiles.
							 */
								if (idToCheck > maxFoundId) {
									maxFoundId = idToCheck;
								}
								if (idToCheck < minFoundId) {
									minFoundId = idToCheck;
								}

								ids.add(idToCheck);

								/**
								 * Keep list of the ids.
								 */
								try {
									ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(getFilesDir() + IDS_FILE_NAME));
									out.writeObject(ids);
									out.close();
								} catch (IOException e) {
								}

								/**
								 * Keep values in the shared preferences.
								 */
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
								state = WebPageState.LOGGED_IN;
							} else if (html.contains("			Двойка (Ж+Ж) на ")) {
								debug(6);
								gender = UserGender.NONE;
								state = WebPageState.LOGGED_IN;
							} else if (html.contains("			Двойка (М+М) на ")) {
								debug(7);
								gender = UserGender.NONE;
								state = WebPageState.LOGGED_IN;
							} else if (html.contains("			Двойка (М+Ж) на ")) {
								debug(8);
								gender = UserGender.NONE;
								state = WebPageState.LOGGED_IN;
							} else if (html.contains("ограничение на профила")) {
								debug(9);
								gender = UserGender.NONE;
								state = WebPageState.LOGGED_IN;
							} else if (html.contains("Заключен профил")) {
								debug(10);
								gender = UserGender.NONE;
								state = WebPageState.LOGGED_IN;
							} else if (html.contains("Изтрит профил")) {
								debug(11);
								gender = UserGender.NONE;
								state = WebPageState.LOGGED_IN;
							}
							loadUrl("https://wwww.gepime.com/");
						} else if (state == WebPageState.PROFILE_SELECTED) {
							debug(15);

							if (messageSend == true) {
								loadUrl(
										  "javascript:{var uselessvar = document.getElementById('pm-input-content').value = 'Здравей! Как си? С каква цел си в сайта? Поздрави, Тодор'; profilePMSend('Профил - Нов разговор');}");
							}
							if (friendshipSend == true) {
								loadUrl(
										  "javascript:{inviteFriend(" + idToCheck + ");Analytics.track('profile', 'friend_invite');}");
								loadUrl(
										  "javascript:{var uselessvar = document.getElementById('invCustText').value = 'Здравей! Как си? С каква цел си в сайта? Поздрави, Тодор';}", 100);
								loadUrl("javascript:{ajaxSubmit('friendInviteForm', 'invitationContainer');}", 400);
							}

							state = WebPageState.LOGGED_IN;

							loadUrl("https://www.gepime.com/", timeout);
						} else if (state == WebPageState.MESSAGE_SENT) {
							debug(16);
							state = WebPageState.LOGGED_IN;
							loadUrl("https://wwww.gepime.com/");
						} else {
							debug(17);
							state = WebPageState.LOGGED_IN;
							loadUrl("https://wwww.gepime.com/");
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
	 * Creat option menu.
	 *
	 * @param menu Menu information.
	 * @return Success of the cration.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		new MenuInflater(this).inflate(R.menu.main_options, menu);
		return (super.onCreateOptionsMenu(menu));
	}

	/**
	 * On menu item selected handler.
	 *
	 * @param item Item which was selected.
	 * @return Is the handling succesful.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

			/*
			 * Report found ids by email.
			 */
			case R.id.report:
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("message/rfc822");
				intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"tol@abv.bg"});
				intent.putExtra(Intent.EXTRA_SUBJECT, "Report - " + new Date() + " ...");
				String message = "";
				message += "min: ";
				message += minFoundId;
				message += "\n";
				message += "max: ";
				message += maxFoundId;
				message += "\n";
				message += "visited: ";
				List<Integer> list = new ArrayList<Integer>(ids);
				Collections.sort(list);
				message += list.toString();
				message += "\n";
				intent.putExtra(Intent.EXTRA_TEXT, message);
				try {
					startActivity(Intent.createChooser(intent, "Send visited profiles report..."));
				} catch (android.content.ActivityNotFoundException ex) {
				}
				break;

			/*
			 * Handle start of the bot.
			 */
			case R.id.runnig_on:
				wakeupAt = System.currentTimeMillis() + wakeup;
				running = true;
				randomId();
				state = WebPageState.BEFORE_SEARCH;
				loadUrl("https://wwww.gepime.com/");
				break;

			/*
			 * Handle stop of the bot.
			 */
			case R.id.runnig_off:
				running = false;
				break;

			/*
			 * Run settings activity.
			 */
			case R.id.settings:
				startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), SETTINGS_ACTIVITY);
				break;
		}

		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SETTINGS_ACTIVITY) {
			idToCheck = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getInt("id", 1);
			minId = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getInt("min", 1);
			maxId = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getInt("max", Integer.MAX_VALUE);
			minFoundId = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getInt("min_found", Integer.MAX_VALUE);
			maxFoundId = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getInt("max_found", Integer.MIN_VALUE);
			timeout = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getInt("timeout", 0);
			wakeup = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getInt("wakeup", Integer.MAX_VALUE);
			messageSend = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getBoolean("message_send", false);
			friendshipSend = getSharedPreferences(MainActivity.class.getName(), MODE_PRIVATE).getBoolean("friendship_send", false);
		}
	}
}

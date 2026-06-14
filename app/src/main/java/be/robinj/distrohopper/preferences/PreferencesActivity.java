package be.robinj.distrohopper.preferences;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreference;
import androidx.preference.ListPreferenceDialogFragmentCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import be.robinj.distrohopper.desktop.FrostedGlass;
import be.robinj.distrohopper.home.PinnedAppsMigration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.acra.ACRA;

import be.robinj.distrohopper.AboutActivity;
import be.robinj.distrohopper.AppRestart;
import be.robinj.distrohopper.BuildConfig;
import be.robinj.distrohopper.ContributeActivity;
import be.robinj.distrohopper.DependencyContainer;
import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.HomeRole;
import be.robinj.distrohopper.accessibility.AccessibilityGestureSetupActivity;
import be.robinj.distrohopper.accessibility.NotificationAccessibilityService;
import be.robinj.distrohopper.home.GestureAction;
import be.robinj.distrohopper.IconPackHelper;
import be.robinj.distrohopper.InsetsHelper;
import be.robinj.distrohopper.R;
import be.robinj.distrohopper.cache.AppIconCache;
import be.robinj.distrohopper.cache.AppLabelCache;
import be.robinj.distrohopper.home.DefaultPinnedApps;
import be.robinj.distrohopper.onboarding.OnboardingGate;

/**
 * Presents the application settings as a single list.
 */
public class PreferencesActivity extends AppCompatActivity
{
	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_preferences);
		InsetsHelper.applySystemBarsPadding (this);

		if (savedInstanceState == null)
		{
			this.getSupportFragmentManager ()
				.beginTransaction ()
				.replace (R.id.preferences_container, new PreferencesFragment ())
				.commit ();
		}

		this.getSupportActionBar ().setDisplayHomeAsUpEnabled (true);
	}

	@Override
	public boolean onCreateOptionsMenu (Menu menu)
	{
		this.getMenuInflater ().inflate (R.menu.preferences, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected (MenuItem item)
	{
		try
		{
			int id = item.getItemId ();

			if (id == android.R.id.home)
			{
				NavUtils.navigateUpFromSameTask (this);
				return true;
			}
			else if (id == R.id.menuAbout)
			{
				Intent intent = new Intent (this, AboutActivity.class);
				this.startActivity (intent);
			}
			else if (id == R.id.menuContribute)
			{
				Intent intent = new Intent (this, ContributeActivity.class);
				this.startActivity (intent);
			}
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this);
		}

		return super.onOptionsItemSelected (item);
	}

	public static class PreferencesFragment extends PreferenceFragmentCompat
	{
		private PreferenceCategory devCategory;

		// The native HOME-role dialog is broken on some OEM builds (notably
		// Samsung): it returns without ever showing a picker. When the role still
		// isn't held after it closes, fall back to the system home settings;
		// homeSettingsRequest is a separate launcher so its result never
		// re-triggers that fallback (which would loop). //
		private final ActivityResultLauncher<Intent> roleRequest =
			this.registerForActivityResult (
				new ActivityResultContracts.StartActivityForResult (),
				result ->
				{
					if (! HomeRole.isHeld (this.requireContext ()))
						this.homeSettingsRequest.launch (HomeRole.homeSettingsIntent ());
				});

		private final ActivityResultLauncher<Intent> homeSettingsRequest =
			this.registerForActivityResult (
				new ActivityResultContracts.StartActivityForResult (),
				result -> { /* onResume re-checks the option's visibility */ });

		@Override
		public void onCreatePreferences (Bundle savedInstanceState, String rootKey)
		{
			this.getPreferenceManager ().setSharedPreferencesName (Preferences.PREFERENCES);

			// No root PreferenceScreen exists until one is set; give the categories
			// below something to attach to //
			this.setPreferenceScreen (
				this.getPreferenceManager ().createPreferenceScreen (this.requireContext ()));

			this.addCategory (R.string.pref_header_appearance, R.xml.pref_appearance);
			this.addCategory (R.string.pref_header_functionality, R.xml.pref_functionality);
			this.addCategory (R.string.pref_header_gestures, R.xml.pref_gestures);
			this.addCategory (R.string.pref_header_advanced, R.xml.pref_advanced);
			this.devCategory = this.addCategory (R.string.pref_header_dev, R.xml.pref_dev);

			this.initFontPreference ();
			this.initCrashReportsPreference ();
			this.initLauncherPinModePreference ();
			this.initDevPreference ();
			this.initGesturePreferences ();

			this.findPreference ("dummy_wallpaper").setOnPreferenceClickListener (
				new Preference.OnPreferenceClickListener ()
				{
					@Override
					public boolean onPreferenceClick (Preference preference)
					{
						try
						{
							startActivity (Intent.createChooser (
								new Intent (Intent.ACTION_SET_WALLPAPER),
								getString (R.string.option_wallpaper)));
						}
						catch (Exception ex)
						{
							new ExceptionHandler (ex).show (requireActivity ());
						}

						return true;
					}
				}
			);

			this.findPreference ("dummy_customise").setOnPreferenceClickListener (
				new Preference.OnPreferenceClickListener ()
				{
					@Override
					public boolean onPreferenceClick (Preference preference)
					{
						requireActivity ().setResult (4);
						requireActivity ().finish ();

						return true;
					}
				}
			);

			final Preference clearCache = this.findPreference ("dummy_clear_cache");
			if (clearCache != null)
			{
				clearCache.setOnPreferenceClickListener (new Preference.OnPreferenceClickListener ()
				{
					@Override
					public boolean onPreferenceClick (Preference preference)
					{
						try
						{
							clearAppCaches ();
							Toast.makeText (requireContext (),
								R.string.toast_cache_cleared, Toast.LENGTH_SHORT).show ();
						}
						catch (Exception ex)
						{
							new ExceptionHandler (ex).show (requireActivity ());
						}

						return true;
					}
				});
			}

			this.findPreference ("dummy_rerun_onboarding").setOnPreferenceClickListener (
				new Preference.OnPreferenceClickListener ()
				{
					@Override
					public boolean onPreferenceClick (Preference preference)
					{
						OnboardingGate.reset (
							DependencyContainer.of (requireContext ()).getPrefs ());
						Toast.makeText (requireContext (),
							R.string.toast_rerun_onboarding, Toast.LENGTH_SHORT).show ();

						return true;
					}
				}
			);

			this.findPreference ("dummy_pin_default_apps").setOnPreferenceClickListener (
				new Preference.OnPreferenceClickListener ()
				{
					@Override
					public boolean onPreferenceClick (Preference preference)
					{
						DefaultPinnedApps.queue (
							DependencyContainer.of (requireContext ()).getPrefs ());
						Toast.makeText (requireContext (),
							R.string.toast_pin_default_apps, Toast.LENGTH_SHORT).show ();

						return true;
					}
				}
			);

			this.findPreference ("dummy_set_default_launcher").setOnPreferenceClickListener (
				new Preference.OnPreferenceClickListener ()
				{
					@Override
					public boolean onPreferenceClick (Preference preference)
					{
						try
						{
							final Intent roleIntent =
								HomeRole.roleRequestIntent (requireContext ());
							if (roleIntent != null)
								roleRequest.launch (roleIntent);
							else
								homeSettingsRequest.launch (HomeRole.homeSettingsIntent ());
						}
						catch (Exception ex)
						{
							new ExceptionHandler (ex).show (requireActivity ());
						}

						return true;
					}
				}
			);

			this.findPreference ("dummy_full_restart").setOnPreferenceClickListener (
				new Preference.OnPreferenceClickListener ()
				{
					@Override
					public boolean onPreferenceClick (Preference preference)
					{
						try
						{
							AppRestart.restart (requireContext ());
						}
						catch (Exception ex)
						{
							new ExceptionHandler (ex).show (requireActivity ());
						}

						return true;
					}
				}
			);
		}

		@Override
		public void onResume ()
		{
			super.onResume ();

			// Only offer to set DistroHopper as the home screen while it isn't;
			// re-checked here in case the user just made it so //
			final Preference setDefaultLauncher = this.findPreference ("dummy_set_default_launcher");
			if (setDefaultLauncher != null)
			{
				setDefaultLauncher.setVisible (! HomeRole.isHeld (this.requireContext ()));
			}

			// Re-evaluate the notification-tray option in case the user just
			// enabled (or disabled) the accessibility service. //
			this.applyGesturePreferences ();
		}

		// The icon-pack and app-sort-order pickers are framework-created ListPreference
		// dialogs, so they can't take an OnShowListener at a builder. Route them through a
		// subclass that applies the frosted fallback once shown, keeping them legible where
		// cross-window blur is unavailable (e.g. Samsung). //
		private static final String DIALOG_FRAGMENT_TAG =
			"androidx.preference.PreferenceFragment.DIALOG";

		@Override
		public void onDisplayPreferenceDialog (final Preference preference)
		{
			if (this.getParentFragmentManager ().findFragmentByTag (DIALOG_FRAGMENT_TAG) != null)
				return;

			if (preference instanceof ListPreference)
			{
				final DialogFragment fragment =
					FrostedListPreferenceDialogFragment.newInstance (preference.getKey ());
				fragment.setTargetFragment (this, 0);
				fragment.show (this.getParentFragmentManager (), DIALOG_FRAGMENT_TAG);
			}
			else
			{
				super.onDisplayPreferenceDialog (preference);
			}
		}

		private PreferenceCategory addCategory (final int titleRes, final int prefsRes)
		{
			final PreferenceCategory header = new PreferenceCategory (this.requireContext ());
			header.setTitle (titleRes);
			final androidx.preference.PreferenceScreen screen = this.getPreferenceScreen ();
			screen.addPreference (header);

			// addPreferencesFromResource appends the inflated preferences to the root
			// screen; move them under the category header so the whole section can be
			// shown or hidden as a single unit. //
			final int firstNew = screen.getPreferenceCount ();
			this.addPreferencesFromResource (prefsRes);
			final List<Preference> inflated = new ArrayList<> ();
			for (int i = firstNew; i < screen.getPreferenceCount (); i++)
				inflated.add (screen.getPreference (i));
			for (final Preference child : inflated)
			{
				screen.removePreference (child);
				header.addPreference (child);
			}

			return header;
		}

		private void initDevPreference ()
		{
			final SwitchPreferenceCompat pref = this.findPreference (
				be.robinj.distrohopper.preferences.Preference.DEV.getName ());
			if (pref == null)
				return;

			this.applyDeveloperModeVisibility (pref.isChecked ());

			pref.setOnPreferenceChangeListener ((preference, newValue) ->
			{
				this.applyDeveloperModeVisibility (Boolean.TRUE.equals (newValue));

				return true;
			});
		}

		// The swipe-up / swipe-down gesture dropdowns. The notification-tray
		// option is only offered once the accessibility service is enabled, and a
		// no-lockout rule keeps at least one gesture opening the dash. Re-run on
		// resume because the user may return from the enable wizard / system
		// settings with the service now on. //
		private void initGesturePreferences ()
		{
			this.applyGesturePreferences ();
		}

		private void applyGesturePreferences ()
		{
			final ListPreference up = this.findPreference (
				be.robinj.distrohopper.preferences.Preference.GESTURE_SWIPE_UP.getName ());
			final ListPreference down = this.findPreference (
				be.robinj.distrohopper.preferences.Preference.GESTURE_SWIPE_DOWN.getName ());
			if (up == null || down == null)
				return;

			final boolean serviceEnabled =
				NotificationAccessibilityService.isEnabled (this.requireContext ());

			this.populateGestureList (up, serviceEnabled);
			this.populateGestureList (down, serviceEnabled);

			// A stored notifications choice can't work with the service off: fall
			// back to opening the dash. //
			if (! serviceEnabled)
			{
				resetIfNotifications (up);
				resetIfNotifications (down);
			}

			up.setSummaryProvider (ListPreference.SimpleSummaryProvider.getInstance ());
			down.setSummaryProvider (ListPreference.SimpleSummaryProvider.getInstance ());

			up.setOnPreferenceChangeListener ((preference, newValue) ->
			{
				reconcileSiblingGesture (down, String.valueOf (newValue));
				return true;
			});
			down.setOnPreferenceChangeListener ((preference, newValue) ->
			{
				reconcileSiblingGesture (up, String.valueOf (newValue));
				return true;
			});

			final Preference enableRow = this.findPreference ("dummy_enable_notification_gestures");
			if (enableRow != null)
			{
				enableRow.setVisible (! serviceEnabled);
				enableRow.setOnPreferenceClickListener (preference ->
				{
					this.startActivity (new Intent (this.requireContext (),
						AccessibilityGestureSetupActivity.class));
					return true;
				});
			}
		}

		private void populateGestureList (final ListPreference pref, final boolean withNotifications)
		{
			final List<CharSequence> entries = new ArrayList<> ();
			final List<CharSequence> values = new ArrayList<> ();

			entries.add (this.getString (R.string.gesture_action_none));
			values.add (GestureAction.NONE.getValue ());
			entries.add (this.getString (R.string.gesture_action_open_dash));
			values.add (GestureAction.OPEN_DASH.getValue ());
			entries.add (this.getString (R.string.gesture_action_open_dash_search));
			values.add (GestureAction.OPEN_DASH_SEARCH.getValue ());
			if (withNotifications)
			{
				entries.add (this.getString (R.string.gesture_action_notifications_tray));
				values.add (GestureAction.NOTIFICATIONS.getValue ());
			}

			pref.setEntries (entries.toArray (new CharSequence[0]));
			pref.setEntryValues (values.toArray (new CharSequence[0]));
		}

		private static void resetIfNotifications (final ListPreference pref)
		{
			if (GestureAction.fromValue (pref.getValue ()) == GestureAction.NOTIFICATIONS)
				pref.setValue (GestureAction.OPEN_DASH.getValue ());
		}

		// Enforce the no-lockout rule: once one gesture is set to [changedValue],
		// nudge the other back to "open dash" if neither would open the dash.
		// Package-private + static so it can be unit-tested without the Activity. //
		static void reconcileSiblingGesture (final ListPreference other, final String changedValue)
		{
			final GestureAction fix = GestureAction.reconcileOther (
				GestureAction.fromValue (changedValue),
				GestureAction.fromValue (other.getValue ()));
			if (fix != null)
				other.setValue (fix.getValue ());
		}

		// Only surface the developer options section while developer mode is on;
		// turning it off also clears any toggles that were left enabled. //
		private void applyDeveloperModeVisibility (final boolean enabled)
		{
			if (this.devCategory != null)
				this.devCategory.setVisible (enabled);

			if (! enabled)
				this.clearChildPreferences (be.robinj.distrohopper.preferences.Preference.DEV);
		}

		private void clearChildPreferences (final be.robinj.distrohopper.preferences.Preference parent)
		{
			final SharedPreferences.Editor editor = Preferences
				.getSharedPreferences (this.requireContext ())
				.edit ();

			for (final be.robinj.distrohopper.preferences.Preference child
				: be.robinj.distrohopper.preferences.Preference.values ())
			{
				if (child.getParent () != parent)
					continue;

				final Preference toggle = this.findPreference (child.getName ());
				if (toggle instanceof SwitchPreferenceCompat)
					((SwitchPreferenceCompat) toggle).setChecked (false);

				editor.remove (child.getName ());
			}

			editor.apply ();
		}

		private void clearAppCaches ()
		{
			new AppLabelCache (this.requireContext ().getApplicationContext ()).clear ();
			AppIconCache.clearAll (this.requireContext ().getApplicationContext ());
		}

		private void initLauncherPinModePreference ()
		{
			final SwitchPreferenceCompat pref = this.findPreference ("dummy_launcher_pin_per_desktop");
			if (pref == null)
				return;

			final SharedPreferences prefs = Preferences.getSharedPreferences (this.requireContext ());
			pref.setChecked (LauncherPinMode.current (prefs) == LauncherPinMode.DESKTOP);
			pref.setOnPreferenceChangeListener ((preference, newValue) ->
			{
				// Rewrite the stored pins for the new mode; returning to the home
				// screen relaunches it, which loads them in the new mode //
				final LauncherPinMode mode = ((Boolean) newValue)
						? LauncherPinMode.DESKTOP : LauncherPinMode.GLOBAL;
				PinnedAppsMigration.migrate (this.requireContext (), mode);
				prefs.edit ()
					.putString (
						be.robinj.distrohopper.preferences.Preference.LAUNCHER_APP_PIN_MODE.getName (),
						mode.getValue ())
					.apply ();

				return true;
			});
		}

		private void initCrashReportsPreference ()
		{
			this.applyCrashReportsPreference (this.isCrashReportingConfigured ());
		}

		/** Allows tests to exercise both build-time configurations. */
		protected boolean isCrashReportingConfigured ()
		{
			return BuildConfig.ACRA_CONFIGURED;
		}

		// Package-private so it can be driven with either flag value from tests. //
		void applyCrashReportsPreference (final boolean acraConfigured)
		{
			final Preference crashPref = this.findPreference (
				be.robinj.distrohopper.preferences.Preference.CRASH_REPORTING_ENABLED.getName ());
			if (crashPref == null) return;

			if (! acraConfigured)
			{
				// No crash-report credentials were provided at build time, so the
				// option can never do anything: hide it entirely. //
				if (crashPref.getParent () != null)
					crashPref.getParent ().removePreference (crashPref);
				return;
			}

			// Apply toggles immediately, without waiting for an app restart. //
			crashPref.setOnPreferenceChangeListener (new Preference.OnPreferenceChangeListener ()
			{
				@Override
				public boolean onPreferenceChange (Preference preference, Object newValue)
				{
					try
					{
						ACRA.getErrorReporter ().setEnabled (Boolean.TRUE.equals (newValue));
					}
					catch (Exception ex)
					{
						new ExceptionHandler (ex).logAndTrack ();
					}
					return true;
				}
			});
		}

		private void initFontPreference ()
		{
			final ListPreference fontPref = this.findPreference (
				be.robinj.distrohopper.preferences.Preference.FONT.getName ());
			if (fontPref == null)
				return;

			// Show the chosen font as the summary; recreate so the new font is
			// applied to this screen immediately. Other activities pick it up
			// the next time they are created. //
			fontPref.setSummaryProvider (ListPreference.SimpleSummaryProvider.getInstance ());
			fontPref.setOnPreferenceChangeListener (new Preference.OnPreferenceChangeListener ()
			{
				@Override
				public boolean onPreferenceChange (final Preference preference, final Object newValue)
				{
					requireActivity ().recreate ();
					return true;
				}
			});
		}

	}

	/**
	 * A {@link ListPreferenceDialogFragmentCompat} that paints the frosted fallback over its
	 * surface once shown, so the picker stays legible where cross-window blur is unavailable.
	 * Public + static so the framework can recreate it across configuration changes.
	 */
	public static class FrostedListPreferenceDialogFragment extends ListPreferenceDialogFragmentCompat
	{
		public static FrostedListPreferenceDialogFragment newInstance (final String key)
		{
			final FrostedListPreferenceDialogFragment fragment =
				new FrostedListPreferenceDialogFragment ();
			final Bundle args = new Bundle (1);
			args.putString (ARG_KEY, key);
			fragment.setArguments (args);

			return fragment;
		}

		@Override
		public void onStart ()
		{
			super.onStart ();

			final Dialog dialog = this.getDialog ();
			if (dialog != null && dialog.getWindow () != null)
				FrostedGlass.INSTANCE.applyDialogFallback (dialog.getWindow ());
		}
	}
}

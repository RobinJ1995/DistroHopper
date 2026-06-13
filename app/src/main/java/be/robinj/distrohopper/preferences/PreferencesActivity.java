package be.robinj.distrohopper.preferences;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import be.robinj.distrohopper.home.PinnedAppsMigration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.acra.ACRA;

import be.robinj.distrohopper.AboutActivity;
import be.robinj.distrohopper.BuildConfig;
import be.robinj.distrohopper.ContributeActivity;
import be.robinj.distrohopper.DependencyContainer;
import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.HomeRole;
import be.robinj.distrohopper.IconPackHelper;
import be.robinj.distrohopper.InsetsHelper;
import be.robinj.distrohopper.R;
import be.robinj.distrohopper.cache.AppIconCache;
import be.robinj.distrohopper.cache.AppLabelCache;
import be.robinj.distrohopper.cache.ExpiringCache;
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
			this.addCategory (R.string.pref_header_advanced, R.xml.pref_advanced);
			this.addCategory (R.string.pref_header_dev, R.xml.pref_dev);

			this.initIconPackList ();
			this.initCrashReportsPreference ();
			this.initLauncherPinModePreference ();
			this.initDevPreference ();

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

			this.findPreference (
				be.robinj.distrohopper.preferences.Preference.DEV_CLEAR_CACHE.getName ())
				.setOnPreferenceClickListener (
				new Preference.OnPreferenceClickListener ()
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
				}
			);

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
							startActivity (HomeRole.requestIntent (requireContext ()));
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
		}

		private void addCategory (final int titleRes, final int prefsRes)
		{
			final PreferenceCategory header = new PreferenceCategory (this.requireContext ());
			header.setTitle (titleRes);
			this.getPreferenceScreen ().addPreference (header);
			this.addPreferencesFromResource (prefsRes);
		}

		private void initDevPreference ()
		{
			final SwitchPreferenceCompat pref = this.findPreference (
				be.robinj.distrohopper.preferences.Preference.DEV.getName ());
			if (pref == null)
				return;

			pref.setOnPreferenceChangeListener ((preference, newValue) ->
			{
				if (Boolean.FALSE.equals (newValue))
				{
					this.clearDevPreferences ();
				}

				return true;
			});
		}

		private void clearDevPreferences ()
		{
			final SharedPreferences prefs = Preferences.getSharedPreferences (this.requireContext ());
			prefs.edit ()
				.remove (be.robinj.distrohopper.preferences.Preference.DEV_LOG_TOASTER.getName ())
				.remove (be.robinj.distrohopper.preferences.Preference.DEV_WIDGET_RESIZE_ANY.getName ())
				.apply ();

			final SwitchPreferenceCompat logToaster = this.findPreference (
				be.robinj.distrohopper.preferences.Preference.DEV_LOG_TOASTER.getName ());
			if (logToaster != null)
				logToaster.setChecked (false);

			final SwitchPreferenceCompat widgetResize = this.findPreference (
				be.robinj.distrohopper.preferences.Preference.DEV_WIDGET_RESIZE_ANY.getName ());
			if (widgetResize != null)
				widgetResize.setChecked (false);
		}

		private void clearAppCaches ()
		{
			new AppLabelCache (this.requireContext ().getApplicationContext ()).clear ();
			new ExpiringCache<> (
				this.requireContext ().getApplicationContext (),
				new AppIconCache (this.requireContext ().getApplicationContext ()),
				AppIconCache.EXPIRATION)
				.clear ();
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

		private void initIconPackList ()
		{
			try
			{
				final ListPreference lpIconPack = this.findPreference (
					be.robinj.distrohopper.preferences.Preference.ICON_PACK.getName ());
				if (lpIconPack == null) return;

				final IconPackHelper helper = new IconPackHelper (this.requireContext ().getApplicationContext ());
				final Map<String, ResolveInfo> packs = helper.getIconPacks ();

				final List<CharSequence> entries = new ArrayList<> ();
				final List<CharSequence> entryValues = new ArrayList<> ();

				// Add "None" option
				entries.add (this.getString (R.string.option_icon_pack_none));
				entryValues.add ("");

				final PackageManager pm = this.requireContext ().getPackageManager ();
				for (Map.Entry<String, ResolveInfo> e : packs.entrySet ())
				{
					final String packageName = e.getKey ();
					final CharSequence label = e.getValue ().loadLabel (pm);
					entries.add (label != null ? label : packageName);
					entryValues.add (packageName);
				}

				lpIconPack.setEntries (entries.toArray (new CharSequence[0]));
				lpIconPack.setEntryValues (entryValues.toArray (new CharSequence[0]));

				final SharedPreferences prefs = Preferences.getSharedPreferences (
					this.requireContext (), Preferences.PREFERENCES);
				final String current = prefs.getString (
					be.robinj.distrohopper.preferences.Preference.ICON_PACK.getName (), "");
				if (current == null || current.isEmpty ())
				{
					lpIconPack.setSummary (this.getString (R.string.option_icon_pack_none));
				}
				else
				{
					final ResolveInfo resInf = packs.get (current);
					if (resInf != null)
					{
						final CharSequence label = resInf.loadLabel (pm);
						lpIconPack.setSummary (label != null ? label : current);
					}
					else
					{
						lpIconPack.setSummary (current);
					}
				}

				lpIconPack.setOnPreferenceChangeListener (new Preference.OnPreferenceChangeListener ()
				{
					@Override
					public boolean onPreferenceChange (Preference preference, Object newValue)
					{
						final String value = String.valueOf (newValue);
						if (value == null || value.isEmpty ())
						{
							preference.setSummary (getString (R.string.option_icon_pack_none));
						}
						else
						{
							final ResolveInfo resInf = packs.get (value);
							if (resInf != null)
							{
								final CharSequence label = resInf.loadLabel (pm);
								preference.setSummary (label != null ? label : value);
							}
							else
							{
								preference.setSummary (value);
							}
						}
						// Clear the app icon cache so the newly selected icon pack can take effect
						try
						{
							new AppIconCache (requireContext ().getApplicationContext ()).clear ();
						}
						catch (Exception ex)
						{
							new ExceptionHandler (ex).logAndTrack ();
						}
						return true;
					}
				});
			}
			catch (Exception ex)
			{
				new ExceptionHandler (ex).logAndTrack ();
			}
		}
	}
}

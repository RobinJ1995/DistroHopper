package be.robinj.distrohopper.preferences;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.acra.ACRA;

import be.robinj.distrohopper.AboutActivity;
import be.robinj.distrohopper.BuildConfig;
import be.robinj.distrohopper.ContributeActivity;
import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.IconPackHelper;
import be.robinj.distrohopper.InsetsHelper;
import be.robinj.distrohopper.R;
import be.robinj.distrohopper.cache.AppIconCache;

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
		}

		private void addCategory (final int titleRes, final int prefsRes)
		{
			final PreferenceCategory header = new PreferenceCategory (this.requireContext ());
			header.setTitle (titleRes);
			this.getPreferenceScreen ().addPreference (header);
			this.addPreferencesFromResource (prefsRes);
		}

		private void initCrashReportsPreference ()
		{
			this.applyCrashReportsPreference (BuildConfig.ACRA_CONFIGURED);
		}

		// Package-private so it can be driven with either flag value from tests. //
		void applyCrashReportsPreference (final boolean acraConfigured)
		{
			final Preference crashPref = this.findPreference (
				be.robinj.distrohopper.preferences.Preference.CRASH_REPORTS_ENABLED.getName ());
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

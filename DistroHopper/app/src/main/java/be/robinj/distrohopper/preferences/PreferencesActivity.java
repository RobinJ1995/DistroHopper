package be.robinj.distrohopper.preferences;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.*;
import android.preference.Preference;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;

import java.util.List;

import be.robinj.distrohopper.AboutActivity;
import be.robinj.distrohopper.ContributeActivity;
import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.R;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class PreferencesActivity extends PreferenceActivity
{
	/**
	 * Determines whether to always show the simplified settings UI, where
	 * settings are presented in a single list. When false, settings are shown
	 * as a master/detail two-pane view on tablets. When true, a single pane is
	 * shown on tablets.
	 */
	private static final boolean ALWAYS_SIMPLE_PREFS = true;

	/**
	 * Helper method to determine if the device has an extra-large screen. For
	 * example, 10" tablets are extra-large.
	 */
	private static boolean isXLargeTablet (Context context)
	{
		return (context.getResources ().getConfiguration ().screenLayout
			& Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	/**
	 * Determines whether the simplified settings UI should be shown. This is
	 * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
	 * doesn't have newer APIs like {@link PreferenceFragment}, or the device
	 * doesn't have an extra-large screen. In these cases, a single-pane
	 * "simplified" settings UI should be shown.
	 */
	private static boolean isSimplePreferences (Context context)
	{
		return ALWAYS_SIMPLE_PREFS || ! isXLargeTablet (context);
	}

	@Override
	public SharedPreferences getSharedPreferences (String name, int mode) // I'll decide for myself which file I want to store the preferences in, thanks. //
	{
		return super.getSharedPreferences ("prefs", MODE_PRIVATE);
	}

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate (savedInstanceState);

		this.setupActionBar ();
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi (Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar ()
	{
		// Show the Up button in the action bar.
		this.getActionBar ().setDisplayHomeAsUpEnabled (true);
	}

	@Override
	public boolean onCreateOptionsMenu (Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
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
				// This ID represents the Home or Up button. In the case of this
				// activity, the Up button is shown. Use NavUtils to allow users
				// to navigate up one level in the application structure. For
				// more details, see the Navigation pattern on Android Design:
				//
				// http://developer.android.com/design/patterns/navigation.html#up-vs-back
				//
				// TODO: If Settings has multiple levels, Up should navigate up
				// that hierarchy.
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

	@Override
	protected void onPostCreate (Bundle savedInstanceState)
	{
		super.onPostCreate (savedInstanceState);

		setupSimplePreferencesScreen ();
	}

	/**
	 * Shows the simplified settings UI if the device configuration if the
	 * device configuration dictates that a simplified, single-pane UI should be
	 * shown.
	 */
	private void setupSimplePreferencesScreen ()
	{
		if (! isSimplePreferences (this))
			return;

		// In the simplified UI, fragments are not used at all and we instead
		// use the older PreferenceActivity APIs.

		this.addPreferencesFromResource (R.xml.pref_empty); // Workaround for a shortcoming in Android //

		PreferenceCategory fakeHeader = new PreferenceCategory (this);
		fakeHeader.setTitle (R.string.pref_header_appearance);
		this.getPreferenceScreen ().addPreference (fakeHeader);
		this.addPreferencesFromResource (R.xml.pref_appearance);

		fakeHeader = new PreferenceCategory (this);
		fakeHeader.setTitle (R.string.pref_header_functionality);
		this.getPreferenceScreen ().addPreference (fakeHeader);
		this.addPreferencesFromResource (R.xml.pref_functionality);

		fakeHeader = new PreferenceCategory (this);
		fakeHeader.setTitle (R.string.pref_header_advanced);
		this.getPreferenceScreen ().addPreference (fakeHeader);
		this.addPreferencesFromResource (R.xml.pref_advanced);

		fakeHeader = new PreferenceCategory (this);
		fakeHeader.setTitle (R.string.pref_header_dev);
		this.getPreferenceScreen ().addPreference (fakeHeader);
		this.addPreferencesFromResource (R.xml.pref_dev);

		// Bind the summaries of EditText/List/Dialog/Ringtone preferences to
		// their values. When their values change, their summaries are updated
		// to reflect the new value, per the Android Design guidelines.
		//this.bindPreferenceSummaryToValue (findPreference ("example_text"));
		
		final Activity self = this;
		((android.preference.Preference) this.findPreference ("dummy_customise")).setOnPreferenceClickListener (
			new Preference.OnPreferenceClickListener ()
			{
				@Override
				public boolean onPreferenceClick (Preference preference)
				{
					self.setResult (4);
					self.finish ();
					
					return true;
				}
			}
		);
	}

	@Override
	public boolean onIsMultiPane ()
	{
		return isXLargeTablet (this) && !isSimplePreferences (this);
	}

	@Override
	@TargetApi (Build.VERSION_CODES.HONEYCOMB)
	public void onBuildHeaders (List<Header> target)
	{
		if (! isSimplePreferences (this))
			loadHeadersFromResource (R.xml.pref_headers, target);
	}

	@TargetApi (Build.VERSION_CODES.HONEYCOMB)
	public static class AdvancedPreferenceFragment extends PreferenceFragment
	{
		@Override
		public void onCreate (Bundle savedInstanceState)
		{
			super.onCreate (savedInstanceState);
			addPreferencesFromResource (R.xml.pref_advanced);

			// Bind the summaries of EditText/List/Dialog/Ringtone preferences
			// to their values. When their values change, their summaries are
			// updated to reflect the new value, per the Android Design
			// guidelines.
			//bindPreferenceSummaryToValue (findPreference ("sync_frequency"));
		}
	}

	@TargetApi (Build.VERSION_CODES.HONEYCOMB)
	public static class DevPreferenceFragment extends PreferenceFragment
	{
		@Override
		public void onCreate (Bundle savedInstanceState)
		{
			super.onCreate (savedInstanceState);
			addPreferencesFromResource (R.xml.pref_dev);

			// Bind the summaries of EditText/List/Dialog/Ringtone preferences
			// to their values. When their values change, their summaries are
			// updated to reflect the new value, per the Android Design
			// guidelines.
			//bindPreferenceSummaryToValue (findPreference ("sync_frequency"));
		}
	}

	@TargetApi (Build.VERSION_CODES.HONEYCOMB)
	public static class AppearancePreferenceFragment extends PreferenceFragment
	{
		@Override
		public void onCreate (Bundle savedInstanceState)
		{
			super.onCreate (savedInstanceState);
			addPreferencesFromResource (R.xml.pref_appearance);

			// Bind the summaries of EditText/List/Dialog/Ringtone preferences
			// to their values. When their values change, their summaries are
			// updated to reflect the new value, per the Android Design
			// guidelines.
			//bindPreferenceSummaryToValue (findPreference ("sync_frequency"));
		}
	}

	@TargetApi (Build.VERSION_CODES.HONEYCOMB)
	public static class FunctionalityPreferenceFragment extends PreferenceFragment
	{
		@Override
		public void onCreate (Bundle savedInstanceState)
		{
			super.onCreate (savedInstanceState);
			addPreferencesFromResource (R.xml.pref_functionality);

			// Bind the summaries of EditText/List/Dialog/Ringtone preferences
			// to their values. When their values change, their summaries are
			// updated to reflect the new value, per the Android Design
			// guidelines.
			//bindPreferenceSummaryToValue (findPreference ("sync_frequency"));
		}
	}
}

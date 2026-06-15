package be.robinj.distrohopper.preferences;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.core.app.NavUtils;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.HomeAwareActivity;
import be.robinj.distrohopper.IconPackHelper;
import be.robinj.distrohopper.InsetsHelper;
import be.robinj.distrohopper.R;
import be.robinj.distrohopper.cache.AppIconCache;

/**
 * The dedicated "Icons" settings sub-screen, grouping the icon shape, icon pack
 * and tint (recolour) options. Reached from the Appearance category.
 */
public class IconPreferencesActivity extends HomeAwareActivity
{
	@Override
	protected void onCreate (final Bundle savedInstanceState)
	{
		super.onCreate (savedInstanceState);
		this.setContentView (R.layout.activity_preferences);
		InsetsHelper.applySystemBarsPadding (this);

		if (savedInstanceState == null)
		{
			this.getSupportFragmentManager ()
				.beginTransaction ()
				.replace (R.id.preferences_container, new IconPreferencesFragment ())
				.commit ();
		}

		this.getSupportActionBar ().setDisplayHomeAsUpEnabled (true);
	}

	@Override
	public boolean onOptionsItemSelected (final MenuItem item)
	{
		if (item.getItemId () == android.R.id.home)
		{
			NavUtils.navigateUpFromSameTask (this);
			return true;
		}

		return super.onOptionsItemSelected (item);
	}

	public static class IconPreferencesFragment extends PreferenceFragmentCompat
	{
		@Override
		public void onCreatePreferences (final Bundle savedInstanceState, final String rootKey)
		{
			this.getPreferenceManager ().setSharedPreferencesName (Preferences.PREFERENCES);
			this.setPreferencesFromResource (R.xml.pref_icons, rootKey);

			this.initIconPackList ();
			this.initTintPreferences ();
		}

		/** Hide the tint options entirely on devices that can't recolour icons (pre-API 33). */
		private void initTintPreferences ()
		{
			final SwitchPreferenceCompat tintedSwitch = this.findPreference (
				be.robinj.distrohopper.preferences.Preference.TINTED_ICONS.getName ());
			final Preference tintColour = this.findPreference (
				be.robinj.distrohopper.preferences.Preference.ICON_TINT.getName ());
			if (tintedSwitch == null || tintColour == null)
				return;

			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
			{
				// Remove from their category, since they're nested under it. //
				if (tintedSwitch.getParent () != null)
					tintedSwitch.getParent ().removePreference (tintedSwitch);
				if (tintColour.getParent () != null)
					tintColour.getParent ().removePreference (tintColour);
				return;
			}

			tintColour.setVisible (tintedSwitch.isChecked ());
			tintedSwitch.setOnPreferenceChangeListener ((preference, newValue) ->
			{
				final boolean on = Boolean.TRUE.equals (newValue);
				tintColour.setVisible (on);
				this.clearIconCache ();

				return true;
			});
		}

		private void initIconPackList ()
		{
			try
			{
				final ListPreference lpIconPack = this.findPreference (
					be.robinj.distrohopper.preferences.Preference.ICON_PACK.getName ());
				if (lpIconPack == null) return;

				final IconPackHelper helper =
					new IconPackHelper (this.requireContext ().getApplicationContext ());
				final Map<String, ResolveInfo> packs = helper.getIconPacks ();
				final PackageManager pm = this.requireContext ().getPackageManager ();

				final List<CharSequence> entries = new ArrayList<> ();
				final List<CharSequence> entryValues = new ArrayList<> ();
				entries.add (this.getString (R.string.option_icon_pack_none));
				entryValues.add ("");
				for (final Map.Entry<String, ResolveInfo> e : packs.entrySet ())
				{
					final CharSequence label = e.getValue ().loadLabel (pm);
					entries.add (label != null ? label : e.getKey ());
					entryValues.add (e.getKey ());
				}

				lpIconPack.setEntries (entries.toArray (new CharSequence[0]));
				lpIconPack.setEntryValues (entryValues.toArray (new CharSequence[0]));

				final SharedPreferences prefs = Preferences.getSharedPreferences (this.requireContext ());
				final String current = prefs.getString (
					be.robinj.distrohopper.preferences.Preference.ICON_PACK.getName (), "");
				lpIconPack.setSummary (this.summaryForPack (current, packs, pm));

				lpIconPack.setOnPreferenceChangeListener ((preference, newValue) ->
				{
					preference.setSummary (this.summaryForPack (String.valueOf (newValue), packs, pm));
					this.clearIconCache ();

					return true;
				});
			}
			catch (final Exception ex)
			{
				new ExceptionHandler (ex).logAndTrack ();
			}
		}

		private CharSequence summaryForPack (final String value,
											 final Map<String, ResolveInfo> packs, final PackageManager pm)
		{
			if (value == null || value.isEmpty ())
				return this.getString (R.string.option_icon_pack_none);

			final ResolveInfo resInf = packs.get (value);
			if (resInf != null)
			{
				final CharSequence label = resInf.loadLabel (pm);
				if (label != null)
					return label;
			}

			return value;
		}

		/** Clear the rendered-icon cache so a shape/pack/tint change is reflected on the next load. */
		private void clearIconCache ()
		{
			try
			{
				AppIconCache.clearAll (this.requireContext ().getApplicationContext ());
			}
			catch (final Exception ex)
			{
				new ExceptionHandler (ex).logAndTrack ();
			}
		}
	}
}

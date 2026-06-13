package be.robinj.distrohopper.desktop.dash.lens;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import be.robinj.distrohopper.AppManager;
import be.robinj.distrohopper.Permission;
import be.robinj.distrohopper.R;
import be.robinj.distrohopper.home.SearchLoader;
import be.robinj.distrohopper.preferences.Preference;
import be.robinj.distrohopper.preferences.Preferences;
import be.robinj.distrohopper.thirdparty.ProgressWheel;

/**
 * Created by robin on 5/11/14.
 */
public class LensManager
{
	private Context context;
	private LinkedHashMap<String, Lens> lenses = new LinkedHashMap<> ();
	private List<Lens> enabled;
	private SearchLoader searchLoader;
	private final List<LensSearchResultCollection> results = new ArrayList<> ();
	private CollectionGridAdapter adapter;

	private int maxResultsPerLens = 10;

	private LinearLayout llDashHomeAppsContainer;
	private LinearLayout llDashHomeLensesContainer;
	private ListView lvDashHomeLensResults;
	private ProgressWheel pwDashSearchProgress;

	private final float displayDensity;
	private final int dashIconWidth;

	public LensManager (Context context, LinearLayout llDashHomeAppsContainer, LinearLayout llDashHomeLensesContainer, ProgressWheel pwDashSearchProgress, AppManager apps)
	{
		this.context = context;
		this.enabled = new ArrayList<Lens> ();
		this.llDashHomeAppsContainer = llDashHomeAppsContainer;
		this.llDashHomeLensesContainer = llDashHomeLensesContainer;
		if (llDashHomeLensesContainer != null)
			this.lvDashHomeLensResults = (ListView) llDashHomeLensesContainer.findViewById (R.id.lvDashHomeLensResults);
		this.pwDashSearchProgress = pwDashSearchProgress;

		final SharedPreferences prefs = Preferences.getSharedPreferences(this.context, Preferences.PREFERENCES);
		final SharedPreferences prefsLenses = this.getPrefsLenses();
		this.displayDensity = this.getContext().getResources().getDisplayMetrics().density;
		// Same fallback as every other reader: a stray 80 here made lens
		// results ~50% larger than the dash grid until the preference was
		// first saved //
		this.dashIconWidth = prefs.getInt(Preference.DASHICON_WIDTH.getName(),
			Preference.DASHICON_WIDTH.getDefault());

		if (apps != null)
			context = apps.getContext ();

		this.lenses.put ("DuckDuckGo", new DuckDuckGo (context));
		this.lenses.put ("FDroid", new FDroid (context));
		this.lenses.put ("GitHub", new GitHub (context));
		this.lenses.put ("GooglePlayStore", new GooglePlayStore (context));
		this.lenses.put ("InstalledApps", new InstalledApps (context, apps));
		this.lenses.put ("LocalFiles", new LocalFiles (context)); // LocalFiles needs to show an AlertDialog in some cases, thus it needs the activity's Context (which AppsManager has) instead of the Application Context (this.context). //

		List<String> defaultLenses = new ArrayList<String> ();
		defaultLenses.add ("InstalledApps");
		defaultLenses.add ("LocalFiles");

		List<String> enabledLenses = new ArrayList<>();
		if (prefsLenses.getAll().size() > 0)
		{
			int i = 0;
			String lensName = null;
			while ((lensName = prefsLenses.getString(Integer.toString(i), null)) != null) {
				enabledLenses.add(lensName);
				i++;
			}
		}
		else
		{
			// Default lenses whose permissions haven't been granted (e.g. the
			// wizard's storage prompt was declined) start out disabled; enabling
			// them in the preferences re-requests the permissions //
			for (String lensName : defaultLenses)
			{
				final Lens lens = this.lenses.get (lensName);
				if (lens != null && Permission.missingPermissions (this.context, lens.requiredPermissions ()).length == 0)
					enabledLenses.add (lensName);
			}
		}

		for (String lensName : enabledLenses)
		{
			Lens lens = this.lenses.get (lensName);

			if (lens != null && (! this.isLensEnabled (lens)))
				this.enabled.add (lens);
		}

		this.maxResultsPerLens = Integer.valueOf (prefs.getString (Preference.DASH_SEARCH_LENSES_MAX_RESULTS.getName(), "10"));
	}

	private SharedPreferences getPrefsLenses() {
		return Preferences.getSharedPreferences(this.context, Preferences.LENSES);
	}

	public void disableLens (Lens lens)
	{
		this.disableLens (lens.getClass ().getSimpleName ());
	}

	public void disableLens (String name)
	{
		this.enabled.remove (this.lenses.get (name));

		this.saveEnabledLenses ();
	}

	public void enableLens (Lens lens)
	{
		this.enableLens (lens.getClass ().getSimpleName ());
	}

	public void enableLens (String name)
	{
		final Lens lens = this.lenses.get(name);
		if (lens != null && ! this.enabled.contains(lens))
			this.enabled.add (lens);

		this.saveEnabledLenses ();
	}

	public Context getContext ()
	{
		return this.context;
	}

	public HashMap<String, Lens> getAvailableLenses ()
	{
		return this.lenses;
	}

	public List<Lens> getEnabledLenses ()
	{
		return this.enabled;
	}

	public int getMaxResultsPerLens ()
	{
		return this.maxResultsPerLens;
	}

	public boolean isLensEnabled (Lens lens)
	{
		return this.isLensEnabled (lens.getClass ().getSimpleName ());
	}

	public boolean isLensEnabled (String name)
	{
		Lens lens = this.lenses.get (name);

		return this.enabled.contains (lens);
	}

	public void saveEnabledLenses ()
	{
		final SharedPreferences prefsLenses = this.getPrefsLenses();
		final SharedPreferences.Editor editor = prefsLenses.edit();

		editor.clear();

		for (int i = 0; i < this.enabled.size (); i++) {
			editor.putString(Integer.toString(i), this.enabled.get(i).getClass().getSimpleName());
		}

		editor.apply();
	}

	public void showAppsContainer ()
	{
		this.llDashHomeAppsContainer.setVisibility (View.VISIBLE);
		this.llDashHomeLensesContainer.setVisibility (View.GONE);
		this.pwDashSearchProgress.setVisibility (View.GONE);
	}

	public void showLensesContainer ()
	{
		this.llDashHomeAppsContainer.setVisibility (View.GONE);
		this.llDashHomeLensesContainer.setVisibility (View.VISIBLE);
	}

	public void sortEnabledLenses (List<Lens> reference)
	{
		// Remove all lenses and add them again in the right order //
		// this.enabled = new ArrayList<Lens> () would break existing references //
		List<Lens> oldEnabled = new ArrayList<Lens> ();

		for (Lens lens : this.enabled)
			oldEnabled.add (lens);

		this.enabled.clear ();

		for (Lens lens : reference)
		{
			if (oldEnabled.contains (lens))
				this.enabled.add (lens);
		}
	}

	/**
	 * Injected by HomeActivity once the activity exists (it owns the
	 * lifecycleScope and dispatchers the loader runs on).
	 */
	public void setSearchLoader (SearchLoader searchLoader)
	{
		this.searchLoader = searchLoader;
	}

	public void startSearch (String pattern)
	{
		if (this.searchLoader != null)
			this.searchLoader.cancel ();

		if (! pattern.equals (""))
		{
			this.showLensesContainer ();

			// Fresh adapter + backing list per query; the loader appends collections
			// to this.results and notifies the adapter as lenses complete //
			this.results.clear ();
			this.adapter = new CollectionGridAdapter (this.getContext (), this.results,
					this.displayDensity, this.dashIconWidth);
			this.lvDashHomeLensResults.setAdapter (this.adapter);

			if (this.searchLoader != null)
				this.searchLoader.start (pattern, this.enabled, this.maxResultsPerLens,
						this.adapter, this.results, this.pwDashSearchProgress);
		}
		else
		{
			this.showAppsContainer ();
		}
	}
}

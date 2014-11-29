package be.robinj.ubuntu.unity.dash.lens;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import be.robinj.ubuntu.AppManager;
import be.robinj.ubuntu.R;
import be.robinj.ubuntu.thirdparty.ProgressWheel;

/**
 * Created by robin on 5/11/14.
 */
public class LensManager
{
	private Context context;
	private HashMap<String, Lens> lenses = new HashMap<String, Lens> ();
	private List<Lens> enabled;
	private AsyncSearch asyncSearch;

	private int maxResultsPerLens = 10;

	private LinearLayout llDashHomeAppsContainer;
	private LinearLayout llDashHomeLensesContainer;
	private ListView lvDashHomeLensResults;
	private ProgressWheel pwDashSearchProgress;

	public LensManager (Context context, LinearLayout llDashHomeAppsContainer, LinearLayout llDashHomeLensesContainer, ProgressWheel pwDashSearchProgress, AppManager apps)
	{
		this.context = context;
		this.enabled = new ArrayList<Lens> ();
		this.llDashHomeAppsContainer = llDashHomeAppsContainer;
		this.llDashHomeLensesContainer = llDashHomeLensesContainer;
		if (llDashHomeAppsContainer != null)
			this.lvDashHomeLensResults = (ListView) llDashHomeLensesContainer.findViewById (R.id.lvDashHomeLensResults);
		this.pwDashSearchProgress = pwDashSearchProgress;

		SharedPreferences prefs = this.context.getSharedPreferences ("prefs", Context.MODE_PRIVATE);
		SharedPreferences prefsLenses = this.context.getSharedPreferences ("lenses", Context.MODE_PRIVATE);

		this.lenses.put ("AskUbuntu", new AskUbuntu (context));
		this.lenses.put ("DuckDuckGo", new DuckDuckGo (context));
		this.lenses.put ("GitHub", new GitHub (context));
		this.lenses.put ("GooglePlus", new GooglePlus (context));
		this.lenses.put ("InstalledApps", new InstalledApps (context, apps));
		this.lenses.put ("LocalFiles", new LocalFiles (apps != null ? apps.getContext () : context)); // LocalFiles needs to show an AlertDialog in some cases, thus it needs the activity's Context (which AppsManager has) in stead of the Application Context (this.context). //
		this.lenses.put ("Reddit", new Reddit (context));
		this.lenses.put ("ServerFault", new ServerFault (context));
		this.lenses.put ("StackOverflow", new StackOverflow (context));
		this.lenses.put ("SuperUser", new SuperUser (context));

		List<String> defaultLenses = new ArrayList<String> ();
		defaultLenses.add ("InstalledApps");
		defaultLenses.add ("LocalFiles");

		for (String key : this.lenses.keySet ())
		{
			if (prefsLenses.getBoolean (key + "_enabled", defaultLenses.contains (key)))
				this.enabled.add (this.lenses.get (key));
		}

		this.maxResultsPerLens = Integer.valueOf (prefs.getString ("dashsearch_lenses_maxresults", "10"));
	}

	public void disableLens (Lens lens)
	{
		this.disableLens (lens.getClass ().getSimpleName ());
	}

	public void disableLens (String name)
	{
		SharedPreferences prefsLenses = this.context.getSharedPreferences ("lenses", Context.MODE_PRIVATE);

		SharedPreferences.Editor editor = prefsLenses.edit ();
		editor.putBoolean (name + "_enabled", false);

		if (Build.VERSION.SDK_INT >= 9)
			editor.apply ();
		else
			editor.commit ();

		this.enabled.remove (this.lenses.get (name));
	}

	public void enableLens (Lens lens)
	{
		this.enableLens (lens.getClass ().getSimpleName ());
	}

	public void enableLens (String name)
	{
		SharedPreferences prefsLenses = this.context.getSharedPreferences ("lenses", Context.MODE_PRIVATE);

		SharedPreferences.Editor editor = prefsLenses.edit ();
		editor.putBoolean (name + "_enabled", true);

		if (Build.VERSION.SDK_INT >= 9)
			editor.apply ();
		else
			editor.commit ();

		this.enabled.add (this.lenses.get (name));
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

	public void startSearch (String pattern)
	{
		if (this.asyncSearch != null)
			this.asyncSearch.cancel (true);

		if (! pattern.equals (""))
		{
			this.asyncSearch = new AsyncSearch (this, this.pwDashSearchProgress, this.lvDashHomeLensResults);
			this.asyncSearch.execute (pattern);
		}
		else
		{
			this.showAppsContainer ();
		}
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
		this.pwDashSearchProgress.setVisibility (View.VISIBLE);
	}
}

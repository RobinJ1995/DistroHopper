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
import java.util.LinkedHashMap;
import java.util.List;

import be.robinj.ubuntu.App;
import be.robinj.ubuntu.AppManager;
import be.robinj.ubuntu.R;
import be.robinj.ubuntu.thirdparty.ProgressWheel;

/**
 * Created by robin on 5/11/14.
 */
public class LensManager
{
	private Context context;
	private LinkedHashMap<String, Lens> lenses = new LinkedHashMap<String, Lens> ();
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

		if (apps != null)
			context = apps.getContext ();

		this.lenses.put ("AskUbuntu", new AskUbuntu (context));
		this.lenses.put ("DuckDuckGo", new DuckDuckGo (context));
		this.lenses.put ("GitHub", new GitHub (context));
		this.lenses.put ("GooglePlus", new GooglePlus (context));
		this.lenses.put ("InstalledApps", new InstalledApps (context, apps));
		this.lenses.put ("LocalFiles", new LocalFiles (context)); // LocalFiles needs to show an AlertDialog in some cases, thus it needs the activity's Context (which AppsManager has) in stead of the Application Context (this.context). //
		this.lenses.put ("Reddit", new Reddit (context));
		this.lenses.put ("ServerFault", new ServerFault (context));
		this.lenses.put ("StackOverflow", new StackOverflow (context));
		this.lenses.put ("SuperUser", new SuperUser (context));

		List<String> defaultLenses = new ArrayList<String> ();
		defaultLenses.add ("InstalledApps");
		defaultLenses.add ("LocalFiles");

		int i = 0;
		String lensName;
		while ((lensName = prefsLenses.getString (Integer.toString (i), null)) != null)
		{
			Lens lens = this.lenses.get (lensName);

			if (lens != null && (! this.isLensEnabled (lens)))
				this.enabled.add (lens);

			i++;
		}

		this.maxResultsPerLens = Integer.valueOf (prefs.getString ("dashsearch_lenses_maxresults", "10"));
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
		if (! this.isLensEnabled (name))
			this.enabled.add (this.lenses.get (name));

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
		SharedPreferences prefsLenses = this.context.getSharedPreferences ("lenses", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefsLenses.edit ();

		int i = 0;
		String lensName;
		while ((lensName = prefsLenses.getString (Integer.toString (i), null)) != null)
		{
			editor.remove (Integer.toString (i));

			i++;
		}

		for (int j = 0; j < this.enabled.size (); j++)
			editor.putString (Integer.toString (j), this.enabled.get (j).getClass ().getSimpleName ());

		if (Build.VERSION.SDK_INT >= 9)
			editor.apply ();
		else
			editor.commit ();
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
}

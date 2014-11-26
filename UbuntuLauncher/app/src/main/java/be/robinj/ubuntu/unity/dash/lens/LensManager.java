package be.robinj.ubuntu.unity.dash.lens;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
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
		this.lvDashHomeLensResults = (ListView) llDashHomeLensesContainer.findViewById (R.id.lvDashHomeLensResults);
		this.pwDashSearchProgress = pwDashSearchProgress;

		SharedPreferences prefs = this.context.getSharedPreferences ("prefs", Context.MODE_PRIVATE);

		this.enabled.add (new InstalledApps (context, apps));
		this.enabled.add (new LocalFiles (apps.getContext ())); // LocalFiles needs to show an AlertDialog in some cases, thus it needs the activity's Context (which AppsManager has) in stead of the Application Context (this.context). //

		this.maxResultsPerLens = Integer.valueOf (prefs.getString ("dashsearch_lenses_maxresults", "10"));
	}

	public Context getContext ()
	{
		return this.context;
	}

	public List<Lens> getEnabledLenses ()
	{
		return this.enabled;
	}

	public int getMaxResultsPerLens ()
	{
		return this.maxResultsPerLens;
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

	public List<LensSearchResultCollection> search (String pattern) throws IOException, JSONException
	//public List<LensSearchResult> search (String pattern) throws IOException, JSONException
	{
		return this.search (pattern, false);
	}

	// No longer used. Replaced by AsyncSearch.doInBackground. //
	public List<LensSearchResultCollection> search (String pattern, boolean showResults) throws IOException, JSONException
	//public List<LensSearchResult> search (String pattern, boolean showResults) throws IOException, JSONException
	{
		List<LensSearchResultCollection> results = new ArrayList<LensSearchResultCollection> ();
		//List<LensSearchResult> results = new ArrayList<LensSearchResult> ();

		if (pattern.length () == 0)
		{
			this.llDashHomeAppsContainer.setVisibility (View.VISIBLE);
			this.llDashHomeLensesContainer.setVisibility (View.GONE);
		}
		else
		{
			this.llDashHomeAppsContainer.setVisibility (View.GONE);
			this.llDashHomeLensesContainer.setVisibility (View.VISIBLE);

			for (Lens lens : this.enabled)
			{
				List<LensSearchResult> lensResults = lens.search (pattern);

				if (lensResults != null && lensResults.size () > 0)
				{
					lensResults = lensResults.subList (0, lensResults.size () > this.maxResultsPerLens ? this.maxResultsPerLens : lensResults.size ());

					LensSearchResultCollection collection = new LensSearchResultCollection (lens, lensResults);
					results.add (collection);

					//results.addAll (lensResults);
				}
			}
		}

		if (showResults)
		{
			this.lvDashHomeLensResults.setAdapter (new be.robinj.ubuntu.unity.dash.lens.CollectionGridAdapter (this.context, results));
		}

		return results;
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

package be.robinj.ubuntu.unity.dash.lens;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.GridView;
import android.widget.LinearLayout;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.robinj.ubuntu.R;

/**
 * Created by robin on 5/11/14.
 */
public class LensManager
{
	private Context context;
	private List<Lens> enabled;

	private int maxResultsPerLens = 5;

	private LinearLayout gvDashHomeLensesContainer;
	private GridView gvDashHomeLenses;

	public LensManager (Context context, LinearLayout gvDashHomeLensesContainer)
	{
		this.context = context;
		this.enabled = new ArrayList<Lens> ();
		this.gvDashHomeLensesContainer = gvDashHomeLensesContainer;
		this.gvDashHomeLenses = (GridView) gvDashHomeLensesContainer.findViewById (R.id.gvDashHomeLenses);

		SharedPreferences prefs = this.context.getSharedPreferences ("prefs", Context.MODE_PRIVATE);
		if (prefs.getBoolean ("dashsearch_lenses", false))
		{
			this.enabled.add (new DuckDuckGo (context));
			this.enabled.add (new SuperUser (context));
		}

		this.maxResultsPerLens = Integer.valueOf (prefs.getString ("dashsearch_lenses_maxresults", "5"));
	}

	//public List<LensSearchResultCollection> search (String pattern) throws IOException, JSONException
	public List<LensSearchResult> search (String pattern) throws IOException, JSONException
	{
		return this.search (pattern, false);
	}

	//public List<LensSearchResultCollection> search (String pattern, boolean showResults) throws IOException, JSONException
	public List<LensSearchResult> search (String pattern, boolean showResults) throws IOException, JSONException
	{
		//List<LensSearchResultCollection> results = new ArrayList<LensSearchResultCollection> ();
		List<LensSearchResult> results = new ArrayList<LensSearchResult> ();

		if (pattern.length () == 0)
		{
			this.gvDashHomeLensesContainer.setVisibility (View.GONE);
		}
		else
		{
			this.gvDashHomeLensesContainer.setVisibility (View.VISIBLE);

			for (Lens lens : this.enabled)
			{
				List<LensSearchResult> lensResults = lens.search (pattern);
				lensResults = lensResults.subList (0, lensResults.size () > this.maxResultsPerLens ? this.maxResultsPerLens : lensResults.size ());

				//LensSearchResultCollection collection = new LensSearchResultCollection (lens, lensResults);
				//results.add (collection);

				results.addAll (lensResults);
			}
		}

		if (showResults)
		{
			this.gvDashHomeLenses.setAdapter (new be.robinj.ubuntu.unity.dash.lens.GridAdapter (this.context, results));
		}

		return results;
	}
}

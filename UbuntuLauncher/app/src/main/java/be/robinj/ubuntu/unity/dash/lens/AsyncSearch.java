package be.robinj.ubuntu.unity.dash.lens;

import android.os.AsyncTask;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import be.robinj.ubuntu.thirdparty.ProgressWheel;

/**
 * Created by robin on 25/11/14.
 */
public class AsyncSearch extends AsyncTask<String, Integer, Object[]>
{
	private LensManager lensManager;
	private List<LensSearchResultCollection> results = new ArrayList<LensSearchResultCollection> ();
	private CollectionGridAdapter adapter;

	private ProgressWheel progressWheel;
	private ListView lvDashHomeLensResults;

	public AsyncSearch (LensManager lensManager, ProgressWheel progressWheel, ListView lvDashHomeLensResults)
	{
		this.lensManager = lensManager;

		this.progressWheel = progressWheel;
		this.lvDashHomeLensResults = lvDashHomeLensResults;
	}

	@Override
	protected void onPreExecute ()
	{
		super.onPreExecute ();

		this.lensManager.showLensesContainer ();
		this.progressWheel.setVisibility (View.VISIBLE);

		this.adapter = new be.robinj.ubuntu.unity.dash.lens.CollectionGridAdapter (this.lensManager.getContext (), this.results);
		this.lvDashHomeLensResults.setAdapter (this.adapter);
	}

	@Override
	protected Object[] doInBackground (String... params)
	{
		String pattern = params[0];
		this.results.clear ();

		if (pattern.length () > 0)
		{
			List<Lens> lenses = this.lensManager.getEnabledLenses ();
			int nLenses = lenses.size ();
			int maxResultsPerLens = lensManager.getMaxResultsPerLens ();

			this.publishProgress (0, nLenses);

			for (int i = 0; i < nLenses; i++)
			{
				if (this.isCancelled ())
					return null;

				Lens lens = lenses.get (i);
				List<LensSearchResult> lensResults = null;
				LensSearchResultCollection collection = null;

				try
				{
					lensResults = lens.search (pattern);

					if (lensResults.size () > 0)
					{
						lensResults = lensResults.subList (0, lensResults.size () > maxResultsPerLens ? maxResultsPerLens : lensResults.size ());

						collection = new LensSearchResultCollection (lens, lensResults);
					}
				}
				catch (Exception ex)
				{
					collection = new LensSearchResultCollection (lens, ex);
				}

				if (collection != null)
					this.results.add (collection);

				this.publishProgress (i, nLenses);
			}
		}

		return new Object[] { this.results };
	}

	@Override
	protected void onProgressUpdate (Integer... progress)
	{
		super.onProgressUpdate (progress[0]);

		this.progressWheel.setProgress ((int) ((float) progress[0] / (float) progress[1] * 360));

		this.progressWheel.invalidate ();
		this.adapter.notifyDataSetChanged ();
	}

	@Override
	protected void onPostExecute (Object[] result)
	{
		super.onPostExecute (result);

		this.progressWheel.setVisibility (View.GONE);
	}

	@Override
	protected void onCancelled ()
	{
		super.onCancelled ();

		this.progressWheel.setProgress (0);
	}
}
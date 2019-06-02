package be.robinj.distrohopper.desktop.dash.lens;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import be.robinj.distrohopper.thirdparty.ProgressWheel;

import static be.robinj.distrohopper.Utils.runOnUiThread;

/**
 * Created by robin on 25/11/14.
 */
public class AsyncSearch extends AsyncTask<String, AsyncSearch.AsyncSearchProgressUpdate, List<LensSearchResultCollection>>
{
	private final static int PROGRESS_WHEEL_DELAY = 240;

	private LensManager lensManager;
	private List<LensSearchResultCollection> results = new ArrayList<> ();
	private CollectionGridAdapter adapter;

	private ProgressWheel progressWheel;
	private ListView lvDashHomeLensResults;

	private volatile boolean finished = false;

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

		final Runnable setProgressWheelVisible = () -> {
			try {
				Thread.sleep(PROGRESS_WHEEL_DELAY);
			} catch (final InterruptedException e) {
				return;
			}

			runOnUiThread(() -> {
				if (this.finished || this.isCancelled()) {
					return;
				}

				this.progressWheel.setVisibility (View.VISIBLE);
			});
		};
		new Thread(setProgressWheelVisible).start();

		this.adapter = new be.robinj.distrohopper.desktop.dash.lens.CollectionGridAdapter (this.lensManager.getContext (), this.results);
		this.lvDashHomeLensResults.setAdapter (this.adapter);
	}

	@Override
	protected List<LensSearchResultCollection> doInBackground (String... params)
	{
		String pattern = params[0];
		this.results.clear ();

		if (pattern.length () > 0)
		{
			List<Lens> lenses = this.lensManager.getEnabledLenses ();
			int nLenses = lenses.size ();
			int maxResultsPerLens = lensManager.getMaxResultsPerLens ();

			this.publishProgress (new AsyncSearchProgressUpdate (null, 0, nLenses));

			for (int i = 0; i < nLenses; i++)
			{
				if (this.isCancelled ())
					return null;

				Lens lens = lenses.get (i);
				List<LensSearchResult> lensResults = null;
				LensSearchResultCollection collection = null;

				try
				{
					lensResults = lens.search (pattern, maxResultsPerLens);

					if (lensResults.size () > 0)
					{
						if (lensResults.size() > maxResultsPerLens) {
							lensResults = lensResults.subList(0, maxResultsPerLens);
						}

						collection = new LensSearchResultCollection (lens, lensResults);
					}
				}
				catch (Exception ex)
				{
					collection = new LensSearchResultCollection (lens, ex);
				}

				this.publishProgress (new AsyncSearchProgressUpdate (collection, i + 1, nLenses));
			}
		}

		return this.results;
	}

	@Override
	protected void onProgressUpdate (AsyncSearchProgressUpdate... progressUpdate)
	{
		super.onProgressUpdate (progressUpdate);

		AsyncSearchProgressUpdate update = progressUpdate[0];

		this.progressWheel.setProgress ((int) ((float) update.getProgress () / (float) update.getNLenses () * 360));

		LensSearchResultCollection collection = update.getCollection ();
		if (collection != null)
		{
			this.results.add (collection);
			this.adapter.notifyDataSetChanged ();
		}
	}

	@Override
	protected void onPostExecute (List<LensSearchResultCollection> results)
	{
		this.finished = true;

		super.onPostExecute (results);

		this.progressWheel.setVisibility (View.GONE);
	}

	@Override
	protected void onCancelled ()
	{
		super.onCancelled ();

		this.progressWheel.setProgress (0);
	}

	protected class AsyncSearchProgressUpdate
	{
		private LensSearchResultCollection collection;
		private int progress;
		private int nLenses;

		public AsyncSearchProgressUpdate (LensSearchResultCollection collection, int progress, int nLenses)
		{
			this.collection = collection;
			this.progress = progress;
			this.nLenses = nLenses;
		}

		public LensSearchResultCollection getCollection ()
		{
			return this.collection;
		}

		public int getProgress ()
		{
			return this.progress;
		}

		public int getNLenses ()
		{
			return this.nLenses;
		}
	}
}
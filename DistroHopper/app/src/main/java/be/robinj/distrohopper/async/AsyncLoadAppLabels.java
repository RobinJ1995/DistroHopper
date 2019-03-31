package be.robinj.distrohopper.async;

import android.os.AsyncTask;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.AppManager;
import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.cache.ICache;
import be.robinj.distrohopper.dev.Log;

public class AsyncLoadAppLabels extends AsyncTask<ICache<String>, Integer, Integer>
{
	private final AppManager appManager;

	public AsyncLoadAppLabels(final AppManager appManager)
	{
		this.appManager = appManager;
	}

	@Override
	protected Integer doInBackground (ICache<String>... params)
	{
		final ICache<String> appLabelCache = params[0];
		int n = 0;

		long tStart = System.currentTimeMillis ();

		for (final App app : this.appManager.getInstalledApps()) {
			if (!app.isLabelLoaded()) {
				final String label = app.getLabel(false);
				n += app.setLabel(label, appLabelCache) ? 1 : 0;
			} else if (!appLabelCache.containsKey(app.getPackageAndActivityName())) {
				appLabelCache.put(app.getPackageAndActivityName(), app.getLabel());
				n += 1;
			}
		}

		long tDoneCachingAppLabels = System.currentTimeMillis ();
		long tdCachingAppLabels = tDoneCachingAppLabels - tStart;

		Log.getInstance().v(this.getClass().getSimpleName(), n + " app labels cached in " + tdCachingAppLabels + "ms.");

		if (this.isCancelled ()) {
			return null;
		} else if (n == 0) {
			return n;
		}

		this.appManager.sort ();

		long tDoneSortingInstalledApps = System.currentTimeMillis ();
		long tdSortingInstalledApps = tDoneSortingInstalledApps - tDoneCachingAppLabels;
		Log.getInstance ().v (this.getClass ().getSimpleName (), "Sorted " + this.appManager.size() + " apps in " + tdSortingInstalledApps + "ms.");

		return n;
	}

	@Override
	protected void onPostExecute (Integer n)
	{
		try
		{
			if (this.isCancelled()) {
				return;
			}

			appManager.asyncLoadAppLabelsDone();
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.logAndTrack();
		}
	}
}

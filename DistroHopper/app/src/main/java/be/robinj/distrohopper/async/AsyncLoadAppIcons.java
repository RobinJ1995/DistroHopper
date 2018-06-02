package be.robinj.distrohopper.async;

import android.os.AsyncTask;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.AppManager;
import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.cache.DrawableCache;
import be.robinj.distrohopper.cache.StringCache;
import be.robinj.distrohopper.desktop.AppIcon;
import be.robinj.distrohopper.dev.Log;

public class AsyncLoadAppIcons extends AsyncTask<DrawableCache, Integer, Integer>
{
	private final AppManager appManager;

	public AsyncLoadAppIcons(final AppManager appManager)
	{
		this.appManager = appManager;
	}

	@Override
	protected Integer doInBackground (DrawableCache... params)
	{
		final DrawableCache appIconCache = params[0];
		int n = 0;

		long tStart = System.currentTimeMillis ();

		for (final App app : this.appManager.getInstalledApps()) {
			if (!app.isIconLoaded()) {
				final AppIcon icon = app.getIcon(false);
				n += app.setIcon(icon, appIconCache) ? 1 : 0;
			} else if (!appIconCache.containsKey(app.getPackageAndActivityName())) {
				appIconCache.put(app.getPackageAndActivityName(), app.getIcon().getDrawable());
				n += 1;
			}
		}

		long tDoneCachingAppIcons = System.currentTimeMillis ();
		long tdCachingAppIcons = tDoneCachingAppIcons - tStart;

		Log.getInstance().v(this.getClass().getSimpleName(), n + " app icons cached in " + tdCachingAppIcons + "ms.");

		return n;
	}

	@Override
	protected void onPostExecute (Integer n)
	{
		try {
			if (this.isCancelled()) {
				return;
			}

			appManager.asyncLoadAppIconsDone();
		} catch (Exception ex) {
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.logAndTrack();
		}
	}
}

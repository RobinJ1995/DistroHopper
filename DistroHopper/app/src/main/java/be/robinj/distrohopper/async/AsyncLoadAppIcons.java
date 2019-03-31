package be.robinj.distrohopper.async;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import java.util.HashMap;
import java.util.Map;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.AppManager;
import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.cache.ICache;
import be.robinj.distrohopper.desktop.AppIcon;
import be.robinj.distrohopper.dev.Log;

public class AsyncLoadAppIcons extends AsyncTask<ICache<Drawable>, Integer, Integer>
{
	private final AppManager appManager;

	public AsyncLoadAppIcons(final AppManager appManager)
	{
		this.appManager = appManager;
	}

	@Override
	protected Integer doInBackground (ICache<Drawable>... params)
	{
		final ICache<Drawable> appIconCache = params[0];
		int n = 0;

		long tStart = System.currentTimeMillis ();

		final Map<String, Drawable> populateIconCache = new HashMap<>();

		for (final App app : this.appManager.getInstalledApps()) {
			if (!appIconCache.containsKey(app.getPackageAndActivityName())) {
				populateIconCache.put(app.getPackageAndActivityName(), app.getIcon().getDrawable());
				n += 1;
			}
		}

		appIconCache.putAll(populateIconCache);

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

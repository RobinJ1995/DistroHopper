package be.robinj.distrohopper.async;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.View;
import android.widget.GridView;

import java.util.List;
import java.util.Map;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.AppManager;
import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.HomeActivity;
import be.robinj.distrohopper.cache.ICache;
import be.robinj.distrohopper.dev.Log;
import be.robinj.distrohopper.preferences.Preferences;
import be.robinj.distrohopper.thirdparty.ProgressWheel;
import be.robinj.distrohopper.desktop.dash.AppLauncherClickListener;
import be.robinj.distrohopper.desktop.dash.AppLauncherLongClickListener;
import be.robinj.distrohopper.desktop.dash.GridAdapter;
import be.robinj.distrohopper.desktop.launcher.SpinnerAppLauncher;

/**
 * Created by robin on 8/21/14.
 */
public class AsyncLoadApps extends AsyncTask<Context, Integer, AppManager>
{
	private final SpinnerAppLauncher lalSpinner;
	private final be.robinj.distrohopper.desktop.launcher.AppLauncher lalBfb;
	private final GridView gvDashHomeApps;
	private final HomeActivity parent;
	private Context context;

	private ICache<Drawable> appIconCache;
	private ICache<String> appLabelCache;
	
	private static final String[] IGNORE = {"be.robinj.distrohopper", "be.robinj.ubuntu"}; // Inception //

	public AsyncLoadApps (HomeActivity parent, SpinnerAppLauncher lalSpinner,
						  be.robinj.distrohopper.desktop.launcher.AppLauncher lalBfb,
						  GridView gvDashHomeApps, ICache<Drawable> appIconCache,
						  ICache<String> appLabelCache)
	{
		this.parent = parent;
		this.lalSpinner = lalSpinner;
		this.lalBfb = lalBfb;
		this.gvDashHomeApps = gvDashHomeApps;
		this.appIconCache = appIconCache;
		this.appLabelCache = appLabelCache;
	}

	@Override
	protected AppManager doInBackground (Context... params)
	{
		AppManager appManager = null;

		try
		{
			this.context = params[0];

			appManager = new AppManager (this.parent);
			final SharedPreferences prefsPinned = Preferences.getSharedPreferences(this.context, Preferences.PINNED_APPS);

			/*try
			{
				SharedPreferences prefs = this.context.getSharedPreferences ("prefs", Context.MODE_PRIVATE);
				String iconPack = prefs.getString ("iconpack", null);

				if (iconPack != null)
					appManager.loadIconPack ("com.numix.icons_circle");
			}
			catch (Exception ex)
			{
				ex.printStackTrace ();
			}*/

			long tStart = System.currentTimeMillis ();
			
			List<ResolveInfo> resInfs = appManager.queryInstalledApps ();
			int size = resInfs.size ();
			this.publishProgress (0, 3);

			if (this.isCancelled ())
				return null;

			for (int i = 0; i < size; i++)
			{
				final ResolveInfo resInf = resInfs.get (i);
				boolean skip = false;
				
				for (int j = 0; j < this.IGNORE.length; j++)
				{
					if (this.IGNORE[j].equals (resInf.activityInfo.packageName))
						skip = true;
				}
				if (skip)
					continue;
				
				final App app = new App(this.context, appManager, resInf, this.appLabelCache, this.appIconCache);
				appManager.add (app, false, false);
			}

			long tDoneRetrievingInstalledApps = System.currentTimeMillis ();
			long tdRetrievingInstalledApps = tDoneRetrievingInstalledApps - tStart;
			Log.getInstance ().v (this.getClass ().getSimpleName (), "Retrieved " + size + " apps from package manager in " + tdRetrievingInstalledApps + "ms.");

			this.publishProgress (1, 3);

			if (this.isCancelled ())
				return null;

			appManager.sort ();

			long tDoneSortingInstalledApps = System.currentTimeMillis ();
			long tdSortingInstalledApps = tDoneSortingInstalledApps - tDoneRetrievingInstalledApps;
			Log.getInstance ().v (this.getClass ().getSimpleName (), "Sorted " + size + " apps in " + tdSortingInstalledApps + "ms.");

			this.publishProgress (2, 3);

			if (this.isCancelled ())
				return null;

			final int nPinned = prefsPinned.getAll().size();
			if (nPinned > 0)
			{
				final Map<String, App> appMap = appManager.getInstalledAppsMap();

				int i = 0;
				String packageAndActivityName = null;
				while ((packageAndActivityName = prefsPinned.getString(Integer.toString(i++), null)) != null) {
					final App pinnedApp  = appMap.get(packageAndActivityName);

					if (pinnedApp == null) {
						continue;
					}

					appManager.pin(pinnedApp, false, false, false);
				}
			}

			long tDoneFilteringPinnedApps = System.currentTimeMillis ();
			long tdFilteringPinnedApps = tDoneFilteringPinnedApps - tDoneSortingInstalledApps;
			Log.getInstance ().v (this.getClass ().getSimpleName (), "Loaded " + nPinned + " pinned apps in " + tdFilteringPinnedApps + "ms.");

			this.publishProgress (3, 3);
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this.parent);
		}

		return appManager;
	}

	@Override
	protected void onProgressUpdate (Integer... progress)
	{
		super.onProgressUpdate (progress[0]);

		ProgressWheel pw = this.lalSpinner.getProgressWheel ();
		pw.setProgress (progress[0] / progress[1] * 360);
	}

	@Override
	protected void onPostExecute (AppManager appManager)
	{
		try {
			if (this.isCancelled()) {
				return;
			}

			this.lalSpinner.setVisibility (View.GONE);
			this.lalBfb.setVisibility (View.VISIBLE);
			
			appManager.refreshPinnedView ();
			
			this.gvDashHomeApps.setAdapter (new GridAdapter (this.context, appManager.getInstalledApps ()));
			this.gvDashHomeApps.setOnItemClickListener (new AppLauncherClickListener (this.parent));
			this.gvDashHomeApps.setOnItemLongClickListener (new AppLauncherLongClickListener (this.parent));
			
			this.parent.asyncLoadInstalledAppsDone (appManager);
		} catch (Exception ex) {
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this.parent);
		}
	}
}

package be.robinj.distrohopper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.view.View;
import android.widget.GridView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.robinj.distrohopper.dev.Log;
import be.robinj.distrohopper.thirdparty.ProgressWheel;
import be.robinj.distrohopper.desktop.dash.AppLauncherClickListener;
import be.robinj.distrohopper.desktop.dash.AppLauncherLongClickListener;
import be.robinj.distrohopper.desktop.dash.GridAdapter;
import be.robinj.distrohopper.desktop.launcher.SpinnerAppLauncher;

/**
 * Created by robin on 8/21/14.
 */
public class AsyncLoadApps extends AsyncTask<Context, Float, AppManager>
{
	private SpinnerAppLauncher lalSpinner;
	private be.robinj.distrohopper.desktop.launcher.AppLauncher lalBfb;
	private GridView gvDashHomeApps;
	private HomeActivity parent;
	private Context context;
	
	private static final String[] IGNORE = {"be.robinj.distrohopper", "be.robinj.ubuntu"}; // Inception //

	public AsyncLoadApps (HomeActivity parent, SpinnerAppLauncher lalSpinner, be.robinj.distrohopper.desktop.launcher.AppLauncher lalBfb, GridView gvDashHomeApps)
	{
		this.parent = parent;
		this.lalSpinner = lalSpinner;
		this.lalBfb = lalBfb;
		this.gvDashHomeApps = gvDashHomeApps;
	}

	@Override
	protected AppManager doInBackground (Context... params)
	{
		AppManager appManager = null;

		try
		{
			this.context = params[0];

			appManager = new AppManager (this.parent);
			final SharedPreferences prefsPinned = this.context.getSharedPreferences ("pinned", Context.MODE_PRIVATE);

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

			long tStartRetrievingInstalledApps = System.currentTimeMillis ();
			
			List<ResolveInfo> resInfs = appManager.queryInstalledApps ();
			int size = resInfs.size ();
			this.publishProgress (0F, 3F);

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
				
				final App app = new App(this.context, appManager, resInf);
				appManager.add (app, false, false);
			}

			long tDoneRetrievingInstalledApps = System.currentTimeMillis ();
			float tdRetrievingInstalledApps = (float) (tDoneRetrievingInstalledApps - tStartRetrievingInstalledApps) / 1000F;

			Log.getInstance ().v (this.getClass ().getSimpleName (), "Data about " + size + " installed apps was retrieved from the package manager. Operation took " + tdRetrievingInstalledApps + " seconds.");

			this.publishProgress (1F, 3F);

			if (this.isCancelled ())
				return null;

			appManager.sort ();

			this.publishProgress (2F, 3F);

			if (this.isCancelled ())
				return null;

			if (prefsPinned.getAll().size() > 0)
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

			this.publishProgress (3F, 3F);
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this.parent);
		}

		return appManager;
	}

	@Override
	protected void onProgressUpdate (Float... progress)
	{
		super.onProgressUpdate (progress[0]);

		ProgressWheel pw = this.lalSpinner.getProgressWheel ();
		pw.setProgress ((int) (progress[0] / progress[1] * 360));
	}

	@Override
	protected void onPostExecute (AppManager appManager)
	{
		try
		{
			this.lalSpinner.setVisibility (View.GONE);
			this.lalBfb.setVisibility (View.VISIBLE);
			
			appManager.refreshPinnedView ();
			
			this.gvDashHomeApps.setAdapter (new GridAdapter (this.context, appManager.getInstalledApps ()));
			this.gvDashHomeApps.setOnItemClickListener (new AppLauncherClickListener (this.parent));
			this.gvDashHomeApps.setOnItemLongClickListener (new AppLauncherLongClickListener (this.parent));
			
			this.parent.asyncLoadInstalledAppsDone (appManager);
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this.parent);
		}
	}
}

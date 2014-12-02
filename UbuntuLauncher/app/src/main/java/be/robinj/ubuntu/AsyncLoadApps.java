package be.robinj.ubuntu;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.view.View;
import android.widget.GridView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import be.robinj.ubuntu.thirdparty.ProgressWheel;
import be.robinj.ubuntu.unity.dash.AppLauncher;
import be.robinj.ubuntu.unity.dash.AppLauncherClickListener;
import be.robinj.ubuntu.unity.dash.AppLauncherLongClickListener;
import be.robinj.ubuntu.unity.dash.GridAdapter;
import be.robinj.ubuntu.unity.launcher.SpinnerAppLauncher;

/**
 * Created by robin on 8/21/14.
 */
public class AsyncLoadApps extends AsyncTask<Context, Float, Object[]>
{
	private SpinnerAppLauncher lalSpinner;
	private be.robinj.ubuntu.unity.launcher.AppLauncher lalBfb;
	private GridView gvDashHomeApps;
	private LinearLayout llLauncherPinnedApps;
	private HomeActivity parent;
	private Context context;

	public AsyncLoadApps (HomeActivity parent, SpinnerAppLauncher lalSpinner, be.robinj.ubuntu.unity.launcher.AppLauncher lalBfb, GridView gvDashHomeApps, LinearLayout llLauncherPinnedApps)
	{
		this.parent = parent;
		this.lalSpinner = lalSpinner;
		this.lalBfb = lalBfb;
		this.gvDashHomeApps = gvDashHomeApps;
		this.llLauncherPinnedApps = llLauncherPinnedApps;
	}

	@Override
	protected Object[] doInBackground (Context... params)
	{
		this.context = params[0];

		AppManager appManager = new AppManager (this.parent, this.parent);

		try
		{
			SharedPreferences prefs = this.context.getSharedPreferences ("prefs", Context.MODE_PRIVATE);
			String iconPack = prefs.getString ("iconpack", null);

			if (iconPack != null)
				appManager.loadIconPack ("com.numix.icons_circle");
		}
		catch (Exception e)
		{
			e.printStackTrace ();
		}

		List<ResolveInfo> resInfs = appManager.queryInstalledApps ();
		float size = resInfs.size ();
		this.publishProgress (0F, size);

		if (this.isCancelled ())
			return null;

		for (int i = 0; i < size; i++)
		{
			App app = App.fromResolveInfo (this.context, appManager, resInfs.get (i));
			if (! "be.robinj.ubuntu".equals (app.getPackageName ())) // Inception //
				appManager.add (app);

			this.publishProgress ((float) i, size);
		}

		size = appManager.size (); // Since the app itself is being filtered out to avoid an inception, the size will have changed, too //

		this.publishProgress (360.0F, 360.0F);

		if (this.isCancelled ())
			return null;

		appManager.sort ();

		//this.publishProgress (0.0F, size);

		List<AppLauncher> appLaunchers = new ArrayList<AppLauncher> ();
		for (int i = 0; i < size; i++)
		{
			App app = appManager.get (i);
			appLaunchers.add (new be.robinj.ubuntu.unity.dash.AppLauncher (this.context, app));

			//this.publishProgress ((float) i, size); // Looks like it spends more time updating the progress bar than actually looping over and adding the app launchers //
		}

		if (this.isCancelled ())
			return null;

		SharedPreferences pinned = this.context.getSharedPreferences ("pinned", Context.MODE_PRIVATE);

		int i = 0;
		String packageAndActivityName;
		while ((packageAndActivityName = pinned.getString (Integer.toString (i), null)) != null)
		{
			if (packageAndActivityName.contains ("\n")) // Transition from the Beta builds where pinned apps were stored by either package name or activity name (which resulted in bugs because some apps share a package/activity name) //
			{
				String packageName = packageAndActivityName.substring (0, packageAndActivityName.indexOf ("\n"));
				String activityName = packageAndActivityName.substring (packageAndActivityName.indexOf ("\n") + 1);

				App app = appManager.findAppByPackageAndActivityName (packageName, activityName);

				if (app != null) // The result of findAppByPackageName () is null if the app is no longer present on the device //
					appManager.pin (app, false, false, false);
			}

			i++;
		}

		return new Object[] { appManager, appLaunchers };
	}

	@Override
	protected void onProgressUpdate (Float... progress)
	{
		super.onProgressUpdate (progress[0]);

		ProgressWheel pw = this.lalSpinner.getProgressWheel ();
		pw.setProgress ((int) (progress[0] / progress[1] * 360));
	}

	@Override
	protected void onPostExecute (Object[] result)
	{
		this.lalSpinner.setVisibility (View.GONE);
		this.lalBfb.setVisibility (View.VISIBLE);

		AppManager appManager = (AppManager) result[0];
		appManager.refreshPinnedView ();

		this.gvDashHomeApps.setAdapter (new GridAdapter (this.context, (List<AppLauncher>) result[1]));
		this.gvDashHomeApps.setOnItemClickListener (new AppLauncherClickListener ());
		this.gvDashHomeApps.setOnItemLongClickListener (new AppLauncherLongClickListener ());

		this.parent.asyncLoadInstalledAppsDone (appManager);
	}
}

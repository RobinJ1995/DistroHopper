package be.robinj.ubuntu;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Debug;
import be.robinj.ubuntu.dev.Log;
import android.view.View;
import android.widget.GridView;
import android.widget.LinearLayout;

import com.snappydb.DB;
import com.snappydb.DBFactory;

import java.util.ArrayList;
import java.util.HashMap;
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
public class AsyncLoadApps extends AsyncTask<Context, Float, AppManager>
{
	private SpinnerAppLauncher lalSpinner;
	private be.robinj.ubuntu.unity.launcher.AppLauncher lalBfb;
	private GridView gvDashHomeApps;
	private HomeActivity parent;
	private Context context;

	public AsyncLoadApps (HomeActivity parent, SpinnerAppLauncher lalSpinner, be.robinj.ubuntu.unity.launcher.AppLauncher lalBfb, GridView gvDashHomeApps)
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
		List<AppLauncher> appLaunchers = null;

		try
		{
			this.context = params[0];

			appManager = new AppManager (this.parent, this.parent);
			DB db = DBFactory.open (this.context);

			try
			{
				SharedPreferences prefs = this.context.getSharedPreferences ("prefs", Context.MODE_PRIVATE);
				String iconPack = prefs.getString ("iconpack", null);

				if (iconPack != null)
					appManager.loadIconPack ("com.numix.icons_circle");
			}
			catch (Exception ex)
			{
				ex.printStackTrace ();
			}

			long tStartRetrievingInstalledApps = System.currentTimeMillis ();

			List<ResolveInfo> resInfs = appManager.queryInstalledApps ();
			int size = resInfs.size ();
			float fSize = (float) size;
			this.publishProgress (0F, fSize);

			if (this.isCancelled ())
				return null;

			PackageManager pacMan = this.context.getPackageManager ();

			for (int i = 0; i < size; i++)
			{
				App app = App.fromResolveInfo (this.context, pacMan, appManager, resInfs.get (i));
				if (! "be.robinj.ubuntu".equals (app.getPackageName ())) // Inception //
					appManager.add (app);

				this.publishProgress ((float) i, fSize);
			}

			long tDoneRetrievingInstalledApps = System.currentTimeMillis ();
			float tdRetrievingInstalledApps = (float) (tDoneRetrievingInstalledApps - tStartRetrievingInstalledApps) / 1000F;

			Log.v (this.getClass ().getSimpleName (), "Data about " + size + " installed apps was retrieved from the package manager. Operation took " + tdRetrievingInstalledApps + " seconds.");

			this.publishProgress (360.0F, 360.0F);

			if (this.isCancelled ())
				return null;

			appManager.sort ();

			if (this.isCancelled ())
				return null;

			/*SharedPreferences pinned = this.context.getSharedPreferences ("pinned", Context.MODE_PRIVATE);

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
			}*/

			if (db.exists ("launcher_pinnedApps"))
			{
				App[] apps = db.getObjectArray ("launcher_pinnedApps", App.class);

				for (App app : apps)
				{
					app.fixAfterUnserialize (appManager);
					appManager.pin (app, false, false, false);
				}
			}

			db.close ();
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this.context, ex);
			exh.show ();
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
		this.lalSpinner.setVisibility (View.GONE);
		this.lalBfb.setVisibility (View.VISIBLE);

		appManager.refreshPinnedView ();

		this.gvDashHomeApps.setAdapter (new GridAdapter (this.context, appManager.getInstalledApps ()));
		this.gvDashHomeApps.setOnItemClickListener (new AppLauncherClickListener ());
		this.gvDashHomeApps.setOnItemLongClickListener (new AppLauncherLongClickListener ());

		this.parent.asyncLoadInstalledAppsDone (appManager);
	}
}

package be.robinj.distrohopper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.view.View;
import android.widget.GridView;

import com.snappydb.DB;
import com.snappydb.DBFactory;

import java.util.List;

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
			DB db = DBFactory.open (this.context);

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
			float fSize = (float) size;
			this.publishProgress (0F, fSize);

			if (this.isCancelled ())
				return null;

			PackageManager pacMan = this.context.getPackageManager ();

			for (int i = 0; i < size; i++)
			{
				ResolveInfo resInf = resInfs.get (i);
				boolean skip = false;
				
				for (int j = 0; j < this.IGNORE.length; j++)
				{
					if (this.IGNORE[j].equals (resInf.activityInfo.packageName))
						skip = true;
				}
				if (skip)
					continue;
				
				App app = App.fromResolveInfo (this.context, pacMan, appManager, resInf);
				appManager.add (app, false, false);

				this.publishProgress ((float) i, fSize);
			}

			long tDoneRetrievingInstalledApps = System.currentTimeMillis ();
			float tdRetrievingInstalledApps = (float) (tDoneRetrievingInstalledApps - tStartRetrievingInstalledApps) / 1000F;

			Log.getInstance ().v (this.getClass ().getSimpleName (), "Data about " + size + " installed apps was retrieved from the package manager. Operation took " + tdRetrievingInstalledApps + " seconds.");

			this.publishProgress (360.0F, 360.0F);

			if (this.isCancelled ())
				return null;

			appManager.sort ();

			if (this.isCancelled ())
				return null;

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

package be.robinj.ubuntu;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import be.robinj.ubuntu.thirdparty.ExpandableHeightGridView;
import be.robinj.ubuntu.unity.dash.GridAdapter;
import be.robinj.ubuntu.unity.launcher.AppLauncherClickListener;
import be.robinj.ubuntu.unity.launcher.AppLauncherLongClickListener;

/**
 * Created by robin on 8/20/14.
 */
public class AppManager implements Iterable<App>
{
	private List<App> apps = new ArrayList<App> ();
	private List<App> pinned = new ArrayList<App> ();

	private LinearLayout llLauncherPinnedApps;
	private ExpandableHeightGridView gvDashHomeApps;

	private Context context;

	public AppManager (Context context, HomeActivity parent)
	{
		this.context = context;
		this.llLauncherPinnedApps = (LinearLayout) parent.findViewById (R.id.llLauncherPinnedApps);
		this.gvDashHomeApps = (ExpandableHeightGridView) parent.findViewById (R.id.gvDashHomeApps);
	}

	public void add (App app)
	{
		this.apps.add (app);
	}

	public void add (ResolveInfo resInf)
	{
		this.apps.add (App.fromResolveInfo (this.context, this, resInf));
	}

	public void clear ()
	{
		this.apps.clear ();
	}

	public App findAppByPackageAndActivityName (String packageName, String activityName)
	{
		for (App app : this.apps)
		{
			if (packageName.equals (app.getPackageName ()) && activityName.equals (app.getActivityName ()))
				return app;
		}

		return null;
	}

	public App get (int index)
	{
		return this.apps.get (index);
	}

	public Context getContext ()
	{
		return this.context;
	}

	public boolean isPinned (App app)
	{
		return this.pinned.contains (app);
	}

	public Iterator<App> iterator ()
	{
		return this.apps.iterator ();
	}

	public boolean pin (App app)
	{
		return this.pin (app, true, true, true);
	}

	public boolean pin (App app, boolean save, boolean showToast, boolean addView)
	{
		if (! this.isPinned (app))
		{
			boolean returnValue = this.pinned.add (app);

			if (showToast)
				Toast.makeText (this.context, app.getLabel () + " has been pinned to the Launcher", Toast.LENGTH_SHORT).show ();

			if (save)
				this.savePinnedApps ();

			if (addView)
			{
				be.robinj.ubuntu.unity.launcher.AppLauncher appLauncher = new be.robinj.ubuntu.unity.launcher.AppLauncher (this.context, app);
				appLauncher.setOnClickListener (new AppLauncherClickListener ());
				appLauncher.setOnLongClickListener (new AppLauncherLongClickListener ());

				this.llLauncherPinnedApps.addView (appLauncher);
			}

			return returnValue;
		}
		else
		{
			return false;
		}
	}

	public List<ResolveInfo> queryInstalledApps ()
	{
		Intent mainIntent = new Intent (Intent.ACTION_MAIN);
		mainIntent.addCategory (Intent.CATEGORY_LAUNCHER);
		PackageManager pacMan = this.context.getPackageManager ();
		List<ResolveInfo> apps = pacMan.queryIntentActivities (mainIntent, 0);

		return apps;
	}

	public void refreshPinnedView ()
	{
		this.llLauncherPinnedApps.removeAllViews ();

		for (App app : this.pinned)
		{
			be.robinj.ubuntu.unity.launcher.AppLauncher appLauncher = new be.robinj.ubuntu.unity.launcher.AppLauncher (this.context, app);
			appLauncher.setOnClickListener (new AppLauncherClickListener ());
			appLauncher.setOnLongClickListener (new AppLauncherLongClickListener ());

			this.llLauncherPinnedApps.addView (appLauncher);
		}
	}

	public void savePinnedApps ()
	{
		SharedPreferences prefs = this.context.getSharedPreferences ("pinned", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit ();

		editor.clear ();

		for (int i = 0; i < this.pinned.size (); i++)
		{
			App app = this.pinned.get (i);
			StringBuilder packageAndActivityName = new StringBuilder (app.getPackageName ())
				.append ("\n")
				.append (app.getActivityName ());

			editor.putString (Integer.toString (i), packageAndActivityName.toString ());
		}

		editor.apply ();
	}

	public List<App> search (String pattern)
	{
		return this.search (pattern, false);
	}

	public List<App> search (String pattern, boolean showResults)
	{
		List<App> results;

		if (pattern.isEmpty ())
		{
			results = new ArrayList<App> (this.apps);
		}
		else
		{
			results = new ArrayList<App> ();

			SharedPreferences prefs = this.context.getSharedPreferences ("prefs", Context.MODE_PRIVATE);
			boolean fullSearch = prefs.getBoolean ("fullSearch", false);

			pattern = pattern.toLowerCase ();

			for (App app : this.apps)
			{
				if (app.getLabel ().toLowerCase ().startsWith (pattern))
					results.add (app);

				if (fullSearch)
				{
					if ((! results.contains (app)) && (app.getLabel ().toLowerCase ().contains (pattern) || app.getDescription ().toLowerCase ().contains (pattern)))
						results.add (app);
				}
			}
		}

		if (showResults)
		{
			List<be.robinj.ubuntu.unity.dash.AppLauncher> appLaunchers = new ArrayList<be.robinj.ubuntu.unity.dash.AppLauncher> ();

			for (App app : results)
			{
				be.robinj.ubuntu.unity.dash.AppLauncher appLauncher = new be.robinj.ubuntu.unity.dash.AppLauncher (this.context, app);
				appLaunchers.add (appLauncher);
			}

			this.gvDashHomeApps.setAdapter (new GridAdapter (this.context, appLaunchers));
		}

		return results;
	}

	public int size ()
	{
		return this.apps.size ();
	}

	public void sort ()
	{
		AppComparatorAlphabetical comparator = new AppComparatorAlphabetical ();
		Collections.sort (this.apps, comparator);
		String x=  ";";
	}

	public boolean unpin (App app)
	{
		boolean modified = this.pinned.remove (app);

		String message;
		if (modified)
			message = " has been unpinned from the Launcher";
		else
			message = " isn't pinned";

		Toast.makeText (this.context, app.getLabel () + message, Toast.LENGTH_SHORT).show ();

		be.robinj.ubuntu.unity.launcher.AppLauncher appLauncher = (be.robinj.ubuntu.unity.launcher.AppLauncher) this.llLauncherPinnedApps.findViewWithTag (app);
		this.llLauncherPinnedApps.removeView (appLauncher);

		this.savePinnedApps ();

		return modified;
	}
}

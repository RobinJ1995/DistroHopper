package be.robinj.ubuntu;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.view.View;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.snappydb.DB;
import com.snappydb.DBFactory;
import com.snappydb.SnappydbException;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import be.robinj.ubuntu.unity.dash.GridAdapter;
import be.robinj.ubuntu.unity.launcher.AppLauncher;
import be.robinj.ubuntu.unity.launcher.AppLauncherClickListener;
import be.robinj.ubuntu.unity.launcher.AppLauncherDragListener;
import be.robinj.ubuntu.unity.launcher.AppLauncherLongClickListener;
import be.robinj.ubuntu.unity.launcher.RunningAppLauncher;

/**
 * Created by robin on 8/20/14.
 */
public class AppManager implements Iterable<App>
{
	private List<App> apps = new ArrayList<App> ();
	private List<App> pinned = new ArrayList<App> ();

	private IconPackHelper iconPack;

	private LinearLayout llLauncher;
	private LinearLayout llLauncherPinnedApps;
	private LinearLayout llLauncherRunningApps;
	private GridView gvDashHomeApps;

	private HomeActivity parent;
	private Context context;

	public AppManager (Context context, HomeActivity parent)
	{
		this.context = context;
		this.parent = parent;

		this.iconPack = new IconPackHelper (context.getApplicationContext ());

		this.llLauncher = (LinearLayout) parent.findViewById (R.id.llLauncher);
		this.llLauncherPinnedApps = (LinearLayout) this.llLauncher.findViewById (R.id.llLauncherPinnedApps);
		this.llLauncherRunningApps = (LinearLayout) this.llLauncher.findViewById (R.id.llLauncherRunningApps);
		this.gvDashHomeApps = (GridView) parent.findViewById (R.id.gvDashHomeApps);
	}

	public void add (App app)
	{
		this.apps.add (app);
	}

	public void add (ResolveInfo resInf)
	{
		this.apps.add (App.fromResolveInfo (this.context, this, resInf));
	}

	public void addRunningApps (int colour)
	{
		this.llLauncherRunningApps.removeAllViews ();

		for (int i = 0; i < this.llLauncherPinnedApps.getChildCount (); i++)
			((AppLauncher) this.llLauncherPinnedApps.getChildAt (i)).setRunning (false);

		for (App app : this.getRunningApps ())
		{
			if (this.isPinned (app))
			{
				AppLauncher appLauncher = (AppLauncher) this.llLauncherPinnedApps.findViewWithTag (app);
				appLauncher.setRunning (true);
			}
			else
			{
				if (! this.context.getResources ().getBoolean (HomeActivity.theme.launcher_applauncher_backgroundcolour_dynamic))
					colour = this.context.getResources ().getColor (HomeActivity.theme.launcher_applauncher_backgroundcolour);

				RunningAppLauncher appLauncher = new RunningAppLauncher (this.context, app);
				appLauncher.setOnClickListener (new AppLauncherClickListener ());
				appLauncher.setColour (colour);

				this.llLauncherRunningApps.addView (appLauncher);
			}
		}
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

	public IconPackHelper getIconPack ()
	{
		return this.iconPack;
	}

	public HomeActivity getParent ()
	{
		return this.parent;
	}

	public List<App> getPinned ()
	{
		return this.pinned;
	}

	public List<App> getRunningApps ()
	{
		List<App> running = new ArrayList<App> ();

		ActivityManager am = (ActivityManager) this.context.getSystemService (Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningTaskInfo> runningTasks = am.getRunningTasks (16);

		for (ActivityManager.RunningTaskInfo task : runningTasks)
		{
			String packageName = task.baseActivity.getPackageName ();
			String activityName = task.baseActivity.getClassName ();

			App app = this.findAppByPackageAndActivityName (packageName, activityName);

			if (app != null)
				running.add (app);
		}

		return running;
	}

	public int indexOfPinned (App app)
	{
		return this.pinned.indexOf (app);
	}

	public boolean isIconPackLoaded ()
	{
		return this.iconPack.isIconPackLoaded ();
	}

	public boolean isPinned (App app)
	{
		return this.pinned.contains (app);
	}

	public Iterator<App> iterator ()
	{
		return this.apps.iterator ();
	}

	public void loadIconPack (String name) throws IOException, XmlPullParserException, PackageManager.NameNotFoundException
	{
		this.iconPack.loadIconPack (name);
	}

	public void movePinnedApp (int oldIndex, int newIndex)
	{
		App app = this.pinned.remove (oldIndex);
		this.pinned.add (newIndex, app);
	}

	public boolean pin (App app) throws SnappydbException
	{
		return this.pin (app, true, true, true);
	}

	public boolean pin (App app, boolean save, boolean showToast, boolean addView) throws SnappydbException
	{
		if (! this.isPinned (app))
		{
			boolean returnValue = this.pinned.add (app);

			if (showToast)
				Toast.makeText (this.context, app.getLabel () + " " + this.context.getResources ().getString (R.string.pinned), Toast.LENGTH_SHORT).show ();

			if (addView)
			{
				be.robinj.ubuntu.unity.launcher.AppLauncher appLauncher = new be.robinj.ubuntu.unity.launcher.AppLauncher (this.context, app);
				appLauncher.setOnClickListener (new AppLauncherClickListener ());
				appLauncher.setOnLongClickListener (new AppLauncherLongClickListener ());
				if (Build.VERSION.SDK_INT >= 11)
					appLauncher.setOnDragListener (new AppLauncherDragListener (this));

				this.llLauncherPinnedApps.addView (appLauncher);
			}

			if (save)
				this.savePinnedApps ();

			return returnValue;
		}
		else
		{
			if (showToast)
				Toast.makeText (this.context, app.getLabel () + " " + this.context.getResources ().getString (R.string.alreadypinned), Toast.LENGTH_SHORT).show ();

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
			if (Build.VERSION.SDK_INT >= 11)
				appLauncher.setOnDragListener (new AppLauncherDragListener (this));

			this.llLauncherPinnedApps.addView (appLauncher);
		}
	}

	public void savePinnedApps () throws SnappydbException
	{
		/*
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

		if (Build.VERSION.SDK_INT >= 9)
			editor.apply ();
		else
			editor.commit ();*/

		DB db = DBFactory.open (this.context);
		db.put ("launcher_pinnedApps", this.pinned.toArray ());
		db.close ();

		this.parent.pinnedAppsChanged ();
	}

	public List<App> search (String pattern)
	{
		return this.search (pattern, false);
	}

	public List<App> search (String pattern, boolean showResults)
	{
		List<App> results;

		if (pattern.length () == 0)
		{
			results = new ArrayList<App> (this.apps);
		}
		else
		{
			results = new ArrayList<App> ();

			SharedPreferences prefs = this.context.getSharedPreferences ("prefs", Context.MODE_PRIVATE);
			boolean fullSearch = prefs.getBoolean ("dashsearch_full", true);

			pattern = pattern.toLowerCase ();

			for (App app : this.apps)
			{
				if (app.getLabel ().toLowerCase ().startsWith (pattern))
					results.add (app);
			}

			if (fullSearch)
			{
				for (App app : this.apps)
				{
					if ((! results.contains (app)) && (app.getLabel ().toLowerCase ().contains (pattern)))
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
	}

	public boolean unpin (int index) throws SnappydbException
	{
		return this.unpin (this.pinned.get (index));
	}

	public boolean unpin (App app) throws SnappydbException
	{
		boolean modified = this.pinned.remove (app);

		String message;
		if (modified)
			message = " " + this.context.getResources ().getString (R.string.unpinned);
		else
			message = " " + this.context.getResources ().getString (R.string.notpinned);

		Toast.makeText (this.context, app.getLabel () + message, Toast.LENGTH_SHORT).show ();

		be.robinj.ubuntu.unity.launcher.AppLauncher appLauncher = (be.robinj.ubuntu.unity.launcher.AppLauncher) this.llLauncherPinnedApps.findViewWithTag (app);
		this.llLauncherPinnedApps.removeView (appLauncher);

		this.savePinnedApps ();

		return modified;
	}

	/*# Event handlers #*/
	public void startedDraggingPinnedApp ()
	{
		AppLauncher lalPreferences = (AppLauncher) this.llLauncher.findViewById (R.id.lalPreferences);
		AppLauncher lalTrash = (AppLauncher) this.llLauncher.findViewById (R.id.lalTrash);

		lalPreferences.setVisibility (View.GONE);
		lalTrash.setVisibility (View.VISIBLE);

		if (Build.VERSION.SDK_INT >= 11)
			this.llLauncherPinnedApps.setAlpha (0.9F);
	}

	public void stoppedDraggingPinnedApp ()
	{
		AppLauncher lalPreferences = (AppLauncher) this.llLauncher.findViewById (R.id.lalPreferences);
		AppLauncher lalTrash = (AppLauncher) this.llLauncher.findViewById (R.id.lalTrash);

		switch (this.context.getResources ().getInteger (HomeActivity.theme.launcher_preferences_location))
		{
			case -1:
				lalPreferences.setVisibility (View.GONE);
				break;
			default:
				lalPreferences.setVisibility (View.VISIBLE);
				break;
		}
		lalTrash.setVisibility (View.GONE);

		if (Build.VERSION.SDK_INT >= 11)
			this.llLauncherPinnedApps.setAlpha (1.0F);
	}
}

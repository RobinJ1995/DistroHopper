package be.robinj.distrohopper;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import be.robinj.distrohopper.desktop.launcher.AppLauncher;
import be.robinj.distrohopper.desktop.launcher.AppLauncherClickListener;
import be.robinj.distrohopper.desktop.launcher.AppLauncherDragListener;
import be.robinj.distrohopper.desktop.launcher.AppLauncherLongClickListener;
import be.robinj.distrohopper.desktop.launcher.RunningAppLauncher;
import be.robinj.distrohopper.preferences.Preference;
import be.robinj.distrohopper.preferences.Preferences;
import be.robinj.distrohopper.theme.Location;

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

	public AppManager (HomeActivity parent)
	{
		this.parent = parent;

		this.iconPack = new IconPackHelper (parent.getApplicationContext ());

		this.llLauncher = parent.getViewFinder().get(R.id.llLauncher);
		this.llLauncherPinnedApps = parent.getViewFinder().get(this.llLauncher, R.id.llLauncherPinnedApps);
		this.llLauncherRunningApps = parent.getViewFinder().get(this.llLauncher, R.id.llLauncherRunningApps);
		this.gvDashHomeApps = parent.getViewFinder().get(R.id.gvDashHomeApps);
	}

	public void add (App app)
	{
		this.add (app, false, true);
	}

	public void add (App app, boolean checkDuplicate, boolean sortAndNotifyAdapter)
	{
		if (! (checkDuplicate && this.apps.contains (app)))
		{
			this.apps.add (app);

			if (sortAndNotifyAdapter)
			{
				this.sort ();

				ArrayAdapter adapter = (ArrayAdapter) this.gvDashHomeApps.getAdapter ();
				if (adapter != null)
					adapter.notifyDataSetChanged ();
			}
		}
	}

	public void add (ResolveInfo resInf)
	{
		this.add (resInf, false, true);
	}

	public void add (ResolveInfo resInf, boolean checkDuplicate, boolean sortAndNotifyAdapter)
	{
		this.add (new App(this.getContext (), this, resInf), checkDuplicate, sortAndNotifyAdapter);
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
				if (! this.getContext ().getResources ().getBoolean (HomeActivity.theme.launcher_applauncher_backgroundcolour_dynamic))
					colour = this.getContext ().getResources ().getColor (HomeActivity.theme.launcher_applauncher_backgroundcolour);

				RunningAppLauncher appLauncher = new RunningAppLauncher (this.getContext (), app);
				appLauncher.setOnClickListener (new AppLauncherClickListener (this.getContext ()));
				appLauncher.setColour (colour);

				this.llLauncherRunningApps.addView (appLauncher);
			}
		}
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

	public List<App> findAppsByPackageName (String packageName)
	{
		List<App> results = new ArrayList<App> ();

		for (App app : this.apps)
		{
			if (packageName.equals (app.getPackageName ()))
				results.add (app);
		}

		return results;
	}

	public App get (int index)
	{
		return this.apps.get (index);
	}

	public HomeActivity getContext ()
	{
		return this.getParent ();
	}

	public IconPackHelper getIconPack ()
	{
		return this.iconPack;
	}

	public List<App> getInstalledApps ()
	{
		return this.apps;
	}

	public Map<String, App> getInstalledAppsMap() {
		final Map<String, App> map = new HashMap<>();

		for (final App app : this.apps) {
			map.put(app.getPackageAndActivityName(), app);
		}

		return map;
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
		ActivityManager am = (ActivityManager) this.getContext ().getSystemService (Context.ACTIVITY_SERVICE);

		if (Build.VERSION.SDK_INT >= 21) // ActivityManager.getRunningTasks () is deprecated //
		{
			List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses ();

			for (ActivityManager.RunningAppProcessInfo appProcess : runningAppProcesses)
			{
				Integer[] importantImportances = new Integer[]
				{
					ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND,
					ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE,
					ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE,
					ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND,
					ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE
				};

				if (Arrays.asList (importantImportances).contains (appProcess.importance))
				{
					for (App app : this.findAppsByPackageName (appProcess.processName))
						running.add (app);
				}
			}
		}
		else
		{
			List<ActivityManager.RunningTaskInfo> runningTasks = am.getRunningTasks (16);

			for (ActivityManager.RunningTaskInfo task : runningTasks)
			{
				String packageName = task.baseActivity.getPackageName ();
				String activityName = task.baseActivity.getClassName ();

				App app = this.findAppByPackageAndActivityName (packageName, activityName);

				if (app != null)
					running.add (app);
			}
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
				Toast.makeText (this.getContext (), app.getLabel () + " " + this.getContext ().getResources ().getString (R.string.pinned), Toast.LENGTH_SHORT).show ();

			if (addView)
			{
				be.robinj.distrohopper.desktop.launcher.AppLauncher appLauncher = new be.robinj.distrohopper.desktop.launcher.AppLauncher (this.getContext (), app);
				appLauncher.setOnClickListener (new AppLauncherClickListener (this.getContext ()));
				appLauncher.setOnLongClickListener (new AppLauncherLongClickListener (this.getContext ()));
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
				Toast.makeText (this.getContext (), app.getLabel () + " " + this.getContext ().getResources ().getString (R.string.alreadypinned), Toast.LENGTH_SHORT).show ();

			return false;
		}
	}

	public List<ResolveInfo> queryInstalledApps ()
	{
		return this.queryInstalledApps (null);
	}

	public List<ResolveInfo> queryInstalledApps(final String packageName) {
		final Intent mainIntent = new Intent(Intent.ACTION_MAIN);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		if (packageName != null) {
			mainIntent.setPackage(packageName);
		}
		final PackageManager pacMan = this.getContext().getPackageManager();
		List<ResolveInfo> apps = pacMan.queryIntentActivities(mainIntent, 0);

		return apps;
	}

	public void refreshPinnedView ()
	{
		this.llLauncherPinnedApps.removeAllViews ();

		for (App app : this.pinned)
		{
			be.robinj.distrohopper.desktop.launcher.AppLauncher appLauncher = new be.robinj.distrohopper.desktop.launcher.AppLauncher (this.getContext (), app);
			appLauncher.setOnClickListener (new AppLauncherClickListener (this.getContext ()));
			appLauncher.setOnLongClickListener (new AppLauncherLongClickListener (this.getContext ()));
			appLauncher.setOnDragListener (new AppLauncherDragListener (this));

			this.llLauncherPinnedApps.addView (appLauncher);
		}
	}

	public boolean remove (App app)
	{
		boolean modified = this.apps.remove (app);
		this.unpin (app, false);

		ArrayAdapter adapter = (ArrayAdapter) this.gvDashHomeApps.getAdapter ();
		if (adapter != null)
			adapter.notifyDataSetChanged ();

		return modified;
	}

	public void savePinnedApps ()
	{
		SharedPreferences prefs = Preferences.getSharedPreferences(this.getContext(), Preferences.PINNED_APPS);
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

		this.parent.pinnedAppsChanged ();
	}

	public List<App> search (final String pattern) {
		return this.search(pattern, Integer.MAX_VALUE);
	}

	/**
	 * Search apps based on provided pattern.
	 *
	 * @param pattern: Pattern to search for.
	 * @param maxResults: Maximum number of results ot return. NOTE: This is ignored when pattern is empty.
	 * @return results
	 */
	public List<App> search (String pattern, final int maxResults)
	{
		List<App> results;
		int nResults = 0;

		if (pattern.length () == 0)
		{
			results = new ArrayList<App> (this.apps);
		}
		else
		{
			results = new ArrayList<App> ();

			SharedPreferences prefs = Preferences.getSharedPreferences(this.getContext(), Preferences.PREFERENCES);
			boolean fullSearch = prefs.getBoolean (Preference.DASH_SEARCH_FULL.getName(), true);

			pattern = pattern.toLowerCase ();

			for (App app : this.apps)
			{
				if (app.getLabel ().toLowerCase ().startsWith (pattern)) {
					results.add(app);

					if (++nResults >= maxResults) {
						return results;
					}
				}
			}

			if (fullSearch)
			{
				for (App app : this.apps)
				{
					if ((! results.contains (app)) && (app.getLabel ().toLowerCase ().contains (pattern))) {
						results.add(app);

						if (++nResults >= maxResults) {
							return results;
						}
					}
				}
			}
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

	public boolean unpin (int index)
	{
		return this.unpin (this.pinned.get (index));
	}

	public boolean unpin (App app)
	{
		return this.unpin (app, true);
	}

	public boolean unpin (App app, boolean showToast)
	{
		boolean modified = this.pinned.remove (app);

		if (showToast)
		{
			String message;
			if (modified)
				message = " " + this.getContext ().getResources ().getString (R.string.unpinned);
			else
				message = " " + this.getContext ().getResources ().getString (R.string.notpinned);

			Toast.makeText (this.getContext (), app.getLabel () + message, Toast.LENGTH_SHORT).show ();
		}

		be.robinj.distrohopper.desktop.launcher.AppLauncher appLauncher = this.llLauncherPinnedApps.findViewWithTag (app);
		this.llLauncherPinnedApps.removeView (appLauncher);

		this.savePinnedApps ();

		return modified;
	}

	/*# Event handlers #*/
	public void startedDraggingPinnedApp ()
	{
		final AppLauncher lalBfb = this.parent.getViewFinder().get(this.llLauncher, R.id.lalBfb);
		final AppLauncher lalPreferences = this.parent.getViewFinder().get(this.llLauncher, R.id.lalPreferences);
		final AppLauncher lalTrash = this.parent.getViewFinder().get(this.llLauncher, R.id.lalTrash);

		if (this.parent.getResources().getBoolean(HomeActivity.theme.launcher_bfb_hide_while_dragging)) {
			lalBfb.setVisibility(View.GONE);
		}
		lalPreferences.setVisibility (View.GONE);
		lalTrash.setVisibility (View.VISIBLE);
		this.parent.closeDash();
		
		this.llLauncherPinnedApps.setAlpha (0.9F);
	}

	public void stoppedDraggingPinnedApp ()
	{
		final AppLauncher lalBfb = this.parent.getViewFinder().get(this.llLauncher, R.id.lalBfb);
		final AppLauncher lalPreferences = this.parent.getViewFinder().get(this.llLauncher, R.id.lalPreferences);
		final AppLauncher lalTrash = this.parent.getViewFinder().get(this.llLauncher, R.id.lalTrash);

		final Context context = this.getContext();
		final Location lalPreferences_location = HomeActivity.theme.lalPreferences_getLocation(context.getResources(), Preferences.getSharedPreferences(context));
		lalBfb.setVisibility(View.VISIBLE);
		lalPreferences.setVisibility (lalPreferences_location == Location.NONE ? View.GONE : View.VISIBLE);
		lalTrash.setVisibility (View.GONE);
		
		this.llLauncherPinnedApps.setAlpha (1.0F);
	}

	public void asyncLoadAppLabelsDone() {
		this.gvDashHomeApps.invalidateViews();
	}

	public void asyncLoadAppIconsDone() {
		this.gvDashHomeApps.invalidateViews();
	}
}

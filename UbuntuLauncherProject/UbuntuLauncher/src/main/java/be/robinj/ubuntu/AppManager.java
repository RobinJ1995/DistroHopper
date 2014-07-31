package be.robinj.ubuntu;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.webkit.JavascriptInterface;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;

public class AppManager
{
	private ArrayList<AppLauncher> appLaunchers = new ArrayList<AppLauncher> ();
	private static QueueExt<AppLauncher> recent = new QueueExt<AppLauncher> ((short) 5);

	public AppManager ()
	{
	}

	public static AppManager installedApps (Context context)
	{
		return AppManager.installedApps (context, true);
	}

	public static AppManager installedApps (Context context, boolean sorted)
	{
		return AppManager.installedApps (context, true, true);
	}

	public static AppManager installedApps (Context context, boolean sorted, boolean mostUsedFirst)
	{
		AppManager manager = new AppManager ();
		manager.addInstalledApps ();
		manager.sort (mostUsedFirst);

		return manager;
	}

	public static QueueExt<AppLauncher> getRecentStatic ()
	{
		return AppManager.recent;
	}

	@JavascriptInterface
	public QueueExt<AppLauncher> getRecent () // The Javascript side can't call static methods (AFAIK) //
	{
		return AppManager.recent;
	}

	@JavascriptInterface
	public void add (AppLauncher app)
	{
		this.appLaunchers.add (app);
	}

	@JavascriptInterface
	public void add (int i, AppLauncher app)
	{
		this.appLaunchers.add (i, app);
	}

	@JavascriptInterface
	public void add (AppLauncher[] apps)
	{
		for (AppLauncher app : apps)
			this.add (app);
	}

	@JavascriptInterface
	public void add (List<AppLauncher> apps)
	{
		for (AppLauncher app : apps)
			this.add (app);
	}

	@JavascriptInterface
	public void addInstalledApps ()
	{
		this.addInstalledApps (false);
	}

	@JavascriptInterface
	public void addInstalledApps (boolean clear)
	{
		if (clear)
			this.clear ();

		Intent mainIntent = new Intent (Intent.ACTION_MAIN);
		mainIntent.addCategory (Intent.CATEGORY_LAUNCHER);
		PackageManager pacMan = MainActivity.getContext ().getPackageManager ();
		List<ResolveInfo> apps = pacMan.queryIntentActivities (mainIntent, 0);

		for (ResolveInfo app : apps)
			this.add (new AppLauncher (app));
	}

	@JavascriptInterface
	public void clear ()
	{
		this.appLaunchers.clear ();
	}

	@JavascriptInterface
	public boolean contains (AppLauncher app)
	{
		for (AppLauncher appLauncher : this.appLaunchers)
		{
			if (appLauncher.equals (app))
				return true;
		}

		return false;
	}

	@JavascriptInterface
	public boolean containsAll (AppLauncher[] apps)
	{
		int found = 0;
		int length = apps.length;

		for (AppLauncher appLauncher : apps)
		{
			if (this.contains (appLauncher))
				found++;
		}

		return (found >= length);
	}

	@JavascriptInterface
	public boolean containsAll (List<AppLauncher> apps)
	{
		int found = 0;
		int length = apps.size ();

		for (AppLauncher appLauncher : apps)
		{
			if (this.contains (appLauncher))
				found++;
		}

		return (found >= length ? true : false);
	}

	@JavascriptInterface
	public boolean containsAll (AppManager manager)
	{
		int found = 0;
		int length = manager.size ();

		for (Iterator<AppLauncher> iterator = manager.iterator (); iterator.hasNext (); )
		{
			if (this.contains (iterator.next ()))
				found++;
		}

		return (found >= length ? true : false);
	}

	@JavascriptInterface
	public boolean equals (AppManager compareTo)
	{
		boolean equal = false;

		if (this.hashCode () == compareTo.hashCode ())
			equal = true;

		return equal;
	}

	@JavascriptInterface
	public ArrayListExt<Integer> find (String pattern)
	{
		ArrayListExt<Integer> results = new ArrayListExt<Integer> ();
		pattern = pattern.toLowerCase ();

		for (int i = 0; i < this.appLaunchers.size (); i++)
		{
			if (pattern.isEmpty ())
			{
				results.add (i);
			}
			else
			{
				AppLauncher app = this.appLaunchers.get (i);

				if (app.getLabel ().toLowerCase ().startsWith (pattern))
					results.add (i);
			}
		}

		return results;
	}

	@JavascriptInterface
	public AppLauncherSimplified[] toAppLauncherSimplifiedArray ()
	{
		AppLauncherSimplified[] arr = new AppLauncherSimplified[this.appLaunchers.size ()];

		for (int i = 0; i < arr.length; i++)
			arr[i] = new AppLauncherSimplified (this.appLaunchers.get (i));

		return arr;
	}

	@JavascriptInterface
	public List<AppLauncherSimplified> toAppLauncherSimplifiedList ()
	{
		List<AppLauncherSimplified> result = new ArrayList<AppLauncherSimplified> ();

		for (int i = 0; i < this.appLaunchers.size (); i++)
			result.set (i, new AppLauncherSimplified (this.appLaunchers.get (i)));

		return result;
	}

	@JavascriptInterface
	public AppLauncher get (int i)
	{
		return this.appLaunchers.get (i);
	}

	@JavascriptInterface
	public int hashCode ()
	{
		return this.toJSON ().hashCode ();
	}

	@JavascriptInterface
	public int indexOf (AppLauncher app)
	{
		for (int i = 0; i < this.appLaunchers.size (); i++)
		{
			if (appLaunchers.get (i).equals (app))
				return i;
		}

		return -1;
	}

	@JavascriptInterface
	public boolean isEmpty ()
	{
		return this.appLaunchers.isEmpty ();
	}

	@JavascriptInterface
	public Iterator<AppLauncher> iterator ()
	{
		return this.appLaunchers.iterator ();
	}

	@JavascriptInterface
	public ListIterator<AppLauncher> listIterator ()
	{
		return this.appLaunchers.listIterator ();
	}

	@JavascriptInterface
	public ListIterator<AppLauncher> listIterator (int location)
	{
		return this.appLaunchers.listIterator (location);
	}

	@JavascriptInterface
	public void remove (int index)
	{
		this.appLaunchers.remove (index);
	}

	@JavascriptInterface
	public boolean remove (AppLauncher app)
	{
		return this.appLaunchers.remove (app);
	}

	@JavascriptInterface
	public void remove (AppLauncher[] apps)
	{
		for (AppLauncher app : apps)
			this.remove (app);
	}

	@JavascriptInterface
	public void remove (List<AppLauncher> apps)
	{
		for (AppLauncher app : apps)
			this.remove (app);
	}

	@JavascriptInterface
	public String toJSON ()
	{
		AppLauncherSimplified[] arr = this.toAppLauncherSimplifiedArray ();
		Gson gson = new Gson ();

		return gson.toJson (arr, AppLauncherSimplified[].class);
	}

	public static AppManager fromJSON (String json)
	{
		Gson gson = new Gson ();
		AppLauncherSimplified[] arr = gson.fromJson (json, AppLauncherSimplified[].class);

		AppManager manager = new AppManager ();
		for (AppLauncherSimplified simplified : arr)
			manager.add (simplified.toAppLauncher ());

		return manager;
	}

	@JavascriptInterface
	public void set (int index, AppLauncher app)
	{
		this.appLaunchers.set (index, app);
	}

	@JavascriptInterface
	public int size ()
	{
		return this.appLaunchers.size ();
	}

	@JavascriptInterface
	public void sort ()
	{
		this.sort (true);
	}

	@JavascriptInterface
	public void sort (boolean mostUsedFirst)
	{
		AppLauncherComparator comparator = new AppLauncherComparator (mostUsedFirst);
		Collections.sort (this.appLaunchers, comparator);
	}

	@JavascriptInterface
	public List<AppLauncher> subList (int start, int end)
	{
		return this.appLaunchers.subList (start, end);
	}

	@JavascriptInterface
	public Object[] toArray ()
	{
		return this.appLaunchers.toArray ();
	}

	@JavascriptInterface
	public String toHtml (String htmlClass)
	{
		return this.toHtml (htmlClass, false);
	}

	@JavascriptInterface
	public String toHtml (String htmlClass, boolean backgroundColour)
	{
		StringBuilder buildStr = new StringBuilder ();

		for (int i = 0; i < this.size (); i++)
			buildStr.append (this.appLaunchers.get (i).toHtml (htmlClass, new String[][] {{"index", Integer.toString (i)}}, backgroundColour));

		return buildStr.toString ();
	}
}

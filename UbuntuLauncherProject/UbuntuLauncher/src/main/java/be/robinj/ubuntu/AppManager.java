package be.robinj.ubuntu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.webkit.JavascriptInterface;

public class AppManager
{
	private Context context;
	private ArrayList<AppLauncher> appLaunchers = new ArrayList<AppLauncher> ();
	
	public AppManager (Context context)
	{
		this.context = context;
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
		PackageManager pkgMan = this.context.getPackageManager ();
		List<ResolveInfo> apps = pkgMan.queryIntentActivities (mainIntent, 0);
		
		for (ResolveInfo app : apps)
			this.add (new AppLauncher (this.context, app));
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
			if (appLauncher.equals(app))
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
		
		for (Iterator<AppLauncher> iterator = manager.iterator (); iterator.hasNext ();)
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
	public AppLauncher get (int i)
	{
		return this.appLaunchers.get(i);
	}

    @JavascriptInterface
	public int hashCode ()
	{
		return this.serialize ().hashCode ();
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
		return this.appLaunchers.isEmpty();
	}

    @JavascriptInterface
	public Iterator<AppLauncher> iterator ()
	{
		return this.appLaunchers.iterator();
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
	public String serialize ()
	{
		StringBuilder buildStr = new StringBuilder ();
		for (AppLauncher app : this.appLaunchers)
			buildStr.append (app.serialize ());
		
		return buildStr.toString ();
	}

    @JavascriptInterface
	public void set (int index, AppLauncher app)
	{
		this.appLaunchers.set (index,  app);
	}

    @JavascriptInterface
	public int size ()
	{
		return this.appLaunchers.size ();
	}

    @JavascriptInterface
	public void sort ()
	{
		this.sort(true);
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
		return this.appLaunchers.subList(start, end);
	}

    @JavascriptInterface
	public Object[] toArray ()
	{
		return this.appLaunchers.toArray();
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
	        buildStr.append (this.appLaunchers.get (i).toHtml (htmlClass, new String[][] { { "index", Integer.toString (i) } }, backgroundColour));
        
        return buildStr.toString ();
	}

    @JavascriptInterface
	public static AppManager installedApps (Context context)
	{
		return AppManager.installedApps (context, true);
	}

    @JavascriptInterface
	public static AppManager installedApps (Context context, boolean sorted)
	{
		return AppManager.installedApps (context, true, true);
	}

    @JavascriptInterface
	public static AppManager installedApps (Context context, boolean sorted, boolean mostUsedFirst)
	{
		AppManager manager = new AppManager (context);
		manager.addInstalledApps ();
		manager.sort (mostUsedFirst);
		
		return manager;
	}
}

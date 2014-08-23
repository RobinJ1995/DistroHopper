package be.robinj.ubuntu;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.webkit.JavascriptInterface;

public class AppLauncher
{
	private String label;

	private AppIcon icon;
	private String description;

	private String packageName;
	private String activityName;
	private String dataDir;
	private int targetSdk;

	public AppLauncher ()
	{
	}

	public AppLauncher (ResolveInfo resInf)
	{
		this.loadFromResolveInfo (resInf);
	}

	@JavascriptInterface
	public String getPackageName ()
	{
		return packageName;
	}

	@JavascriptInterface
	public void setPackageName (String packageName)
	{
		this.packageName = packageName;
	}

	@JavascriptInterface
	public String getActivityName ()
	{
		return activityName;
	}

	@JavascriptInterface
	public void setActivityName (String activityName)
	{
		this.activityName = activityName;
	}

	@JavascriptInterface
	public String getLabel ()
	{
		return this.label;
	}

	@JavascriptInterface
	public AppIcon getIcon ()
	{
		return this.icon;
	}

	@JavascriptInterface
	public void setIcon (AppIcon icon)
	{
		this.icon = icon;
	}

	@JavascriptInterface
	public void setLabel (String label)
	{
		this.label = label;
	}

	@JavascriptInterface
	public long getTimesLaunched ()
	{
		SharedPreferences prefs = MainActivity.getPrefs ();

		return prefs.getLong ("launched:" + this.getId (), 0);
	}

	@JavascriptInterface
	public String getDataFolder ()
	{
		return this.dataDir;
	}

	@JavascriptInterface
	public String getDescription ()
	{
		return this.description;
	}

	@JavascriptInterface
	public void setDescription (String description)
	{
		this.description = description;
	}

	@JavascriptInterface
	public String getDataDir ()
	{
		return dataDir;
	}

	@JavascriptInterface
	public void setDataDir (String dataDir)
	{
		this.dataDir = dataDir;
	}

	@JavascriptInterface
	public int getTargetSdk ()
	{
		return targetSdk;
	}

	@JavascriptInterface
	public void setTargetSdk (int targetSdk)
	{
		this.targetSdk = targetSdk;
	}

	@JavascriptInterface
	public void launch ()
	{
		this.launch (true);
	}

	@JavascriptInterface
	public void launch (boolean countLaunch)
	{
		if (countLaunch)
		{
			SharedPreferences prefs = MainActivity.getPrefs ();
			SharedPreferences.Editor editor = prefs.edit ();

			editor.putLong ("launched:" + this.getId (), prefs.getLong ("launched:" + this.getId (), 0) + 1);

			editor.apply ();
		}

		//AppManager.getRecentStatic ().shift (this);

		ComponentName compName = new ComponentName (this.packageName, this.activityName);
		Intent intent = new Intent (Intent.ACTION_MAIN);
		intent.addCategory (Intent.CATEGORY_LAUNCHER);
		intent.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		intent.setComponent (compName);

		MainActivity.getContext ().startActivity (intent);
	}

	@Override
	@JavascriptInterface
	public int hashCode ()
	{
		return this.hashCode (true);
	}

	@JavascriptInterface
	public int hashCode (boolean withIcon)
	{
		return new AppLauncherSimplified (this, withIcon).hashCode ();
	}

	@JavascriptInterface
	public boolean equals (AppLauncher compareTo)
	{
		boolean equal = false;

		if (this.hashCode () == compareTo.hashCode ())
			equal = true;

		return equal;
	}

	@JavascriptInterface
	public String toHtml (String htmlClass)
	{
		return this.toHtml (htmlClass, null, false);
	}

	@JavascriptInterface
	public String toHtml (String[][] data)
	{
		return this.toHtml ("", data, false);
	}

	@JavascriptInterface
	public String toHtml (String htmlClass, String[][] data)
	{
		return this.toHtml (htmlClass, data, false);
	}

	@JavascriptInterface
	public String toHtml (String htmlClass, String[][] data, boolean backgroundColour)
	{
		return Html.appLauncher (this, htmlClass, data, backgroundColour);
	}

	@JavascriptInterface
	public String infoToHtml ()
	{
		return Html.appInfo (this);
	}

	@JavascriptInterface
	protected void loadFromResolveInfo (ResolveInfo resInf)
	{
		this.loadFromResolveInfo (resInf, false);
	}

	@JavascriptInterface
	protected void loadFromResolveInfo (ResolveInfo resInf, boolean respectPreferredOrder)
	{
		ApplicationInfo info = resInf.activityInfo.applicationInfo;
		PackageManager pacMan = MainActivity.getContext ().getPackageManager ();
		CharSequence description = pacMan.getText (resInf.activityInfo.packageName, info.descriptionRes, info);

		this.label = resInf.loadLabel (MainActivity.getContext ().getPackageManager ()).toString ();
		this.packageName = resInf.activityInfo.applicationInfo.packageName;
		this.activityName = resInf.activityInfo.name;
		this.icon = new AppIcon (resInf, this);
		this.description = (description == null ? "" : description.toString ());
		this.dataDir = info.dataDir;
		this.targetSdk = info.targetSdkVersion;
	}

	@JavascriptInterface
	public int getId ()
	{
		return this.hashCode (false);
	}
}

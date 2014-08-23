package be.robinj.ubuntu;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import be.robinj.ubuntu.unity.AppIcon;

/**
 * Created by robin on 8/20/14.
 */
public class App
{
	private String label;
	private transient AppIcon icon;
	private String description;
	private String packageName;
	private String activityName;

	private transient Context context;
	private transient AppManager appManager;

	public static App fromResolveInfo (Context context, AppManager appManager, ResolveInfo resInf)
	{
		ApplicationInfo info = resInf.activityInfo.applicationInfo;
		PackageManager pacMan = context.getPackageManager ();
		CharSequence csDescription = pacMan.getText (resInf.activityInfo.packageName, info.descriptionRes, info);

		String label = resInf.loadLabel (pacMan).toString ();
		String packageName = resInf.activityInfo.applicationInfo.packageName;
		String activityName = resInf.activityInfo.name;
		AppIcon icon = new AppIcon (resInf.loadIcon (pacMan));
		String description = (csDescription == null ? "" : csDescription.toString ());

		App app = new App (context, appManager);
		app.setLabel (label);
		app.setPackageName (packageName);
		app.setActivityName (activityName);
		app.setIcon (icon);
		app.setDescription (description);

		return app;
	}

	public App (Context context, AppManager appManager)
	{
		this.context = context;
		this.appManager = appManager;
	}

	public void launch ()
	{
		ComponentName compName = new ComponentName (this.packageName, this.activityName);
		Intent intent = new Intent (Intent.ACTION_MAIN);
		intent.addCategory (Intent.CATEGORY_LAUNCHER);
		intent.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		intent.setComponent (compName);

		this.context.startActivity (intent);
	}

	//# Getters & Setters #//
	public String getLabel ()
	{
		return label;
	}

	public void setLabel (String label)
	{
		this.label = label;
	}

	public AppIcon getIcon ()
	{
		return icon;
	}

	public void setIcon (AppIcon icon)
	{
		this.icon = icon;
	}

	public String getDescription ()
	{
		return description;
	}

	public void setDescription (String description)
	{
		this.description = description;
	}

	public String getPackageName ()
	{
		return packageName;
	}

	public void setPackageName (String packageName)
	{
		this.packageName = packageName;
	}

	public String getActivityName ()
	{
		return activityName;
	}

	public void setActivityName (String activityName)
	{
		this.activityName = activityName;
	}

	public AppManager getAppManager ()
	{
		return appManager;
	}
}

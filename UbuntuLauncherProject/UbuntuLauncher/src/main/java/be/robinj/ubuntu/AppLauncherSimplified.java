package be.robinj.ubuntu;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.io.File;

/**
 * Created by robin on 7/30/14.
 */
public class AppLauncherSimplified // AppLauncher, but safe to serialize to JSON //
{
	private String label;

	private String icon;
	private String description;

	private String packageName;
	private String activityName;
	private String dataDir;
	private int targetSdk;

	public AppLauncherSimplified (AppLauncher appLauncher)
	{
		this (appLauncher, true);
	}

	public AppLauncherSimplified (AppLauncher appLauncher, boolean withIcon)
	{
		this.label = appLauncher.getLabel ();
		if (withIcon)
			this.icon = appLauncher.getIcon ().getPath ();
		this.packageName = appLauncher.getPackageName ();
		this.activityName = appLauncher.getActivityName ();
		this.description = appLauncher.getDescription ();
		this.dataDir = appLauncher.getDataDir ();
		this.targetSdk = appLauncher.getTargetSdk ();
	}

	public AppLauncher toAppLauncher ()
	{
		AppLauncher appLauncher = new AppLauncher ();
		appLauncher.setLabel (this.label);
		appLauncher.setPackageName (this.packageName);
		appLauncher.setActivityName (this.activityName);
		appLauncher.setDescription (this.description);
		appLauncher.setDataDir (this.dataDir);
		appLauncher.setTargetSdk (this.targetSdk);

		if (this.icon != null)
		{
			File file = new File (this.icon);

			AppIcon appIcon = null;

			if (! file.exists () || file.length () == 0)
			{
				PackageManagerExt pacMan = new PackageManagerExt ();

				appIcon = new AppIcon (pacMan.recoverIcon (this.packageName));
			}
			else
			{
				appIcon = new AppIcon (this.icon);
			}

			appIcon.setApp (appLauncher);
			appLauncher.setIcon (appIcon);
		}

		return appLauncher;
	}

	@Override
	public int hashCode ()
	{
		return this.toJSON ().hashCode ();
	}

	public String toJSON ()
	{
		Gson gson = new Gson ();

		return gson.toJson (this);
	}

	public static AppLauncherSimplified fromJSON (String json)
	{
		Gson gson = new Gson ();

		return gson.fromJson (json, AppLauncherSimplified.class);
	}
}

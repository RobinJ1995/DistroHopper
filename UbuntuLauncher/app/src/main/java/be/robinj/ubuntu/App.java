package be.robinj.ubuntu;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import be.robinj.ubuntu.unity.AppIcon;

/**
 * Created by robin on 8/20/14.
 */
public class App implements Parcelable
{
	private String label;
	private AppIcon icon;
	private String description;
	private String packageName;
	private String activityName;

	private Context context;
	private AppManager appManager;

	public static App fromResolveInfo (Context context, AppManager appManager, ResolveInfo resInf)
	{
		PackageManager pacMan = context.getPackageManager ();

		String label = resInf.loadLabel (pacMan).toString ();
		String packageName = resInf.activityInfo.applicationInfo.packageName;
		String activityName = resInf.activityInfo.name;

		App app = new App (context, appManager);
		app.setLabel (label);
		app.setPackageName (packageName);
		app.setActivityName (activityName);

		AppIcon icon = null;
		if (appManager.isIconPackLoaded ())
			icon = appManager.getIconPack ().getIconForApp (app);
		if (icon == null)
			icon = appManager.getIconPack ().getFallbackIcon (resInf.loadIcon (pacMan));

		app.setIcon (icon);

		return app;
	}

	public App (Context context, AppManager appManager)
	{
		this.context = context;
		this.appManager = appManager;
	}

	private App (Context context)
	{
		this.context = context;
	}

	private App (Parcel parcel)
	{
		this.activityName = parcel.readString ();
		this.description = parcel.readString ();
		this.label = parcel.readString ();
		this.packageName = parcel.readString ();
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

	//# Parcelable #//
	@Override
	public int describeContents ()
	{
		return 0;
	}

	@Override
	public void writeToParcel (Parcel dest, int flags)
	{
		dest.writeString (this.activityName);
		dest.writeString (this.description);
		dest.writeString (this.label);
		dest.writeString (this.packageName);
	}

	public static final Parcelable.Creator<App> CREATOR = new Parcelable.Creator <App> ()
	{
		public App createFromParcel (Parcel parcel)
		{
			return new App (parcel);
		}

		public App[] newArray (int size)
		{
			return new App[size];
		}
	};

	public void fixAfterUnpackingFromParcel (Context context)
	{
		this.context = context;

		PackageManager pacMan = context.getPackageManager ();

		Intent intent = new Intent ();
		intent.setComponent (new ComponentName (this.packageName, this.activityName));
		ResolveInfo resInf = pacMan.resolveActivity (intent, 0);

		this.icon = new AppIcon (resInf.loadIcon (pacMan));
	}
}

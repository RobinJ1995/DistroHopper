package be.robinj.distrohopper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.Toast;

import be.robinj.distrohopper.cache.StringCache;
import be.robinj.distrohopper.desktop.AppIcon;
import be.robinj.distrohopper.desktop.dash.AppLauncher;

/**
 * Created by robin on 8/20/14.
 */
public class App implements Parcelable
{
	private String label;
	private transient AppIcon icon;
	private String description;
	private String packageName;
	private String activityName;

	private transient ResolveInfo resInf = null;
	private boolean labelLoaded = false;
	private boolean iconLoaded = false;

	private transient Context context;
	private transient AppManager appManager;

	public App (Context context, AppManager appManager, ResolveInfo resInf)
	{
		this.context = context;
		this.appManager = appManager;
		this.resInf = resInf;

		this.packageName = resInf.activityInfo.applicationInfo.packageName;
		this.activityName = resInf.activityInfo.name;
	}

	public App (Context context, AppManager appManager, ResolveInfo resInf, StringCache appLabelCache)
	{
		this(context, appManager, resInf);

		final String label = appLabelCache.get(this.getPackageAndActivityName());
		if (label != null) {
			this.label = label;
		}
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
		if (HomeActivity.modeCustomise)
		{
			Toast.makeText (this.context, "App launching disabled while customising UI.", Toast.LENGTH_SHORT).show (); //TODO// getString () //
			
			return;
		}
		
		ComponentName compName = new ComponentName (this.packageName, this.activityName);
		Intent intent = new Intent (Intent.ACTION_MAIN);
		intent.addCategory (Intent.CATEGORY_LAUNCHER);
		intent.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		intent.setComponent (compName);

		this.context.startActivity (intent);
	}

	@Override
	public boolean equals (Object obj)
	{
		if (! (obj instanceof App))
			return false;
		else if (obj == this)
			return true;

		App app = (App) obj;

		return (this.getPackageName ().equals (app.getPackageName ()) && this.getActivityName ().equals (app.getActivityName ()));
	}

	//# Getters & Setters #//
	public String getLabel() {
		return this.getLabel(true);
	}

	public String getLabel(final boolean useCached) {
		if (this.label == null || (!this.labelLoaded && !useCached)) {
			this.label = this.resInf.activityInfo.loadLabel(this.getPackageManager()).toString();
			this.labelLoaded = true;
		}

		return this.label;
	}

	public boolean isLabelLoaded() {
		return this.labelLoaded;
	}

	public boolean setLabel(final String label, final StringCache appLabelCache) {
		final String old = this.label;

		this.label = label;
		this.labelLoaded = true;

		if (!old.equals(label)) {
			appLabelCache.put(this.getPackageAndActivityName(), label);

			return true;
		}

		return false;
	}

	public AppIcon getIcon ()
	{
		if (! this.iconLoaded) {
			AppIcon icon = null;
			if (this.appManager.isIconPackLoaded ()) {
				icon = this.appManager.getIconPack().getIconForApp(this);
			}
			if (icon == null) {
				icon = this.appManager.getIconPack().getFallbackIcon(this.resInf.loadIcon(this.getPackageManager()));
			}

			this.icon = icon;
			this.iconLoaded = true;
		}

		return this.icon;
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

	public String getActivityName ()
	{
		return activityName;
	}

	public String getPackageAndActivityName() {
		return new StringBuilder(this.getPackageName())
				.append("\n")
				.append(this.getActivityName())
				.toString();
	}

	public AppManager getAppManager ()
	{
		return appManager;
	}

	public AppLauncher getDashAppLauncher ()
	{
		return new AppLauncher (this.context, this);
	}

	private PackageManager getPackageManager() {
		return this.context.getPackageManager ();
	}

	//# Parcelable, Serializable #//
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
		dest.writeString (this.getLabel());
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
}

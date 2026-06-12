package be.robinj.distrohopper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.Toast;

import java.util.Objects;

import be.robinj.distrohopper.cache.ICache;
import be.robinj.distrohopper.desktop.AppIcon;
import be.robinj.distrohopper.desktop.dash.AppLauncher;
import be.robinj.distrohopper.dev.Log;

import static java.lang.String.format;


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
	private transient Intent launchIntent = null;
	private boolean labelLoaded = false;
	private boolean iconLoaded = false;
	private boolean launchAllowedInCustomiseMode = false;
	private boolean internalShortcut = false;

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

	public App(final Context context, final AppManager appManager, final ResolveInfo resInf,
			   final ICache<String> appLabelCache, final ICache<Drawable> iconCache)
	{
		this(context, appManager, resInf);

		final String packageAndActivityName = this.getPackageAndActivityName();
		final String label = appLabelCache.get(packageAndActivityName);
		if (label != null) {
			this.label = label;
		}
		final Drawable icon = iconCache.get(packageAndActivityName);
		if (icon != null) {
			this.icon = new AppIcon(icon);
		}
	}

	private App (Context context, AppManager appManager, String packageName, String activityName,
				 String label, Intent launchIntent, boolean launchAllowedInCustomiseMode)
	{
		this.context = context;
		this.appManager = appManager;
		this.packageName = packageName;
		this.activityName = activityName;
		this.label = label;
		this.launchIntent = launchIntent;
		this.launchAllowedInCustomiseMode = launchAllowedInCustomiseMode;
		this.internalShortcut = true;
		this.labelLoaded = label != null;
	}

	/**
	 * Creates a DistroHopper-internal shortcut that lives only in the dash and
	 * launches by explicit intent rather than a public launcher component.
	 */
	public static App internalShortcut (Context context, AppManager appManager,
			String packageName, String activityName, String label,
			Intent launchIntent, boolean launchAllowedInCustomiseMode)
	{
		return new App (context, appManager, packageName, activityName,
				label, launchIntent, launchAllowedInCustomiseMode);
	}

	private App (Parcel parcel)
	{
		this.activityName = parcel.readString ();
		this.description = parcel.readString ();
		this.label = parcel.readString ();
		this.packageName = parcel.readString ();
		this.launchAllowedInCustomiseMode = parcel.readInt () != 0;
		this.internalShortcut = parcel.readInt () != 0;

		if (this.internalShortcut) {
			this.launchIntent = new Intent ()
					.setComponent (new ComponentName (this.packageName, this.activityName))
					.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		}
	}

	public void launch ()
	{
		if ((! this.launchAllowedInCustomiseMode) && DependencyContainer.of (this.context).getCustomiseMode ().getValue ()) {
			Toast.makeText(this.context, "App launching disabled while customising UI.", Toast.LENGTH_SHORT).show(); //TODO// getString () //

			return;
		}

		try {
			final Intent intent;
			if (this.launchIntent != null) {
				intent = new Intent (this.launchIntent);
			}
			else {
				final ComponentName compName = new ComponentName(this.packageName, this.activityName);
				intent = new Intent(Intent.ACTION_MAIN);
				intent.addCategory(Intent.CATEGORY_LAUNCHER);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
				intent.setComponent(compName);
			}

			this.context.startActivity(intent);
		} catch (final Exception ex) {
			final String errorMessage = format("Failed to launch %s/%s: %s.",
					this.packageName, this.activityName, ex.getClass().getSimpleName());
			Log.getInstance().e("App", errorMessage);
			Toast.makeText(this.context, errorMessage, Toast.LENGTH_SHORT).show(); //TODO// getString () //
		}
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

	@Override
	public int hashCode() {
		return Objects.hash(this.packageName, this.activityName);
	}

	//# Getters & Setters #//
	public String getLabel() {
		return this.getLabel(true);
	}

	public String getLabel(final boolean useCached) {
		if (this.label == null || (!this.labelLoaded && !useCached)) {
			if (this.resInf != null) {
				this.label = this.resInf.activityInfo.loadLabel(this.getPackageManager()).toString();
				this.labelLoaded = true;
			} else {
				Log.getInstance().w("App", format("getLabel called on internal shortcut %s/%s with no label set",
						this.packageName, this.activityName));
			}
		}

		return this.label;
	}

	public boolean isLabelLoaded() {
		return this.labelLoaded;
	}

	public boolean setLabel(final String label, final ICache<String> appLabelCache) {
		final String old = this.label;

		this.label = label;
		this.labelLoaded = true;

		if (!Objects.equals(old, label) || !appLabelCache.containsKey(this.getPackageAndActivityName())) {
			appLabelCache.put(this.getPackageAndActivityName(), label);

			return true;
		}

		return false;
	}

	public AppIcon getIcon() {
		return this.getIcon(true);
	}

	public AppIcon getIcon (boolean useCached)
	{
		if (this.icon == null || (!this.iconLoaded && !useCached)) {
			AppIcon icon = null;
			if (this.appManager.isIconPackLoaded ()) {
				icon = this.appManager.getIconPack().getIconForApp(this);
			}
			if (icon == null) {
				icon = this.appManager.getIconPack().getFallbackIcon(this.loadFallbackIcon());
			}

			this.icon = icon;
			this.iconLoaded = true;
		}

		return this.icon;
	}

	private Drawable loadFallbackIcon ()
	{
		if (this.resInf != null) {
			return this.resInf.loadIcon(this.getPackageManager());
		}

		// Internal shortcuts have no ResolveInfo; use the application icon.
		return this.context.getApplicationInfo ().loadIcon (this.getPackageManager ());
	}

	public boolean setIcon(final AppIcon icon, final ICache<Drawable> appIconCache) {
		this.icon = icon;
		this.iconLoaded = true;

		if (! appIconCache.containsKey(this.getPackageAndActivityName())) { // There's no proper way to check equality without comparing all pixels
			appIconCache.put(this.getPackageAndActivityName(), icon.getDrawable());

			return true;
		}

		return false;
	}

	public boolean isIconLoaded() {
		return this.iconLoaded;
	}

	public boolean isInternalShortcut() {
		return internalShortcut;
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
		dest.writeInt (this.launchAllowedInCustomiseMode ? 1 : 0);
		dest.writeInt (this.internalShortcut ? 1 : 0);
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

package be.robinj.distrohopper;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.UserHandle;
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
	private transient LauncherActivityInfo launcherActivityInfo = null;
	private transient Intent launchIntent = null;
	/** null = the personal profile; set = the (work) profile this app lives in. */
	private UserHandle user = null;
	/** The profile's stable serial number; -1 for the personal profile. */
	private long userSerial = -1;
	private boolean labelLoaded = false;
	private boolean iconLoaded = false;
	private boolean launchAllowedInCustomiseMode = false;
	private boolean internalShortcut = false;
	private int launchForResultRequestCode = -1;

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

		this.loadFromCaches(appLabelCache, iconCache);
	}

	/**
	 * An app in another profile (e.g. the work profile), as returned by
	 * LauncherApps. Apps in the personal profile keep the ResolveInfo path.
	 */
	public App (Context context, AppManager appManager, LauncherActivityInfo launcherActivityInfo)
	{
		this.context = context;
		this.appManager = appManager;
		this.launcherActivityInfo = launcherActivityInfo;

		this.packageName = launcherActivityInfo.getComponentName ().getPackageName ();
		this.activityName = launcherActivityInfo.getComponentName ().getClassName ();

		if (! Process.myUserHandle ().equals (launcherActivityInfo.getUser ())) {
			this.user = launcherActivityInfo.getUser ();
			this.userSerial = Profiles.serialOf (context, this.user);
		}
	}

	public App(final Context context, final AppManager appManager,
			   final LauncherActivityInfo launcherActivityInfo,
			   final ICache<String> appLabelCache, final ICache<Drawable> iconCache)
	{
		this(context, appManager, launcherActivityInfo);

		this.loadFromCaches(appLabelCache, iconCache);
	}

	private void loadFromCaches(final ICache<String> appLabelCache, final ICache<Drawable> iconCache) {
		final String key = this.getProfileScopedKey();
		final String label = appLabelCache.get(key);
		if (label != null) {
			this.label = label;
		}
		final Drawable icon = iconCache.get(key);
		if (icon != null) {
			this.icon = new AppIcon(icon);
		}
	}

	private App (Context context, AppManager appManager, String packageName, String activityName,
				 String label, Intent launchIntent, boolean launchAllowedInCustomiseMode,
				 int launchForResultRequestCode)
	{
		this.context = context;
		this.appManager = appManager;
		this.packageName = packageName;
		this.activityName = activityName;
		this.label = label;
		this.launchIntent = launchIntent;
		this.launchAllowedInCustomiseMode = launchAllowedInCustomiseMode;
		this.internalShortcut = true;
		this.launchForResultRequestCode = launchForResultRequestCode;
		this.labelLoaded = label != null;
	}

	/**
	 * Creates a DistroHopper-internal shortcut that lives only in the dash and
	 * launches by explicit intent rather than a public launcher component.
	 * launchForResultRequestCode >= 0 makes it launch through
	 * Activity.startActivityForResult() so the host activity sees the result.
	 */
	public static App internalShortcut (Context context, AppManager appManager,
			String packageName, String activityName, String label,
			Intent launchIntent, boolean launchAllowedInCustomiseMode,
			int launchForResultRequestCode)
	{
		return new App (context, appManager, packageName, activityName,
				label, launchIntent, launchAllowedInCustomiseMode,
				launchForResultRequestCode);
	}

	private App (Parcel parcel)
	{
		this.activityName = parcel.readString ();
		this.description = parcel.readString ();
		this.label = parcel.readString ();
		this.packageName = parcel.readString ();
		this.launchAllowedInCustomiseMode = parcel.readInt () != 0;
		this.internalShortcut = parcel.readInt () != 0;
		this.launchForResultRequestCode = parcel.readInt ();
		this.user = parcel.readParcelable (UserHandle.class.getClassLoader ());
		this.userSerial = parcel.readLong ();

		if (this.internalShortcut) {
			// No NEW_TASK flag: the target shares the home task's affinity, so
			// NEW_TASK would only bring the already-visible home task to the front //
			this.launchIntent = new Intent ()
					.setComponent (new ComponentName (this.packageName, this.activityName));
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

				if (this.launchForResultRequestCode >= 0 && this.context instanceof Activity) {
					// E.g. the Settings shortcut: HomeActivity.onActivityResult() turns
					// the Customise UI result into a relaunch with customise=true //
					((Activity) this.context).startActivityForResult (
							intent, this.launchForResultRequestCode);

					return;
				}
			}
			else if (this.user != null) {
				// Apps in another profile cannot be started with a regular intent;
				// LauncherApps starts them in their own profile //
				final LauncherApps launcherApps =
						(LauncherApps) this.context.getSystemService (Context.LAUNCHER_APPS_SERVICE);
				launcherApps.startMainActivity (
						new ComponentName (this.packageName, this.activityName),
						this.user, null, null);

				return;
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

		return (this.getPackageName ().equals (app.getPackageName ())
				&& this.getActivityName ().equals (app.getActivityName ())
				&& Objects.equals (this.user, app.user));
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.packageName, this.activityName, this.user);
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
			} else if (this.launcherActivityInfo != null) {
				// loadLabel rather than LauncherActivityInfo.getLabel(): the same
				// label source the ResolveInfo path uses for personal-profile apps //
				this.label = this.launcherActivityInfo.getActivityInfo()
						.loadLabel(this.getPackageManager()).toString();
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

		if (!Objects.equals(old, label) || !appLabelCache.containsKey(this.getProfileScopedKey())) {
			appLabelCache.put(this.getProfileScopedKey(), label);

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

			if (this.user != null && icon != null) {
				// Work-profile badge on whichever icon won (icon pack or fallback) //
				icon = new AppIcon (this.getPackageManager ()
						.getUserBadgedIcon (icon.getDrawable (), this.user));
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

		if (this.launcherActivityInfo != null) {
			// Unbadged: getIcon() applies the profile badge after icon-pack resolution //
			return this.launcherActivityInfo.getIcon (0);
		}

		// Internal shortcuts have no ResolveInfo; use the application icon.
		return this.context.getApplicationInfo ().loadIcon (this.getPackageManager ());
	}

	public boolean setIcon(final AppIcon icon, final ICache<Drawable> appIconCache) {
		this.icon = icon;
		this.iconLoaded = true;

		if (! appIconCache.containsKey(this.getProfileScopedKey())) { // There's no proper way to check equality without comparing all pixels
			appIconCache.put(this.getProfileScopedKey(), icon.getDrawable());

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

	/**
	 * The profile this app lives in; null for the personal profile. The same
	 * package can be installed in several profiles, so anything that
	 * identifies an app across profiles must combine this with the
	 * package/activity name (see {@link #getProfileScopedKey()}).
	 */
	public UserHandle getUser ()
	{
		return this.user;
	}

	/**
	 * Identity key including the profile: equal to
	 * {@link #getPackageAndActivityName()} for personal-profile apps (so
	 * existing persisted keys and caches keep matching), with the profile's
	 * serial number appended for apps in other profiles.
	 */
	public String getProfileScopedKey() {
		final String key = this.getPackageAndActivityName();

		return this.user == null ? key : key + "\n" + this.userSerial;
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
		dest.writeInt (this.launchForResultRequestCode);
		dest.writeParcelable (this.user, flags);
		dest.writeLong (this.userSerial);
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

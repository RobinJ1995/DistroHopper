package be.robinj.distrohopper;

import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import be.robinj.distrohopper.home.LauncherBarBinder;

/**
 * Created by robin on 8/20/14.
 *
 * Facade over the app model (AppRepository) and the launcher-bar/dash view
 * binding (home.LauncherBarBinder), keeping the API its many callers
 * (listeners, lenses, the broadcast receiver) already use.
 */
public class AppManager implements Iterable<App>
{
	private final AppRepository repository;

	private final IconPackHelper iconPack;

	private final HomeActivity parent;

	private LauncherBarBinder binder;

	public AppManager (HomeActivity parent)
	{
		this.parent = parent;
		this.repository = new AppRepository (parent);
		this.iconPack = new IconPackHelper (parent.getApplicationContext ());
	}

	/** Lazy so that AppManager can be constructed on a background thread. */
	private LauncherBarBinder getBinder ()
	{
		if (this.binder == null)
			this.binder = new LauncherBarBinder (this);

		return this.binder;
	}

	public AppRepository getRepository ()
	{
		return this.repository;
	}

	public void add (App app)
	{
		this.add (app, false, true);
	}

	public void add (App app, boolean checkDuplicate, boolean sortAndNotifyAdapter)
	{
		if (this.repository.add (app, checkDuplicate) && sortAndNotifyAdapter)
		{
			this.repository.sort ();
			this.getBinder ().notifyDashAdapterChanged ();
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

	public void add (LauncherActivityInfo launcherActivityInfo, boolean checkDuplicate, boolean sortAndNotifyAdapter)
	{
		this.add (new App(this.getContext (), this, launcherActivityInfo), checkDuplicate, sortAndNotifyAdapter);
	}

	/** Binds the dash app grid(s); see LauncherBarBinder.bindDashApps(). */
	public void bindDashApps ()
	{
		this.getBinder ().bindDashApps ();
	}

	/** Re-applies the dash grid's column count to the dash grid and pager pages. */
	public void applyDashColumns ()
	{
		this.getBinder ().applyDashColumns ();
	}

	/** The apps grid's last laid-out viewport (width, height) in px, or null. */
	public kotlin.Pair<Integer, Integer> dashGridViewport ()
	{
		return this.getBinder ().dashGridViewport ();
	}

	/** Notifies the dash that it opened or closed (for the profile indicator). */
	public void setDashOpen (boolean open)
	{
		this.getBinder ().setDashOpen (open);
	}

	public void addRunningApps (int colour)
	{
		this.getBinder ().addRunningApps (colour);
	}

	public App findAppByPackageAndActivityName (String packageName, String activityName)
	{
		return this.repository.findAppByPackageAndActivityName (packageName, activityName);
	}

	public List<App> findAppsByPackageName (String packageName)
	{
		return this.repository.findAppsByPackageName (packageName);
	}

	public App get (int index)
	{
		return this.repository.get (index);
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
		return this.repository.getInstalledLive ();
	}

	public Map<String, App> getInstalledAppsMap() {
		return this.repository.installedAppsMap ();
	}

	public HomeActivity getParent ()
	{
		return this.parent;
	}

	public List<App> getPinned ()
	{
		return this.repository.getPinnedLive ();
	}

	public List<App> getRunningApps ()
	{
		return this.repository.getRunningApps ();
	}

	public int indexOfPinned (App app)
	{
		return this.repository.indexOfPinned (app);
	}

	public boolean isIconPackLoaded ()
	{
		return this.iconPack.isIconPackLoaded ();
	}

	public boolean isPinned (App app)
	{
		return this.repository.isPinned (app);
	}

	public Iterator<App> iterator ()
	{
		return this.repository.getInstalledLive ().iterator ();
	}

	public void loadIconPack (String name) throws IOException, XmlPullParserException, PackageManager.NameNotFoundException
	{
		this.iconPack.loadIconPack (name);
	}

	public void movePinnedApp (int oldIndex, int newIndex)
	{
		this.repository.movePinnedApp (oldIndex, newIndex);
	}

	public boolean pin (App app)
	{
		return this.pin (app, true, true, true);
	}

	public boolean pin (App app, boolean save, boolean showToast, boolean addView)
	{
		if (this.repository.pin (app))
		{
			if (showToast)
				Toast.makeText (this.getContext (), app.getLabel () + " " + this.getContext ().getResources ().getString (R.string.pinned), Toast.LENGTH_SHORT).show ();

			if (addView)
				this.getBinder ().addPinnedAppView (app);

			if (save)
				this.savePinnedApps ();

			return true;
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
		return this.repository.queryInstalledApps (packageName);
	}

	public void refreshPinnedView ()
	{
		this.getBinder ().refreshPinnedView ();
	}

	public boolean remove (App app)
	{
		boolean modified = this.repository.remove (app);

		// Uninstalled: drop it from every desktop, not just the current one //
		this.repository.unpinFromAllDesktops (app);
		this.getBinder ().removePinnedAppView (app);
		this.savePinnedApps ();

		this.getBinder ().notifyDashAdapterChanged ();

		return modified;
	}

	//# Per-desktop pinned apps #//

	public void setCurrentDesktop (int desktop)
	{
		this.repository.setCurrentDesktop (desktop);
	}

	public int getCurrentDesktop ()
	{
		return this.repository.getCurrentDesktop ();
	}

	public boolean isPerDesktopPins ()
	{
		return this.repository.getPerDesktop ();
	}

	public List<App> pinnedOn (int desktop)
	{
		return this.repository.pinnedOn (desktop);
	}

	public boolean isPinnedOn (App app, int desktop)
	{
		return this.repository.isPinnedOn (app, desktop);
	}

	public int highestPinnedDesktop ()
	{
		return this.repository.highestPinnedDesktop ();
	}

	public void removePinnedDesktop (int desktop)
	{
		this.repository.removePinnedDesktop (desktop);
	}

	public void loadPinnedApps ()
	{
		this.repository.loadPinnedApps ();
	}

	public void onLauncherPageScroll (int fromPage, int toPage, float fraction)
	{
		this.getBinder ().onPageScroll (fromPage, toPage, fraction);
	}

	public void onLauncherPageSettled (int page)
	{
		this.getBinder ().showDesktop (page);
	}

	public void savePinnedApps ()
	{
		this.repository.savePinnedApps ();

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
		return this.repository.search (pattern, maxResults);
	}

	/** Like search(), but restricted to one profile (null = the personal profile). */
	public List<App> searchProfile (String pattern, final int maxResults, final UserHandle profile)
	{
		return this.repository.searchProfile (pattern, maxResults, profile);
	}

	/** The profiles the installed apps belong to; null = the personal profile. */
	public List<UserHandle> getProfiles ()
	{
		return this.repository.profiles ();
	}

	public int size ()
	{
		return this.repository.size ();
	}

	public void sort ()
	{
		this.repository.sort ();
	}

	public boolean unpin (int index)
	{
		return this.unpin (this.repository.getPinnedLive ().get (index));
	}

	public boolean unpin (App app)
	{
		return this.unpin (app, true);
	}

	public boolean unpin (App app, boolean showToast)
	{
		boolean modified = this.repository.unpin (app);

		if (showToast)
		{
			String message;
			if (modified)
				message = " " + this.getContext ().getResources ().getString (R.string.unpinned);
			else
				message = " " + this.getContext ().getResources ().getString (R.string.notpinned);

			Toast.makeText (this.getContext (), app.getLabel () + message, Toast.LENGTH_SHORT).show ();
		}

		this.getBinder ().removePinnedAppView (app);

		this.savePinnedApps ();

		return modified;
	}

	/*# Event handlers #*/
	public void startedDraggingPinnedApp ()
	{
		this.getBinder ().startedDraggingPinnedApp ();
	}

	public void startedDraggingPinnedApp (App app)
	{
		this.getBinder ().startedDraggingPinnedApp (app);
	}

	public void startedDraggingDashApp (App app)
	{
		this.getBinder ().startedDraggingDashApp (app);
	}

	public void draggedPinnedAppOver (App app)
	{
		this.getBinder ().draggedPinnedAppOver (app);
	}

	public void droppedPinnedApp ()
	{
		this.getBinder ().droppedPinnedApp ();
	}

	public void endedDraggingPinnedApp ()
	{
		this.getBinder ().endedDraggingPinnedApp ();
	}

	public void stoppedDraggingPinnedApp ()
	{
		this.getBinder ().stoppedDraggingPinnedApp ();
	}

	public void asyncLoadAppLabelsDone() {
		this.getBinder ().invalidateDashViews ();
	}

	public void asyncLoadAppIconsDone() {
		this.getBinder ().invalidateDashViews ();
	}
}

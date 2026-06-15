package be.robinj.distrohopper;

import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import be.robinj.distrohopper.cache.AppIconCache;
import be.robinj.distrohopper.icons.IconConfig;
import be.robinj.distrohopper.icons.IconRenderer;
import be.robinj.distrohopper.preferences.Preference;
import be.robinj.distrohopper.preferences.Preferences;
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

	private final DashLayoutRepository dashLayout;

	private final LauncherLayoutRepository launcherLayout;

	private final IconPackHelper iconPack;

	private IconRenderer iconRenderer;

	private final HomeActivity parent;

	private LauncherBarBinder binder;

	public AppManager (HomeActivity parent)
	{
		this.parent = parent;
		this.repository = new AppRepository (parent);
		this.dashLayout = new DashLayoutRepository (parent, this.repository);
		this.launcherLayout = new LauncherLayoutRepository (parent, this.repository);
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

	public DashLayoutRepository getDashLayout ()
	{
		return this.dashLayout;
	}

	/** Loads the persisted dash folders + custom order; call once apps are loaded. */
	public void loadDashLayout ()
	{
		this.dashLayout.load ();
	}

	/** Refreshes the dash grid after a folder / custom-order change. */
	public void dashLayoutChanged ()
	{
		this.getBinder ().notifyDashAdapterChanged ();
	}

	public LauncherLayoutRepository getLauncherLayout ()
	{
		return this.launcherLayout;
	}

	/** Loads the persisted launcher folders + order; call once pins are loaded. */
	public void loadLauncherLayout ()
	{
		this.launcherLayout.load ();
	}

	/** Rebuilds the pinned bar after a launcher folder / order change. */
	public void launcherLayoutChanged ()
	{
		this.getBinder ().refreshPinnedView ();
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

	/**
	 * The renderer that shapes adaptive icons to the current {@link IconConfig}.
	 * Rebuilt whenever the icon-shape/themed preferences change (in-process or
	 * across restarts); a config change also clears the shape-unaware icon cache
	 * so no stale-shaped icon can be served afterwards.
	 */
	public synchronized IconRenderer getIconRenderer ()
	{
		final IconConfig config = IconConfig.fromPrefs (this.parent.getApplicationContext ());

		if (this.iconRenderer == null
			|| ! this.iconRenderer.getConfig ().signature ().equals (config.signature ()))
		{
			this.reconcileIconCache (config);
			this.iconRenderer = new IconRenderer (this.parent.getApplicationContext (), config);
		}

		return this.iconRenderer;
	}

	/** Clear the icon cache when the rendered-output signature no longer matches what was cached. */
	private void reconcileIconCache (final IconConfig config)
	{
		final android.content.SharedPreferences prefs =
			Preferences.getSharedPreferences (this.parent.getApplicationContext ());
		final String stored = prefs.getString (Preference.ICON_CONFIG_SIGNATURE.getName (), null);

		if (! config.signature ().equals (stored))
		{
			try
			{
				AppIconCache.clearAll (this.parent.getApplicationContext ());
			}
			catch (final Exception ex)
			{
				new ExceptionHandler (ex).logAndTrack ();
			}

			prefs.edit ()
				.putString (Preference.ICON_CONFIG_SIGNATURE.getName (), config.signature ())
				.apply ();
		}
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
		return this.pin (app, true, true);
	}

	public boolean pin (App app, boolean save, boolean addView)
	{
		if (this.repository.pin (app))
		{
			if (addView)
				this.getBinder ().addPinnedAppView (app);

			if (save)
				this.savePinnedApps ();

			return true;
		}
		else
		{
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

		// And remove its desktop pin, if any //
		if (this.parent.getDesktopAppHost () != null)
			this.parent.getDesktopAppHost ().unpinFromAllDesktops (app);

		// And drop it from any desktop folder it lived in //
		if (this.parent.getDesktopFolderHost () != null)
			this.parent.getDesktopFolderHost ().unpinFromAllDesktops (app);

		// Drop it from any dash / launcher folder / order slot it lived in //
		this.dashLayout.reconcile ();
		this.launcherLayout.reconcile ();

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

	/**
	 * Re-sorts the dash for the usage-based orders ("most recently used" / "most
	 * used") and refreshes the grid, so an app launched since the dash was last
	 * built moves to its new position the next time the dash opens. A no-op for
	 * the alphabetical order, whose result never changes between loads.
	 */
	public void refreshDashSortOrder ()
	{
		if (this.repository.isUsageBasedSortOrder ())
		{
			this.repository.sort ();
			this.getBinder ().notifyDashAdapterChanged ();
		}
	}

	public boolean unpin (int index)
	{
		return this.unpin (this.repository.getPinnedLive ().get (index));
	}

	public boolean unpin (App app)
	{
		boolean modified = this.repository.unpin (app);

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

	/** Begins dragging an app pulled out of launcher folder [folderId], so it
	 *  behaves like dragging the pin itself (reposition / fold / trash). */
	public void startedDraggingLauncherFolderMember (String folderId, App app)
	{
		this.getBinder ().startedDraggingLauncherFolderMember (folderId, app);
	}

	public void draggedPinnedItemOver (android.view.View targetView)
	{
		this.getBinder ().draggedPinnedItemOver (targetView);
	}

	/** The drag is hovering [targetView]: preview the reorder and arm a dwell-fold. */
	public void hoverPinnedItem (android.view.View targetView)
	{
		this.getBinder ().hoverPinnedItem (targetView);
	}

	/** Whether the dragged pinned item could fold onto [targetView]. */
	public boolean canFoldOnto (android.view.View targetView)
	{
		return this.getBinder ().canFoldOnto (targetView);
	}

	/** Preview a reorder/pin insertion before/after [targetView] (null = end of bar). */
	public void previewPinnedInsert (android.view.View targetView, boolean after)
	{
		this.getBinder ().previewPinnedInsert (targetView, after);
	}

	/** Preview a fold onto [targetView] (ring it, collapse the insertion gap). */
	public void previewPinnedFold (android.view.View targetView)
	{
		this.getBinder ().previewPinnedFold (targetView);
	}

	/** Commits a dwell-armed fold if one is pending; @return whether it folded. */
	public boolean dropPinnedFold ()
	{
		return this.getBinder ().dropPinnedFold ();
	}

	/** Cancels any pending dwell-fold. */
	public void cancelPinnedFold ()
	{
		this.getBinder ().cancelPinnedFold ();
	}

	public void draggedPinnedAppOver (App app)
	{
		this.getBinder ().draggedPinnedAppOver (app);
	}

	/** Reorders the desktop's pins to match the launcher bar's flattened item order. */
	public void reorderPinned (int desktop, java.util.List<String> orderedKeys)
	{
		this.repository.reorderPinned (desktop, orderedKeys);
	}

	/** @return whether the dragged app was folded onto the target (else reorder). */
	public boolean foldDraggedOnto (android.view.View targetView)
	{
		return this.getBinder ().foldDraggedOnto (targetView);
	}

	public void startedDraggingFolder (String folderId)
	{
		this.getBinder ().startedDraggingFolder (folderId);
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

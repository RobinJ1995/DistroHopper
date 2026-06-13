package be.robinj.distrohopper

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ResolveInfo
import android.os.UserHandle
import be.robinj.distrohopper.preferences.AppSortOrder
import be.robinj.distrohopper.preferences.LauncherPinMode
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The model behind the launcher and the dash: the installed and pinned app
 * lists and the operations on them. No views — the view half lives in
 * home/LauncherBarBinder, and AppManager is the facade gluing the two
 * together for the existing callers.
 *
 * The live lists are intentionally exposed ([installedLive], [pinnedLive])
 * because the dash GridAdapter is backed by the installed list directly;
 * [installed]/[pinned] emit snapshots on every change for reactive
 * consumers.
 */
class AppRepository(private val context: Context) {
	private val apps = CopyOnWriteArrayList<App>()

	/*
	 * Pinned apps per desktop: index = desktop. In global mode (perDesktop ==
	 * false) only desktop 0 is ever used and every desktop maps onto it, so the
	 * behaviour is identical to a single shared list. The no-desktop pin/unpin
	 * operations act on [currentDesktop], the desktop the launcher is showing.
	 */
	private val pinnedByPage = CopyOnWriteArrayList<CopyOnWriteArrayList<App>>()

	/** Whether pins are kept per desktop; false collapses everything onto desktop 0. */
	var perDesktop: Boolean = false
		private set

	/** The desktop the launcher is currently showing (target of the no-desktop ops). */
	var currentDesktop: Int = 0
		private set

	private val _installed = MutableStateFlow<List<App>>(emptyList())
	val installed: StateFlow<List<App>> = this._installed.asStateFlow()

	private val _pinned = MutableStateFlow<List<App>>(emptyList())
	val pinned: StateFlow<List<App>> = this._pinned.asStateFlow()

	val installedLive: List<App> get() = this.apps
	val pinnedLive: List<App> get() = this.pageList(this.currentDesktop)

	/** @return whether the app was added (false if [checkDuplicate] found it). */
	fun add(app: App, checkDuplicate: Boolean): Boolean {
		if (checkDuplicate && this.apps.contains(app)) {
			return false
		}

		this.apps.add(app)
		this.installedChanged()

		return true
	}

	/** Removes [app] from the installed list (not from the pinned list). */
	fun remove(app: App): Boolean {
		val modified = this.apps.remove(app)
		this.installedChanged()

		return modified
	}

	fun sort() {
		val order = AppSortOrder.current(Preferences.getSharedPreferences(this.context))
		this.apps.sortWith(AppComparators.forOrder(order, AppUsageStats(this.context)))
		this.installedChanged()
	}

	/**
	 * Whether the active sort order ranks by usage data (so it goes stale as apps
	 * are launched and the dash should be re-sorted when it next opens).
	 */
	fun isUsageBasedSortOrder(): Boolean =
		AppSortOrder.current(Preferences.getSharedPreferences(this.context)).usesUsageData

	fun size(): Int = this.apps.size

	operator fun get(index: Int): App = this.apps[index]

	fun findAppByPackageAndActivityName(packageName: String, activityName: String): App? =
		this.apps.firstOrNull {
			packageName == it.packageName && activityName == it.activityName
		}

	fun findAppsByPackageName(packageName: String): List<App> =
		this.apps.filter { packageName == it.packageName }

	fun installedAppsMap(): Map<String, App> =
		this.apps.associateBy { it.profileScopedKey }

	fun queryInstalledApps(packageName: String?): List<ResolveInfo> {
		val mainIntent = Intent(Intent.ACTION_MAIN)
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
		if (packageName != null) {
			mainIntent.setPackage(packageName)
		}

		return this.context.packageManager.queryIntentActivities(mainIntent, 0)
	}

	/**
	 * Launcher entries of every profile other than the personal one (e.g. the
	 * work profile). Package broadcasts only cover the personal profile, so
	 * these come from LauncherApps instead of PackageManager.
	 */
	fun queryOtherProfileApps(): List<LauncherActivityInfo> {
		val launcherApps = this.context.getSystemService(LauncherApps::class.java)
			?: return emptyList()

		// Skip our own package the way the personal-profile load does
		// (AppsLoader): a DistroHopper installed in another profile would
		// otherwise show up as an app in that profile's dash tab, and tapping
		// it would launch HomeActivity in that profile straight into the
		// first-run wizard. A second launcher inside a work profile is never
		// useful anyway.
		return Profiles.otherProfiles(this.context)
			.flatMap { launcherApps.getActivityList(null, it) }
			.filter { it.componentName.packageName != this.context.packageName }
	}

	/**
	 * The distinct profiles ("profiles") the installed apps belong to: always the
	 * personal profile (null) first, the other profiles after it ordered by their
	 * stable profile serial. The order is deliberately independent of the app
	 * list's order — deriving it from the (usage-)sorted [apps] directly would let
	 * a launch reorder the dash's profile pages.
	 */
	fun profiles(): List<UserHandle?> {
		val others = this.apps.mapNotNull { it.user }
			.distinct()
			.sortedBy { Profiles.serialOf(this.context, it) }

		return listOf<UserHandle?>(null) + others
	}

	fun appsForProfile(user: UserHandle?): List<App> =
		this.apps.filter { user == it.user }

	/**
	 * Search apps based on the provided pattern: label-prefix matches first,
	 * then infix matches when the full-search preference is enabled.
	 *
	 * @param maxResults Maximum number of results to return.
	 *   NOTE: This is ignored when the pattern is empty.
	 */
	fun search(pattern: String, maxResults: Int): List<App> =
		this.search(pattern, maxResults) { true }

	/** Like [search], but restricted to one profile (null = the personal profile). */
	fun searchProfile(pattern: String, maxResults: Int, profile: UserHandle?): List<App> =
		this.search(pattern, maxResults) { profile == it.user }

	private fun search(pattern: String, maxResults: Int, filter: (App) -> Boolean): List<App> {
		if (pattern.isEmpty()) {
			return this.apps.filter(filter)
		}

		val results = ArrayList<App>()
		val fullSearch = Preferences.getSharedPreferences(this.context)
			.getBoolean(Preference.DASH_SEARCH_FULL.getName(), true)
		val lowered = pattern.lowercase()

		for (app in this.apps) {
			if (filter(app) && app.label.lowercase().startsWith(lowered)) {
				results.add(app)

				if (results.size >= maxResults) {
					return results
				}
			}
		}

		if (fullSearch) {
			for (app in this.apps) {
				if (filter(app) && (! results.contains(app))
						&& app.label.lowercase().contains(lowered)) {
					results.add(app)

					if (results.size >= maxResults) {
						return results
					}
				}
			}
		}

		return results
	}

	//# Pinned apps — desktop-aware operations #//

	private fun pageIndex(desktop: Int): Int =
		if (this.perDesktop) desktop.coerceAtLeast(0) else 0

	/** The list for [desktop], creating it (and any before it) as needed. */
	private fun pageList(desktop: Int): CopyOnWriteArrayList<App> {
		val idx = this.pageIndex(desktop)
		while (this.pinnedByPage.size <= idx) {
			this.pinnedByPage.add(CopyOnWriteArrayList())
		}

		return this.pinnedByPage[idx]
	}

	private fun pageListOrEmpty(desktop: Int): List<App> =
		this.pinnedByPage.getOrNull(this.pageIndex(desktop)) ?: emptyList()

	fun pinnedOn(desktop: Int): List<App> = this.pageListOrEmpty(desktop)

	fun isPinnedOn(app: App, desktop: Int): Boolean = this.pageListOrEmpty(desktop).contains(app)

	fun indexOfPinnedOn(app: App, desktop: Int): Int = this.pageListOrEmpty(desktop).indexOf(app)

	/** @return whether the app was newly pinned on [desktop]. */
	fun pin(app: App, desktop: Int): Boolean {
		if (this.isPinnedOn(app, desktop)) {
			return false
		}

		val added = this.pageList(desktop).add(app)
		this.pinnedChanged()

		return added
	}

	fun unpin(app: App, desktop: Int): Boolean {
		val modified = this.pinnedByPage.getOrNull(this.pageIndex(desktop))?.remove(app) ?: false
		this.pinnedChanged()

		return modified
	}

	fun movePinnedApp(desktop: Int, oldIndex: Int, newIndex: Int) {
		val list = this.pageList(desktop)
		val app = list.removeAt(oldIndex)
		list.add(newIndex, app)
		this.pinnedChanged()
	}

	/** Removes [app] from every desktop (e.g. when it is uninstalled). */
	fun unpinFromAllDesktops(app: App): Boolean {
		var modified = false
		for (page in this.pinnedByPage) {
			if (page.remove(app)) {
				modified = true
			}
		}
		this.pinnedChanged()

		return modified
	}

	/**
	 * Highest desktop index holding a pinned app, or -1 when none — so pins never
	 * inflate the desktop count in global mode (where they aren't desktop-bound).
	 */
	fun highestPinnedDesktop(): Int {
		if (! this.perDesktop) {
			return -1
		}

		for (i in this.pinnedByPage.indices.reversed()) {
			if (this.pinnedByPage[i].isNotEmpty()) {
				return i
			}
		}

		return -1
	}

	/**
	 * Removes a whole desktop's pins and shifts higher desktops down, for desktop
	 * deletion. A no-op in global mode (the shared pins are not tied to a desktop).
	 */
	fun removePinnedDesktop(desktop: Int) {
		if (! this.perDesktop) {
			return
		}

		if (desktop in this.pinnedByPage.indices) {
			this.pinnedByPage.removeAt(desktop)
			this.pinnedChanged()
		}
	}

	fun setCurrentDesktop(desktop: Int) {
		val idx = this.pageIndex(desktop)
		if (idx == this.currentDesktop) {
			return
		}

		this.currentDesktop = idx
		this.pinnedChanged() // The launcher's active desktop (and so its pins) changed //
	}

	//# Pinned apps — current-desktop convenience (used by the existing callers) #//

	fun isPinned(app: App): Boolean = this.isPinnedOn(app, this.currentDesktop)

	fun indexOfPinned(app: App): Int = this.indexOfPinnedOn(app, this.currentDesktop)

	/** @return whether the app was newly pinned on the current desktop. */
	fun pin(app: App): Boolean = this.pin(app, this.currentDesktop)

	fun unpin(app: App): Boolean = this.unpin(app, this.currentDesktop)

	fun movePinnedApp(oldIndex: Int, newIndex: Int) =
		this.movePinnedApp(this.currentDesktop, oldIndex, newIndex)

	/** Reads the pin mode and loads the persisted per-desktop pins. */
	fun loadPinnedApps() {
		this.perDesktop = LauncherPinMode.current(Preferences.getSharedPreferences(this.context)) ==
			LauncherPinMode.DESKTOP
		val stored = PinnedAppsStorage.read(
			Preferences.getSharedPreferences(this.context, Preferences.PINNED_APPS))
		val appMap = this.installedAppsMap()

		this.pinnedByPage.clear()
		if (this.perDesktop) {
			for (page in stored) {
				this.pinnedByPage.add(this.resolve(page, appMap))
			}
		} else {
			// Collapse any stored pages onto desktop 0, de-duplicated //
			this.pinnedByPage.add(this.resolve(stored.flatten(), appMap))
		}

		this.pinnedChanged()
	}

	private fun resolve(keys: List<String>, appMap: Map<String, App>): CopyOnWriteArrayList<App> {
		val list = CopyOnWriteArrayList<App>()
		for (key in keys) {
			val app = appMap[key] ?: continue
			if (! list.contains(app)) {
				list.add(app)
			}
		}

		return list
	}

	fun savePinnedApps() {
		val prefs = Preferences.getSharedPreferences(this.context, Preferences.PINNED_APPS)

		// profileScopedKey, not packageAndActivityName: identical for personal-
		// profile apps (so existing pins keep matching) but with the profile
		// serial appended for work-profile apps, so they persist distinctly and
		// don't collide with the same package in the personal profile.
		if (this.perDesktop) {
			PinnedAppsStorage.writePerDesktop(prefs,
				this.pinnedByPage.map { page -> page.map { it.profileScopedKey } })
		} else {
			PinnedAppsStorage.writeGlobal(prefs,
				this.pageListOrEmpty(0).map { it.profileScopedKey })
		}
	}

	fun getRunningApps(): List<App> {
		val running = ArrayList<App>()
		val am = this.context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

		val runningAppProcesses = am.runningAppProcesses ?: return running

		val importantImportances = setOf(
			ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND,
			ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE,
			ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE,
			ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND,
			ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE)

		for (appProcess in runningAppProcesses) {
			if (appProcess.importance in importantImportances) {
				running.addAll(this.findAppsByPackageName(appProcess.processName)
					.filter { !it.isInternalShortcut })
			}
		}

		return running
	}

	private fun installedChanged() {
		this._installed.value = ArrayList(this.apps)
	}

	private fun pinnedChanged() {
		this._pinned.value = ArrayList(this.pageListOrEmpty(this.currentDesktop))
	}
}

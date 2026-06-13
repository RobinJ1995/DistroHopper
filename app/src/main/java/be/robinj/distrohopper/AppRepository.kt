package be.robinj.distrohopper

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ResolveInfo
import android.os.UserHandle
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
	private val pinnedApps = CopyOnWriteArrayList<App>()

	private val _installed = MutableStateFlow<List<App>>(emptyList())
	val installed: StateFlow<List<App>> = this._installed.asStateFlow()

	private val _pinned = MutableStateFlow<List<App>>(emptyList())
	val pinned: StateFlow<List<App>> = this._pinned.asStateFlow()

	val installedLive: List<App> get() = this.apps
	val pinnedLive: List<App> get() = this.pinnedApps

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
		this.apps.sortWith(AppComparatorAlphabetical())
		this.installedChanged()
	}

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

		return Profiles.otherProfiles(this.context)
			.flatMap { launcherApps.getActivityList(null, it) }
	}

	/**
	 * The distinct profiles ("profiles") the installed apps belong to:
	 * always the personal profile (null) first, other profiles in the order
	 * their apps were loaded.
	 */
	fun profiles(): List<UserHandle?> {
		val users = LinkedHashSet<UserHandle?>()
		users.add(null)
		this.apps.mapTo(users) { it.user }

		return users.toList()
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

	fun isPinned(app: App): Boolean = this.pinnedApps.contains(app)

	fun indexOfPinned(app: App): Int = this.pinnedApps.indexOf(app)

	/** @return whether the app was newly pinned. */
	fun pin(app: App): Boolean {
		if (this.isPinned(app)) {
			return false
		}

		val added = this.pinnedApps.add(app)
		this.pinnedChanged()

		return added
	}

	fun unpin(app: App): Boolean {
		val modified = this.pinnedApps.remove(app)
		this.pinnedChanged()

		return modified
	}

	fun movePinnedApp(oldIndex: Int, newIndex: Int) {
		val app = this.pinnedApps.removeAt(oldIndex)
		this.pinnedApps.add(newIndex, app)
		this.pinnedChanged()
	}

	fun savePinnedApps() {
		val editor = Preferences
			.getSharedPreferences(this.context, Preferences.PINNED_APPS).edit()

		editor.clear()

		// The key equals "package\nactivity" for personal-profile apps (the
		// pre-work-profile format), with the profile serial appended otherwise //
		for ((i, app) in this.pinnedApps.withIndex()) {
			editor.putString(i.toString(), app.profileScopedKey)
		}

		editor.apply()
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
		this._pinned.value = ArrayList(this.pinnedApps)
	}
}

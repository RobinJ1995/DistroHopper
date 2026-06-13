package be.robinj.distrohopper.home

import android.content.Context
import android.graphics.drawable.Drawable
import android.content.ComponentName
import android.content.Intent
import be.robinj.distrohopper.App
import be.robinj.distrohopper.AppManager
import be.robinj.distrohopper.ExceptionHandler
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.RequestCode
import be.robinj.distrohopper.cache.ICache
import be.robinj.distrohopper.dev.Log
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.PreferencesActivity
import be.robinj.distrohopper.preferences.Preferences
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * The blocking halves of app loading, run on the IO dispatcher by
 * StartupLoader. Ported from the AsyncLoadApps/AsyncLoadAppLabels/
 * AsyncLoadAppIcons AsyncTasks; cancellation checks sit at the same
 * checkpoints the AsyncTasks had.
 */
object AppsLoader {
	private val IGNORE = arrayOf("be.robinj.ubuntu") // Legacy package //

	suspend fun loadApps(
		parent: HomeActivity,
		context: Context,
		appLabelCache: ICache<String>,
		appIconCache: ICache<Drawable>,
		onProgress: (step: Int, steps: Int) -> Unit,
	): AppManager {
		val appManager = AppManager(parent)
		val prefsPinned = Preferences.getSharedPreferences(context, Preferences.PINNED_APPS)

		// Load selected icon pack before any icons are requested
		try {
			val iconPack = Preferences.getSharedPreferences(context)
				.getString(Preference.ICON_PACK.getName(), "")!!
			if (iconPack.isNotEmpty()) {
				appManager.loadIconPack(iconPack)
			}
		} catch (ex: Exception) {
			ExceptionHandler(ex).logAndTrack()
		}

		val tStart = System.currentTimeMillis()

		val resInfs = appManager.queryInstalledApps()
		val size = resInfs.size

		currentCoroutineContext().ensureActive()

		for (resInf in resInfs) {
			// Skip the legacy package and our own package; our own public launcher
			// entry is replaced by the internal Settings shortcut added below.
			if (IGNORE.contains(resInf.activityInfo.packageName) ||
					resInf.activityInfo.packageName == context.packageName)
				continue

			// One broken package (corrupt icon, vanished mid-query, ...) must not
			// abort the load: the launcher used to come up with a partial list //
			try {
				appManager.add(App(context, appManager, resInf, appLabelCache, appIconCache),
					false, false)
			} catch (ex: Exception) {
				ExceptionHandler(ex).logAndTrack()
			}
		}

		// Apps in other profiles (e.g. the work profile), via LauncherApps. In
		// its own try as well: a broken profile must not abort the load either //
		try {
			for (launcherActivityInfo in appManager.repository.queryOtherProfileApps()) {
				try {
					appManager.add(App(context, appManager, launcherActivityInfo,
						appLabelCache, appIconCache), false, false)
				} catch (ex: Exception) {
					ExceptionHandler(ex).logAndTrack()
				}
			}
		} catch (ex: Exception) {
			ExceptionHandler(ex).logAndTrack()
		}

		// Add the Settings shortcut as an internal-only entry. It is never returned
		// by PackageManager, so it cannot appear in other launchers.
		// No NEW_TASK flag (it would only bring the home task itself to the front),
		// and launched for result so HomeActivity.onActivityResult() handles the
		// Customise UI flow exactly like the launcher bar's preferences button.
		val settingsIntent = Intent()
				.setComponent(ComponentName(context.packageName, PreferencesActivity::class.java.name))
		// Built with the activity, not the application context: launching for
		// result needs the Activity, and AppManager already retains it anyway.
		appManager.add(App.internalShortcut(
				parent, appManager,
				context.packageName, PreferencesActivity::class.java.name,
				context.getString(R.string.shortcut_label_distrohopper_settings),
				settingsIntent, true, RequestCode.ACTIVITY_PREFERENCES),
				false, false)

		val tDoneRetrievingInstalledApps = System.currentTimeMillis()
		Log.getInstance().v(this.javaClass.simpleName, "Retrieved " + size
			+ " apps from package manager in "
			+ (tDoneRetrievingInstalledApps - tStart) + "ms.")

		onProgress(1, 3)
		currentCoroutineContext().ensureActive()

		appManager.sort()

		val tDoneSortingInstalledApps = System.currentTimeMillis()
		Log.getInstance().v(this.javaClass.simpleName, "Sorted " + size + " apps in "
			+ (tDoneSortingInstalledApps - tDoneRetrievingInstalledApps) + "ms.")

		onProgress(2, 3)
		currentCoroutineContext().ensureActive()

		val nPinned = prefsPinned.all.size
		if (nPinned > 0) {
			val appMap = appManager.installedAppsMap

			var i = 0
			while (true) {
				val pinnedKey = prefsPinned.getString((i++).toString(), null)
					?: break
				val pinnedApp = appMap[pinnedKey] ?: continue

				appManager.pin(pinnedApp, false, false, false)
			}
		}

		val prefs = Preferences.getSharedPreferences(context)
		if (prefs.getBoolean(Preference.DEFAULT_PINS_PENDING.getName(), false)) {
			val defaults = DefaultPinnedApps.select(
				appManager.installedApps,
				appManager.pinned,
				context.packageManager,
			)
			for (app in defaults) {
				appManager.pin(app, false, false, false)
			}
			if (defaults.isNotEmpty()) {
				appManager.repository.savePinnedApps()
			}
			prefs.edit()
				.remove(Preference.DEFAULT_PINS_PENDING.getName())
				.apply()
		}

		Log.getInstance().v(this.javaClass.simpleName, "Loaded " + appManager.pinned.size + " pinned apps in "
			+ (System.currentTimeMillis() - tDoneSortingInstalledApps) + "ms.")

		onProgress(3, 3)

		return appManager
	}

	suspend fun loadLabels(appManager: AppManager, appLabelCache: ICache<String>): Int {
		var n = 0

		val tStart = System.currentTimeMillis()

		for (app in appManager.installedApps) {
			if (! app.isLabelLoaded) {
				n += if (app.setLabel(app.getLabel(false), appLabelCache)) 1 else 0
			} else if (! appLabelCache.containsKey(app.profileScopedKey)) {
				appLabelCache[app.profileScopedKey] = app.label
				n += 1
			}
		}

		val tDoneCachingAppLabels = System.currentTimeMillis()
		Log.getInstance().v(this.javaClass.simpleName, "" + n + " app labels cached in "
			+ (tDoneCachingAppLabels - tStart) + "ms.")

		currentCoroutineContext().ensureActive()

		if (n == 0) {
			return n
		}

		appManager.sort()

		Log.getInstance().v(this.javaClass.simpleName, "Sorted " + appManager.size() + " apps in "
			+ (System.currentTimeMillis() - tDoneCachingAppLabels) + "ms.")

		return n
	}

	suspend fun loadIcons(appManager: AppManager, appIconCache: ICache<Drawable>): Int {
		var n = 0

		val tStart = System.currentTimeMillis()

		val populateIconCache = mutableMapOf<String, Drawable>()

		for (app in appManager.installedApps) {
			if (! appIconCache.containsKey(app.profileScopedKey)) {
				populateIconCache[app.profileScopedKey] = app.icon.drawable
				n += 1
			}
		}

		appIconCache.putAll(populateIconCache)

		Log.getInstance().v(this.javaClass.simpleName, "" + n + " app icons cached in "
			+ (System.currentTimeMillis() - tStart) + "ms.")

		currentCoroutineContext().ensureActive()

		return n
	}
}

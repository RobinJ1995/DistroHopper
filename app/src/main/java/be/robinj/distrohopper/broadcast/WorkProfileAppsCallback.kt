package be.robinj.distrohopper.broadcast

import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import be.robinj.distrohopper.App
import be.robinj.distrohopper.AppManager
import be.robinj.distrohopper.ExceptionHandler
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.dev.Log

/**
 * LauncherApps counterpart of PackageManagerBroadcastReceiver: package
 * broadcasts are only delivered for the personal profile, so app
 * installs/removals in other profiles (the work profile) arrive through this
 * callback instead. Personal-profile events are ignored here — the broadcast
 * receiver owns those.
 */
class WorkProfileAppsCallback(private val parent: HomeActivity) : LauncherApps.Callback() {
	override fun onPackageAdded(packageName: String, user: UserHandle) {
		// Never add our own package as an app in another profile (see
		// AppRepository.queryOtherProfileApps): tapping a work-profile copy of
		// DistroHopper would drop the user into that profile's first-run wizard.
		if (packageName == this.parent.packageName) {
			return
		}

		this.handle(user) { appManager ->
			val launcherApps = this.parent.getSystemService(LauncherApps::class.java)

			for (launcherActivityInfo in launcherApps.getActivityList(packageName, user)) {
				appManager.add(launcherActivityInfo, true, true)
			}

			Log.getInstance().v(this.javaClass.simpleName,
				"Package added in other profile")
		}
	}

	override fun onPackageRemoved(packageName: String, user: UserHandle) {
		this.handle(user) { appManager ->
			appManager.findAppsByPackageName(packageName)
				.filter { user == it.user }
				.forEach { appManager.remove(it) }

			Log.getInstance().v(this.javaClass.simpleName,
				"Package removed in other profile")
		}
	}

	override fun onPackageChanged(packageName: String, user: UserHandle) {
		// Mirrors the broadcast receiver, which ignores in-place package changes //
	}

	override fun onPackagesAvailable(
		packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
		// Quiet-mode (work apps paused) toggles: the apps stay listed //
	}

	override fun onPackagesUnavailable(
		packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
	}

	private fun handle(user: UserHandle, action: (AppManager) -> Unit) {
		try {
			if (Process.myUserHandle() == user) {
				return
			}

			// No AppManager means loading hasn't finished; the load includes all profiles //
			val appManager = this.parent.appManager ?: return

			action(appManager)
		} catch (ex: Exception) {
			ExceptionHandler(ex).show(this.parent)
		}
	}
}

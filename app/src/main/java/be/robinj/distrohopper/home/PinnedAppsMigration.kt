package be.robinj.distrohopper.home

import android.content.Context
import be.robinj.distrohopper.PinnedAppsStorage
import be.robinj.distrohopper.preferences.LauncherPinMode
import be.robinj.distrohopper.preferences.Preferences

/**
 * Rewrites the pinned-apps storage when the launcher pin mode is switched, so the
 * file matches the new mode:
 *  - to [LauncherPinMode.DESKTOP]: keep the desktops as stored (the former global
 *    pins become desktop 0, the rest stay empty).
 *  - to [LauncherPinMode.GLOBAL]: flatten every desktop in order onto desktop 0,
 *    de-duplicated keeping the first occurrence.
 *
 * Returning from the settings screen already relaunches the home screen, which
 * then loads the rewritten pins in the new mode.
 */
object PinnedAppsMigration {
	@JvmStatic
	fun migrate(context: Context, toMode: LauncherPinMode) {
		val prefs = Preferences.getSharedPreferences(context, Preferences.PINNED_APPS)
		val pages = PinnedAppsStorage.read(prefs)

		when (toMode) {
			LauncherPinMode.DESKTOP -> PinnedAppsStorage.writePerDesktop(prefs, pages)
			LauncherPinMode.GLOBAL -> {
				val flattened = LinkedHashSet<String>()
				for (page in pages) {
					flattened.addAll(page)
				}
				PinnedAppsStorage.writeGlobal(prefs, flattened.toList())
			}
		}
	}
}

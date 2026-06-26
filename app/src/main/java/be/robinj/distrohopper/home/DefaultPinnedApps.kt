package be.robinj.distrohopper.home

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import be.robinj.distrohopper.App
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.PreferencesRepository

/** Selects useful launcher defaults from known package names for a queued pass. */
internal object DefaultPinnedApps {
	@JvmStatic
	fun queue(prefs: PreferencesRepository) {
		prefs.edit { putBoolean(Preference.DEFAULT_PINS_PENDING.getName(), true) }
	}

	private val browsers = listOf(
		"org.mozilla.firefox",
		"com.android.chrome",
		"com.brave.browser",
		"com.microsoft.emmx",
		"com.sec.android.app.sbrowser",
		"com.duckduckgo.mobile.android",
		"com.vivaldi.browser",
		"com.opera.browser",
		"com.kiwibrowser.browser",
		"com.yandex.browser",
		"org.chromium.chrome",
		"com.android.browser",
	)

	private val emailApps = listOf(
		"com.google.android.gm",
		"com.microsoft.office.outlook",
		"com.samsung.android.email.provider",
		"net.thunderbird.android",
		"com.fsck.k9",
		"ch.protonmail.android",
		"eu.faircode.email",
		"com.android.email",
	)

	private val knownCameraApps = listOf(
		"com.google.android.GoogleCamera",
		"com.android.camera2",
		"com.android.camera",
		"com.sec.android.app.camera",
		"com.samsung.android.camera",
		"com.motorola.camera3",
		"com.motorola.camera2",
		"com.oneplus.camera",
		"com.oplus.camera",
		"com.oppo.camera",
		"com.huawei.camera",
		"com.sonyericsson.android.camera",
		"com.lge.camera",
		"com.htc.camera",
		"com.hmdglobal.camera2",
		"com.zte.camera",
		"com.meizu.media.camera",
		"com.asus.camera",
		"com.nothing.camera",
		"org.lineageos.aperture",
		"org.lineageos.snap",
	)

	fun select(
		installed: List<App>,
		pinned: List<App>,
		packageManager: PackageManager,
	): List<App> = listOfNotNull(
		selectByPriority(installed, pinned, browsers),
		selectByPriority(installed, pinned, emailApps),
		selectKnownCameraApp(installed, pinned, packageManager),
	)

	private fun selectByPriority(
		installed: List<App>,
		pinned: List<App>,
		packages: List<String>,
	): App? {
		if (pinned.any { it.packageName in packages }) {
			return null
		}

		for (packageName in packages) {
			installed.firstOrNull { it.packageName == packageName }?.let { return it }
		}

		return null
	}

	private fun selectKnownCameraApp(
		installed: List<App>,
		pinned: List<App>,
		packageManager: PackageManager,
	): App? {
		if (pinned.any { it.packageName in knownCameraApps }) {
			return null
		}

		val candidates = knownCameraApps.mapNotNull { packageName ->
			installed.firstOrNull { it.packageName == packageName }
		}

		return candidates.firstOrNull { isSystemApp(it.packageName, packageManager) }
			?: candidates.firstOrNull()
	}

	private fun isSystemApp(packageName: String, packageManager: PackageManager): Boolean =
		try {
			val flags = packageManager.getApplicationInfo(packageName, 0).flags
			flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
		} catch (_: PackageManager.NameNotFoundException) {
			false
		}
}

package be.robinj.distrohopper.home

import android.content.ComponentCallbacks2
import be.robinj.distrohopper.App
import be.robinj.distrohopper.AppManager
import be.robinj.distrohopper.dev.Log

/**
 * Drops cached app-icon bitmaps on [ComponentCallbacks2.onTrimMemory]. Each
 * [App] holds one rendered icon (108dp ARGB_8888) for the life of the process;
 * nothing released them before.
 *
 * UI_HIDDEN spares the pinned apps; BACKGROUND and deeper drop those too. The
 * legacy TRIM_MEMORY_RUNNING_* levels are ignored because Android 14 stopped
 * delivering them.
 *
 * [App.releaseIcon] only drops the reference, so an icon a view still holds is
 * not reclaimed until that view lets go of it.
 */
class IconMemoryTrimmer(private val apps: AppManager) {
	/** @return how many icons were dropped. */
	fun onTrimMemory(level: Int): Int = when {
		level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> this.release(sparePinned = false)
		level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> this.release(sparePinned = true)
		else -> 0
	}

	private fun release(sparePinned: Boolean): Int {
		val spared: Set<App> = if (sparePinned) this.apps.allPinned else emptySet()

		var released = 0
		// Snapshot: the installed list is live and the loader may still be filling it.
		for (app in this.apps.installedApps.toList()) {
			if (app in spared) {
				continue
			}

			if (app.releaseIcon()) {
				released++
			}
		}

		if (released > 0) {
			LOG.v(
				"IconMemoryTrimmer",
				"Released $released app icon(s)" +
					if (sparePinned) ", sparing ${spared.size} pinned" else " (including pinned)",
			)
		}

		return released
	}

	private companion object {
		private val LOG: Log = Log.getInstance()
	}
}

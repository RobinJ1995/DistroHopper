package be.robinj.distrohopper

import android.content.SharedPreferences

/**
 * The on-disk format of the pinned-apps preference file
 * ([be.robinj.distrohopper.preferences.Preferences.PINNED_APPS]), shared by
 * [AppRepository] (load/save) and the launcher-pin-mode migration so the two
 * can never diverge.
 *
 * Per-desktop entries are keyed `"<page>/<index>"`; the legacy global format
 * keys by a bare `"<index>"` (which loads onto desktop 0). The stored value is
 * an [App.packageAndActivityName] (`"packageName\nactivityName"`).
 */
object PinnedAppsStorage {
	/**
	 * The persisted pins as a dense list of desktops (index 0..highest), each an
	 * ordered list of `"pkg\nactivity"` keys. Empty when nothing is stored.
	 */
	@JvmStatic
	fun read(prefs: SharedPreferences): List<List<String>> {
		val byPage = HashMap<Int, MutableList<Pair<Int, String>>>()
		var maxPage = -1

		for ((key, value) in prefs.all) {
			if (value !is String) {
				continue
			}

			val (page, index) = parseKey(key) ?: continue
			byPage.getOrPut(page) { mutableListOf() }.add(index to value)
			if (page > maxPage) {
				maxPage = page
			}
		}

		if (maxPage < 0) {
			return emptyList()
		}

		return (0..maxPage).map { page ->
			byPage[page]?.sortedBy { it.first }?.map { it.second } ?: emptyList()
		}
	}

	/** Writes desktop 0's pins in the flat legacy format (used in global mode). */
	@JvmStatic
	fun writeGlobal(prefs: SharedPreferences, desktop0: List<String>) {
		val editor = prefs.edit()
		editor.clear()
		desktop0.forEachIndexed { index, value -> editor.putString(index.toString(), value) }
		editor.apply()
	}

	/** Writes all desktops in the per-page format (used in per-desktop mode). */
	@JvmStatic
	fun writePerDesktop(prefs: SharedPreferences, pages: List<List<String>>) {
		val editor = prefs.edit()
		editor.clear()
		pages.forEachIndexed { page, list ->
			list.forEachIndexed { index, value -> editor.putString("$page/$index", value) }
		}
		editor.apply()
	}

	private fun parseKey(key: String): Pair<Int, Int>? {
		val slash = key.indexOf('/')

		return try {
			if (slash >= 0) {
				key.substring(0, slash).toInt() to key.substring(slash + 1).toInt()
			} else {
				0 to key.toInt()
			}
		} catch (ex: NumberFormatException) {
			null
		}
	}
}

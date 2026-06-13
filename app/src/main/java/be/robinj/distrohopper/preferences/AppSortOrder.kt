package be.robinj.distrohopper.preferences

import android.content.SharedPreferences

/**
 * How the apps in the dash grid are ordered.
 *
 * Stored as the string [Preference.APP_SORT_ORDER]. The default is
 * [ALPHABETICAL], so an unset (or unrecognised) preference resolves to it. The
 * two usage-based orders fall back to alphabetical as their secondary key, so
 * apps sharing a score (e.g. never launched) stay alphabetically ordered.
 */
enum class AppSortOrder(val value: String) {
	ALPHABETICAL("alphabetical"),
	MOST_RECENTLY_USED("recent"),
	MOST_USED("most_used");

	companion object {
		/** Maps a stored value to an order, defaulting to [ALPHABETICAL]. */
		@JvmStatic
		fun of(value: String?): AppSortOrder =
			entries.firstOrNull { it.value == value } ?: ALPHABETICAL

		/** The order in effect, reading with the alphabetical default for an unset value. */
		@JvmStatic
		fun current(prefs: SharedPreferences): AppSortOrder =
			of(prefs.getString(Preference.APP_SORT_ORDER.getName(),
				Preference.APP_SORT_ORDER.getDefault()))
	}
}

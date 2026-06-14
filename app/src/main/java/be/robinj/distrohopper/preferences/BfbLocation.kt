package be.robinj.distrohopper.preferences

/**
 * Where the launcher's menu button (the "BFB") sits within the launcher bar, on
 * themes that let the user move or hide it (Pantheon, COSMIC): [NONE] (hidden),
 * [START] (the leading end — the top of a vertical launcher, the left of a
 * horizontal one) or [END] (the trailing end).
 *
 * Stored as the string [Preference.LAUNCHER_BFB_LOCATION]; an unset value means
 * "follow the theme default" (resolved by `Theme.launcherBfbLocationResolved`),
 * and any unrecognised value resolves to [NONE] as a safe fallback. Each theme
 * only ever offers the sides its design supports, so no new positions are
 * exposed where there weren't any before.
 */
enum class BfbLocation(val value: String) {
	NONE("none"),
	START("start"),
	END("end");

	companion object {
		/** Maps a stored value to a side; an unrecognised value is [NONE]. */
		@JvmStatic
		fun of(value: String?): BfbLocation =
			entries.firstOrNull { it.value == value } ?: NONE
	}
}

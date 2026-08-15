package be.robinj.distrohopper.preferences

/**
 * How readily the floating launcher's pull-out gesture triggers (see
 * [be.robinj.distrohopper.desktop.launcher.service.LauncherService]).
 *
 * Two numbers make up the trade-off between reach and accidental triggering:
 * [hotZoneDp] is how far in from the screen edge a touch still counts as a
 * grab (a wider strip is easier to hit, but takes that strip away from the app
 * underneath), and [pullDp] is how far the finger must travel inwards before a
 * release settles the launcher open rather than snapping it back (a shorter
 * pull is quicker, but a stray swipe is more likely to reach it).
 *
 * Stored as the string [Preference.LAUNCHER_SERVICE_SENSITIVITY]; an
 * unrecognised or unset value resolves to [MEDIUM], which is also the default.
 */
enum class LauncherServiceSensitivity(
	val value: String,
	/** Width of the grabbable strip along the edge, in dp. */
	val hotZoneDp: Int,
	/** Inward travel that commits the pull, in dp. */
	val pullDp: Int,
) {
	LOW("low", 8, 96),
	MEDIUM("medium", 16, 56),
	HIGH("high", 28, 28);

	/** The grabbable strip's width in pixels at [density]. */
	fun hotZonePx(density: Float): Int = (this.hotZoneDp * density).toInt().coerceAtLeast(1)

	/** The committing pull distance in pixels at [density]. */
	fun pullPx(density: Float): Int = (this.pullDp * density).toInt().coerceAtLeast(1)

	companion object {
		/** Maps a stored value to a sensitivity; an unrecognised value is [MEDIUM]. */
		@JvmStatic
		fun of(value: String?): LauncherServiceSensitivity =
			entries.firstOrNull { it.value == value } ?: MEDIUM
	}
}

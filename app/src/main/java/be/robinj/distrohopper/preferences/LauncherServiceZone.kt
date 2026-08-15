package be.robinj.distrohopper.preferences

/**
 * Which stretch of the launcher's screen edge the floating launcher (see
 * [be.robinj.distrohopper.desktop.launcher.service.LauncherService]) can be
 * pulled out from.
 *
 * The zone is expressed as a fraction of the edge's length, so it applies
 * unchanged whichever edge the launcher is docked on and at any screen size:
 * [START] is the top of a vertical edge / the left of a horizontal one, [END]
 * the opposite end, and [CENTRE] the middle. Narrowing the zone is what keeps
 * the pull clear of the parts of the edge an app underneath uses (a navigation
 * drawer's handle, the system back gesture, a side toolbar).
 *
 * Stored as the string [Preference.LAUNCHER_SERVICE_ZONE]; an unrecognised or
 * unset value resolves to [FULL], which is also the default.
 */
enum class LauncherServiceZone(
	val value: String,
	/** Where the zone starts, as a fraction of the edge's length. */
	val start: Float,
	/** Where the zone ends, as a fraction of the edge's length. */
	val end: Float,
) {
	FULL("full", 0F, 1F),
	START("start", 0F, 0.4F),
	CENTRE("centre", 0.3F, 0.7F),
	END("end", 0.6F, 1F);

	/** Where the hot zone begins along an edge [edgeLengthPx] long, in pixels. */
	fun offsetPx(edgeLengthPx: Int): Int =
		(edgeLengthPx.coerceAtLeast(0) * this.start).toInt()

	/**
	 * How long the hot zone is along an edge [edgeLengthPx] long, in pixels.
	 *
	 * Measured as the gap between the zone's two ends rather than from its width,
	 * so the rounding of each end is the same one [offsetPx] reports and a zone
	 * that runs to the far end reaches it exactly. Never zero for a non-empty
	 * edge, so a zone can't become ungrabbable.
	 */
	fun lengthPx(edgeLengthPx: Int): Int {
		val length = edgeLengthPx.coerceAtLeast(0)
		val endPx = (length * this.end).toInt().coerceAtMost(length)

		return (endPx - this.offsetPx(length))
			.coerceIn(if (length > 0) 1 else 0, length)
	}

	companion object {
		/** Maps a stored value to a zone; an unrecognised value is [FULL]. */
		@JvmStatic
		fun of(value: String?): LauncherServiceZone =
			entries.firstOrNull { it.value == value } ?: FULL
	}
}

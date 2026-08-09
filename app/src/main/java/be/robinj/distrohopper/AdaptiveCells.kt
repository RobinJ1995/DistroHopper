package be.robinj.distrohopper

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The one adaptive cell-count formula shared by the three sizing surfaces —
 * the dash grid ([be.robinj.distrohopper.desktop.dash.DashGrid]), the pinned
 * launcher icons ([be.robinj.distrohopper.desktop.launcher.LauncherIconGrid])
 * and the desktop grid ([be.robinj.distrohopper.widgets.WidgetGrid]). Each
 * surface keeps its own target/min/max cell-dp constants and passes them in;
 * the shape is always the same: the target cell size picks the device's
 * default count, and the min/max cell sizes bound the offered range.
 */
object AdaptiveCells {
	/** Fewest cells on an [edgeDp]-wide edge (cell size capped at [maxCellDp]). */
	@JvmStatic
	fun minCount(edgeDp: Int, maxCellDp: Int): Int =
		max(2, ceil(edgeDp.toDouble() / maxCellDp).toInt())

	/** Most cells on an [edgeDp]-wide edge (cell size floored at [minCellDp]). */
	@JvmStatic
	fun maxCount(edgeDp: Int, minCellDp: Int, maxCellDp: Int): Int =
		max(minCount(edgeDp, maxCellDp), edgeDp / minCellDp)

	/** The device-adaptive default count for an [edgeDp]-wide edge. */
	@JvmStatic
	fun defaultCount(edgeDp: Int, targetCellDp: Int, minCellDp: Int, maxCellDp: Int): Int =
		(edgeDp.toDouble() / targetCellDp).roundToInt()
			.coerceIn(minCount(edgeDp, maxCellDp), maxCount(edgeDp, minCellDp, maxCellDp))
}

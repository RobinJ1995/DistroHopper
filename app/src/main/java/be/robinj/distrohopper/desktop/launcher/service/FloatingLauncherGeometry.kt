package be.robinj.distrohopper.desktop.launcher.service

import be.robinj.distrohopper.theme.Location
import kotlin.math.abs

/**
 * The pure maths behind the floating launcher's pull-out gesture: how far the
 * finger has travelled *inwards* from the docked edge, how much of the bar that
 * reveals, and whether letting go settles it open or snaps it back.
 *
 * Kept free of views and Android geometry so the gesture's feel can be unit
 * tested; [FloatingLauncherWindow] does nothing but feed it raw screen
 * coordinates and apply what comes back.
 */
object FloatingLauncherGeometry {
	/** Whether a launcher docked on [edge] runs along a vertical screen side. */
	@JvmStatic
	fun isVertical(edge: Location): Boolean = edge == Location.LEFT || edge == Location.RIGHT

	/** The edge the floating launcher uses for [edge]; anything without a side falls back to LEFT. */
	@JvmStatic
	fun edgeOrDefault(edge: Location): Location =
		if (edge == Location.NONE) Location.LEFT else edge

	/**
	 * How far the finger has been pulled *away* from [edge], in pixels: positive
	 * inwards (revealing), negative back towards the edge. Coordinates are raw
	 * (screen) ones, so a window resize mid-gesture cannot shift them.
	 */
	@JvmStatic
	fun pulled(edge: Location, downX: Float, downY: Float, x: Float, y: Float): Float =
		when (edge) {
			Location.RIGHT -> downX - x
			Location.TOP -> y - downY
			Location.BOTTOM -> downY - y
			else -> x - downX
		}

	/**
	 * How much of a [barLengthPx]-long bar a pull of [pulledPx] has revealed,
	 * as a 0..1 fraction. A bar of unknown length (not measured yet) reads as
	 * fully hidden rather than dividing by zero.
	 */
	@JvmStatic
	fun progress(pulledPx: Float, barLengthPx: Int): Float =
		if (barLengthPx <= 0) 0F else (pulledPx / barLengthPx).coerceIn(0F, 1F)

	/**
	 * Whether letting go after [pulledPx] settles the launcher open: the pull
	 * reached the sensitivity's committing distance ([pullPx]), or — on a bar
	 * shorter than that distance — was dragged most of the way out anyway.
	 */
	@JvmStatic
	fun settleOpen(pulledPx: Float, barLengthPx: Int, pullPx: Int): Boolean =
		pulledPx >= pullPx || progress(pulledPx, barLengthPx) >= 0.5F

	/**
	 * The bar's offset from its docked position at [progress]: fully off-screen
	 * (its own length, signed away from the edge) when hidden, zero when open.
	 */
	@JvmStatic
	fun translation(edge: Location, barLengthPx: Int, progress: Float): Float {
		val hidden = barLengthPx * (1F - progress.coerceIn(0F, 1F))

		return when (edge) {
			Location.RIGHT, Location.BOTTOM -> hidden
			else -> -hidden
		}
	}

	/** Animation length (ms) for settling from [progress] to [target] over [fullMs]. */
	@JvmStatic
	fun settleDurationMs(progress: Float, target: Float, fullMs: Long): Long =
		(fullMs * abs(target - progress)).toLong().coerceAtLeast(MIN_SETTLE_MS)

	/** Shortest settle animation, so a nearly-there flick still reads as a movement. */
	const val MIN_SETTLE_MS = 80L
}

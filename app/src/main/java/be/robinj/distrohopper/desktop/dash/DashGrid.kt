package be.robinj.distrohopper.desktop.dash

import android.content.Context
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pure maths for the dash app grid (the counterpart to [be.robinj.distrohopper.widgets.WidgetGrid]
 * for widgets). The user picks how many icon cells span the SHORT screen edge
 * (the [Preference.DASH_GRID_COLUMNS] preference); the column count for the
 * current orientation, the square cell size, and the visible row count all
 * derive from that single number.
 *
 * The count is computed from the SCREEN — never from an individual grid's
 * measured width — so every profile page and every lens results grid get the
 * exact same count, and it stays stable as the dash area changes (a planned
 * launcher auto-hide on open, themes that shrink the dash, rotation). Cells
 * stretch to fill whatever width is actually available, which keeps the grid
 * edge-to-edge.
 */
object DashGrid {
	/** Target cell size (dp) used to pick the adaptive default column count. */
	private const val TARGET_CELL_DP = 96
	/**
	 * Cells never get larger than this (sets the minimum column count). 150dp
	 * keeps a 3-wide grid available on phones up to ~450dp wide (the largest
	 * "fewest columns / biggest icons" option), rather than forcing 4 columns
	 * minimum on larger phones.
	 */
	private const val MAX_CELL_DP = 150
	/** Cells never get smaller than this (sets the maximum column count). */
	private const val MIN_CELL_DP = 64

	/** Fewest columns offered on a screen [swDp] dp wide (cells capped at [MAX_CELL_DP]). */
	@JvmStatic
	fun minColumns(swDp: Int): Int = max(2, ceil(swDp.toDouble() / MAX_CELL_DP).toInt())

	/** Most columns offered on a screen [swDp] dp wide (cells floored at [MIN_CELL_DP]). */
	@JvmStatic
	fun maxColumns(swDp: Int): Int = max(minColumns(swDp), swDp / MIN_CELL_DP)

	/** The adaptive default short-edge column count for a screen [swDp] dp wide. */
	@JvmStatic
	fun defaultColumns(swDp: Int): Int =
		(swDp.toDouble() / TARGET_CELL_DP).roundToInt().coerceIn(minColumns(swDp), maxColumns(swDp))

	/**
	 * The number of columns to show for the current orientation. [n] is the
	 * user's chosen cells across the short edge: portrait shows exactly [n],
	 * landscape proportionally more so the square cells stay the same size —
	 * capped at twice [n] so very wide screens don't shrink icons indefinitely
	 * (the long edge never shows more than 2× the short edge's icons).
	 */
	@JvmStatic
	fun dashColumns(shortEdgePx: Int, longEdgePx: Int, portrait: Boolean, n: Int): Int {
		val cols = max(1, n)
		if (portrait || shortEdgePx <= 0) {
			return cols
		}

		return (n.toDouble() * longEdgePx / shortEdgePx).roundToInt().coerceIn(cols, 2 * cols)
	}

	/** Square cell size for [n] cells across [shortEdgePx]; a pre-layout fallback. */
	@JvmStatic
	fun cellSizePx(shortEdgePx: Int, n: Int): Int = if (n <= 0) 0 else shortEdgePx / n

	/** How many whole rows of [cellSizePx] fit in [gridHeightPx] (for the customise hint). */
	@JvmStatic
	fun visibleRows(gridHeightPx: Int, cellSizePx: Int): Int =
		if (cellSizePx <= 0) 0 else gridHeightPx / cellSizePx

	// --- Context-based convenience -----------------------------------------

	/** The stored short-edge column count [n], clamped to the screen's range. */
	@JvmStatic
	fun columns(context: Context): Int {
		val sw = context.resources.configuration.smallestScreenWidthDp
		val n = Preferences.getSharedPreferences(context)
			.getInt(Preference.DASH_GRID_COLUMNS.getName(), defaultColumns(sw))

		return n.coerceIn(minColumns(sw), maxColumns(sw))
	}

	/** The adaptive default for the current screen (the slider's "reset" point). */
	@JvmStatic
	fun defaultColumns(context: Context): Int =
		defaultColumns(context.resources.configuration.smallestScreenWidthDp)

	/** The valid short-edge column counts for the current screen (the slider range). */
	@JvmStatic
	fun columnsRange(context: Context): IntRange {
		val sw = context.resources.configuration.smallestScreenWidthDp
		return minColumns(sw)..maxColumns(sw)
	}

	/** The column count to apply to every dash grid in the current orientation. */
	@JvmStatic
	fun dashColumns(context: Context): Int {
		// Orientation/aspect come from the Configuration (screenWidthDp/HeightDp),
		// not raw display metrics: the lens grids use the application context
		// while the apps grid uses the activity, and the Configuration is shared
		// across both, so every dash grid agrees on the count (also in
		// multi-window, where the display size would mislead).
		val config = context.resources.configuration
		val shortDp = min(config.screenWidthDp, config.screenHeightDp)
		val longDp = max(config.screenWidthDp, config.screenHeightDp)
		val portrait = config.screenHeightDp >= config.screenWidthDp

		return dashColumns(shortDp, longDp, portrait, columns(context))
	}

	/** Square cell-size fallback for the current screen + preference (pre-layout). */
	@JvmStatic
	fun cellSizePx(context: Context): Int {
		val dm = context.resources.displayMetrics
		return cellSizePx(min(dm.widthPixels, dm.heightPixels), columns(context))
	}
}

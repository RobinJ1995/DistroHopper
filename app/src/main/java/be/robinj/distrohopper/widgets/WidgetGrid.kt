package be.robinj.distrohopper.widgets

import android.content.Context
import android.view.Surface
import be.robinj.distrohopper.AdaptiveCells
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pure grid maths for widget placement. The widget area is divided into
 * [COLS] x [ROWS] cells; widgets occupy a rectangle of cells.
 *
 * [COLS] and [ROWS] are computed at startup from the device's screen
 * dimensions via [init], giving roughly square cells that fill the screen
 * proportionally. The values default to 8×8 so tests that never call
 * [init] continue to work with the historic grid size.
 *
 * Widget positions are always stored in **portrait canonical coordinates**
 * (col, row, colSpan, rowSpan on a COLS×ROWS grid). When the device is in
 * landscape, [portraitToDisplay] transforms those coordinates for layout and
 * drawing; [displayToPortrait] is the inverse (used by drag/resize commit).
 */
object WidgetGrid {
	/** Target cell size in dp; half of DashGrid's 96 dp icon cell (SPAN = 2). */
	private const val TARGET_CELL_DP = 48

	/** Max cell size — sets the minimum column count (fewest, biggest cells). */
	private const val MAX_CELL_DP = 72

	/** Min cell size — sets the maximum column count (most, smallest cells). */
	private const val MIN_CELL_DP = 36

	/** Portrait column count. Changed once at startup via [init]; defaults to 8. */
	@JvmField
	var COLS = 8

	/** Portrait row count. Changed once at startup via [init]; defaults to 8. */
	@JvmField
	var ROWS = 8

	/** Fewest columns offered on a screen [shortEdgeDp] dp wide (cells capped at [MAX_CELL_DP]). */
	@JvmStatic
	fun minColumns(shortEdgeDp: Int): Int = AdaptiveCells.minCount(shortEdgeDp, MAX_CELL_DP)

	/** Most columns offered on a screen [shortEdgeDp] dp wide (cells floored at [MIN_CELL_DP]). */
	@JvmStatic
	fun maxColumns(shortEdgeDp: Int): Int =
		AdaptiveCells.maxCount(shortEdgeDp, MIN_CELL_DP, MAX_CELL_DP)

	/** The adaptive default column count for a screen [shortEdgeDp] dp wide. */
	@JvmStatic
	fun defaultColumns(shortEdgeDp: Int): Int =
		AdaptiveCells.defaultCount(shortEdgeDp, TARGET_CELL_DP, MIN_CELL_DP, MAX_CELL_DP)

	/**
	 * Computes sensible (cols, rows) for a screen with the given dp dimensions.
	 * Cells are approximately [TARGET_CELL_DP] dp square, clamped to
	 * [[MIN_CELL_DP], [MAX_CELL_DP]] and proportional to the screen aspect ratio.
	 */
	@JvmStatic
	fun calculate(shortEdgeDp: Int, longEdgeDp: Int): Pair<Int, Int> =
		calculate(shortEdgeDp, longEdgeDp, defaultColumns(shortEdgeDp))

	/**
	 * The (cols, rows) grid for a user-chosen [cols] count. At the adaptive
	 * default the row count keeps the EXACT legacy formula (rows from
	 * [TARGET_CELL_DP]), so existing desktops keep the grid their stored
	 * coordinates were written against; a non-default choice derives the rows
	 * from the screen's aspect ratio so cells stay roughly square.
	 */
	@JvmStatic
	fun calculate(shortEdgeDp: Int, longEdgeDp: Int, cols: Int): Pair<Int, Int> {
		val chosen = cols.coerceIn(minColumns(shortEdgeDp), maxColumns(shortEdgeDp))
		val rows = if (chosen == defaultColumns(shortEdgeDp) || shortEdgeDp <= 0) {
			(longEdgeDp.toDouble() / TARGET_CELL_DP).roundToInt()
		} else {
			(longEdgeDp.toDouble() * chosen / shortEdgeDp).roundToInt()
		}
		return chosen to max(chosen, rows)
	}

	/**
	 * Corrects a dp length back to the device's STABLE density, so the grid is
	 * immune to the system "Display size" setting: that setting rescales dp
	 * (pixels stay fixed), and a grid derived from live dp would change its
	 * COLS×ROWS underneath the absolute col/row coordinates persisted in
	 * [DesktopLayoutStorage] — scattering items on load. Corrected back to the
	 * stable density, every input the grid depends on is fixed for the device.
	 *
	 * When [stableDensityDpi] is unset or implausibly far from the current
	 * density (the Display-size setting only ranges ~0.85×–1.5×), the value is
	 * treated as unreliable and the live [edgeDp] is used unchanged — this also
	 * keeps test environments (Robolectric reports a bogus stable density) on
	 * the geometry their qualifiers declare.
	 */
	@JvmStatic
	fun stableEdgeDp(edgeDp: Int, currentDensityDpi: Int, stableDensityDpi: Int): Int {
		if (stableDensityDpi <= 0 || currentDensityDpi <= 0) {
			return edgeDp
		}

		val factor = currentDensityDpi.toDouble() / stableDensityDpi
		if (factor < 0.5 || factor > 2.0) {
			return edgeDp
		}

		return (edgeDp * factor).roundToInt()
	}

	/** The stable-density (short, long) screen edges in dp; see [stableEdgeDp]. */
	@JvmStatic
	fun stableEdgesDp(context: Context): Pair<Int, Int> {
		val config = context.resources.configuration
		val currentDpi = context.resources.displayMetrics.densityDpi
		val stableDpi = android.util.DisplayMetrics.DENSITY_DEVICE_STABLE

		return stableEdgeDp(config.smallestScreenWidthDp, currentDpi, stableDpi) to
			stableEdgeDp(max(config.screenWidthDp, config.screenHeightDp), currentDpi, stableDpi)
	}

	// ---- Persisted presets and grid size ------------------------------------

	/** Number of grid-size presets offered; [DEFAULT_PRESET] is the middle one. */
	const val PRESET_COUNT = 5

	/** Index of the default (device-derived) preset — the middle of the ladder. */
	const val DEFAULT_PRESET = 2

	/** The on-disk form of one grid size: "8x17". */
	@JvmStatic
	fun formatSize(size: Pair<Int, Int>): String = "${size.first}x${size.second}"

	/** Parses [formatSize]'s form; null when malformed or non-positive. */
	@JvmStatic
	fun parseSize(raw: String?): Pair<Int, Int>? {
		val parts = raw?.split('x') ?: return null
		if (parts.size != 2) {
			return null
		}

		val cols = parts[0].toIntOrNull() ?: return null
		val rows = parts[1].toIntOrNull() ?: return null
		return if (cols > 0 && rows > 0) cols to rows else null
	}

	/**
	 * The five (cols, rows) preset options for a screen: the adaptive default in
	 * the middle, two steps larger-celled below it and two steps finer above,
	 * clamped to the device's column range (ends may collapse on small screens).
	 * Generated ONCE per install and persisted (see [presets]); this function
	 * never runs against a live screen again afterwards.
	 */
	@JvmStatic
	fun generatePresets(shortEdgeDp: Int, longEdgeDp: Int): List<Pair<Int, Int>> {
		val default = defaultColumns(shortEdgeDp)
		return (-DEFAULT_PRESET..DEFAULT_PRESET).map { offset ->
			calculate(shortEdgeDp, longEdgeDp, default + offset)
		}
	}

	/**
	 * The persisted preset ladder, snapshotted from the stable screen edges on
	 * first launch. Once written it is read back verbatim forever: neither
	 * density/Display-size/orientation changes nor future retuning of the
	 * calculation constants can move a user's options from under their layout.
	 */
	@JvmStatic
	fun presets(context: Context): List<Pair<Int, Int>> {
		val prefs = Preferences.getSharedPreferences(context)
		val stored = prefs.getString(Preference.DESKTOP_GRID_PRESETS.getName(), null)
			?.split(',')?.mapNotNull { parseSize(it) }
		if (stored != null && stored.size == PRESET_COUNT) {
			return stored
		}

		val (shortEdgeDp, longEdgeDp) = stableEdgesDp(context)
		val generated = generatePresets(shortEdgeDp, longEdgeDp)
		prefs.edit()
			.putString(Preference.DESKTOP_GRID_PRESETS.getName(),
				generated.joinToString(",") { formatSize(it) })
			.apply()

		return generated
	}

	/**
	 * The ACTUAL persisted grid size in use. Snapshotted to the middle preset on
	 * first launch and only ever changed by the customise slider, so the grid —
	 * and the absolute col/row coordinates [DesktopLayoutStorage] holds against
	 * it — is stable by construction.
	 */
	@JvmStatic
	fun size(context: Context): Pair<Int, Int> {
		val prefs = Preferences.getSharedPreferences(context)
		val stored = parseSize(prefs.getString(Preference.DESKTOP_GRID_SIZE.getName(), null))
		if (stored != null) {
			return stored
		}

		val middle = presets(context)[DEFAULT_PRESET]
		prefs.edit()
			.putString(Preference.DESKTOP_GRID_SIZE.getName(), formatSize(middle))
			.apply()

		return middle
	}

	/** Persists [size] as the grid to use; applied by relaunching home. */
	@JvmStatic
	fun setSize(context: Context, size: Pair<Int, Int>) {
		Preferences.getSharedPreferences(context).edit()
			.putString(Preference.DESKTOP_GRID_SIZE.getName(), formatSize(size))
			.commit()
	}

	/** The preset index matching the current [size]; the middle when it matches none. */
	@JvmStatic
	fun selectedPreset(context: Context): Int {
		val index = presets(context).indexOf(size(context))
		return if (index >= 0) index else DEFAULT_PRESET
	}

	/**
	 * Initialises [COLS] and [ROWS] from the persisted grid size (snapshotting
	 * the presets and the default size on first launch). Call once from
	 * [be.robinj.distrohopper.HomeActivity.onCreate] before the first layout
	 * pass; a preset change re-applies by relaunching home (the stored desktop
	 * layout is reloaded against the new grid).
	 */
	@JvmStatic
	fun init(context: Context) {
		val (cols, rows) = size(context)
		COLS = cols
		ROWS = rows
	}

	// ---- Orientation helpers ------------------------------------------------

	/** Whether [rotation] represents a landscape orientation. */
	@JvmStatic
	fun isLandscape(rotation: Int): Boolean =
		rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270

	/**
	 * Number of grid columns visible in display space for the given [rotation].
	 * Portrait: [COLS]; landscape: [ROWS] (the grid is transposed).
	 */
	@JvmStatic
	fun displayCols(rotation: Int): Int = if (isLandscape(rotation)) ROWS else COLS

	/**
	 * Number of grid rows visible in display space for the given [rotation].
	 * Portrait: [ROWS]; landscape: [COLS].
	 */
	@JvmStatic
	fun displayRows(rotation: Int): Int = if (isLandscape(rotation)) COLS else ROWS

	/**
	 * Transforms portrait canonical coordinates to display coordinates for
	 * layout and drawing. Returns a [WidgetLayout] with appWidgetId = 0.
	 *
	 * ROTATION_0/180 (portrait): identity (or vertical flip for 180 — treated
	 * as identity since the launcher doesn't reflow for upside-down portrait).
	 *
	 * ROTATION_90 (CCW / home-button right): the physical top of the phone
	 * becomes the LEFT edge of the landscape display.
	 *
	 * ROTATION_270 (CW / home-button left): the physical top becomes the RIGHT
	 * edge of the landscape display.
	 */
	@JvmStatic
	fun portraitToDisplay(col: Int, row: Int, colSpan: Int, rowSpan: Int, rotation: Int): WidgetLayout =
		when (rotation) {
			Surface.ROTATION_90 -> WidgetLayout(
				0,
				row,
				COLS - col - colSpan,
				rowSpan,
				colSpan,
			)
			Surface.ROTATION_270 -> WidgetLayout(
				0,
				ROWS - row - rowSpan,
				col,
				rowSpan,
				colSpan,
			)
			else -> WidgetLayout(0, col, row, colSpan, rowSpan)
		}

	/**
	 * Inverse of [portraitToDisplay]: converts display-space coordinates back
	 * to portrait canonical coordinates for persisting after a drag or resize.
	 */
	@JvmStatic
	fun displayToPortrait(
		displayCol: Int,
		displayRow: Int,
		displayColSpan: Int,
		displayRowSpan: Int,
		rotation: Int,
	): WidgetLayout =
		when (rotation) {
			// CCW inverse: portCol = COLS - displayRow - displayRowSpan
			Surface.ROTATION_90 -> WidgetLayout(
				0,
				COLS - displayRow - displayRowSpan,
				displayCol,
				displayRowSpan,
				displayColSpan,
			)
			// CW inverse: portCol = displayRow
			Surface.ROTATION_270 -> WidgetLayout(
				0,
				displayRow,
				ROWS - displayCol - displayColSpan,
				displayRowSpan,
				displayColSpan,
			)
			else -> WidgetLayout(0, displayCol, displayRow, displayColSpan, displayRowSpan)
		}

	// ---- Snap / span maths (unchanged) ------------------------------------

	/**
	 * Snap a pixel offset to the nearest cell boundary.
	 */
	@JvmStatic
	fun snapToCell(px: Int, cellSizePx: Int, maxCell: Int): Int {
		if (cellSizePx <= 0) {
			return 0
		}

		return (px.toFloat() / cellSizePx.toFloat()).roundToInt().coerceIn(0, maxCell)
	}

	/**
	 * The initial span for a freshly placed or restored widget, in cells, or
	 * **0 when the cell size is not known yet** (the grid hasn't been measured)
	 * so the caller can defer rather than collapse the widget to 1x1.
	 *
	 * [targetCells] is the provider's preferred cell count (API 33
	 * targetCellWidth/Height, 0 if unset); [minPx]/[maxPx] are its resize
	 * limits in px (prefer minResizeWidth/Height over minWidth/Height). The
	 * result is clamped into the provider's range via [clampSpan].
	 */
	@JvmStatic
	fun initialSpan(targetCells: Int, minPx: Int, maxPx: Int, cellSizePx: Int, gridMax: Int): Int {
		if (cellSizePx <= 0) {
			return 0
		}

		val base = if (targetCells > 0) {
			targetCells
		} else if (minPx > 0) {
			(minPx.toDouble() / cellSizePx).roundToInt()
		} else {
			1
		}

		return clampSpan(base.coerceIn(1, gridMax), minPx, maxPx, cellSizePx, gridMax)
	}

	/**
	 * Clamp a span (in cells) to the provider's pixel size limits.
	 * A [minPx] or [maxPx] of 0 or less means "unspecified".
	 *
	 * Both bounds use nearest rounding (not ceil-min / floor-max): the coarse
	 * grid rarely lines up with a provider's exact dp limits, and the old
	 * asymmetric rounding collapsed the range to a single span — making
	 * widgets unresizable even when their resizeMode allowed it. Nearest
	 * rounding keeps a genuine [min, max] range, accepting a sub-cell overshoot
	 * at the boundary.
	 */
	@JvmStatic
	fun clampSpan(span: Int, minPx: Int, maxPx: Int, cellSizePx: Int, gridMax: Int): Int {
		if (cellSizePx <= 0) {
			return span.coerceIn(1, gridMax)
		}

		val minSpan = if (minPx > 0) {
			(minPx.toDouble() / cellSizePx).roundToInt().coerceIn(1, gridMax)
		} else {
			1
		}
		val maxSpan = if (maxPx > 0) {
			(maxPx.toDouble() / cellSizePx).roundToInt().coerceIn(minSpan, gridMax)
		} else {
			gridMax
		}

		return span.coerceIn(minSpan, maxSpan)
	}

	@JvmStatic
	fun overlaps(a: WidgetLayout, b: WidgetLayout): Boolean =
		a.col < b.col + b.colSpan &&
			b.col < a.col + a.colSpan &&
			a.row < b.row + b.rowSpan &&
			b.row < a.row + a.rowSpan

	/**
	 * Whether the candidate is within the portrait grid bounds and free of
	 * overlap with the others.
	 */
	@JvmStatic
	fun fits(others: List<WidgetLayout>, candidate: WidgetLayout): Boolean {
		if (candidate.col < 0 || candidate.row < 0 ||
			candidate.colSpan < 1 || candidate.rowSpan < 1 ||
			candidate.col + candidate.colSpan > COLS ||
			candidate.row + candidate.rowSpan > ROWS
		) {
			return false
		}

		return others.none { overlaps(it, candidate) }
	}

	/**
	 * First-fit (row-major) free rectangle of the requested size, or null if the grid is full.
	 */
	@JvmStatic
	fun findFreeRect(occupied: List<WidgetLayout>, colSpan: Int, rowSpan: Int): WidgetLayout? {
		val cols = colSpan.coerceIn(1, COLS)
		val rows = rowSpan.coerceIn(1, ROWS)

		for (row in 0..ROWS - rows) {
			for (col in 0..COLS - cols) {
				val candidate = WidgetLayout(0, col, row, cols, rows)

				if (fits(occupied, candidate)) {
					return candidate
				}
			}
		}

		return null
	}
}

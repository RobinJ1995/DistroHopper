package be.robinj.distrohopper.widgets

import kotlin.math.roundToInt

/**
 * Pure grid maths for widget placement. The widget area is divided into
 * [COLS] x [ROWS] cells; widgets occupy a rectangle of cells.
 */
object WidgetGrid {
	const val COLS = 8
	const val ROWS = 8

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
	 * 8-cell grid rarely lines up with a provider's exact dp limits, and the
	 * old asymmetric rounding collapsed the range to a single span — making
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
	 * Whether the candidate is within the grid bounds and free of overlap with the others.
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

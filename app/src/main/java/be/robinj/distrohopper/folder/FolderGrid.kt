package be.robinj.distrohopper.folder

import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Pure maths for a folder's contents grid — the folder counterpart to
 * [be.robinj.distrohopper.widgets.WidgetGrid] (which is the 8x8 desktop grid).
 *
 * A folder is at most [SIZE]x[SIZE] cells. Apps-only folders (dash, launcher)
 * lay their icons out with the adaptive [columns] mapping (the spec's 1..9
 * layout). Desktop folders place apps (1x1) and widgets (their span) on the same
 * grid via [fits]/[findFreeRect], so an app + a widget must together fit within
 * 3x3 (e.g. four apps leave room only for a 2x2-or-smaller widget).
 */
object FolderGrid {
	const val SIZE = 3
	const val MAX_CELLS = SIZE * SIZE // 9

	/** A cell rectangle inside the folder grid. Apps are 1x1; widgets carry their span. */
	data class Rect(val col: Int, val row: Int, val colSpan: Int = 1, val rowSpan: Int = 1)

	/**
	 * Columns for an apps-only folder holding [n] apps, per the spec: 1..3 apps
	 * sit in a single row; 4 apps form a 2x2; 5..9 apps use three columns.
	 */
	@JvmStatic
	fun columns(n: Int): Int = when {
		n <= 0 -> 1
		n <= 3 -> n
		n == 4 -> 2
		else -> SIZE
	}

	/** Rows for [n] apps laid out with [columns] columns. */
	@JvmStatic
	fun rows(n: Int): Int {
		val c = this.columns(n)
		return if (c <= 0) 0 else ceil(n.toDouble() / c).roundToInt()
	}

	@JvmStatic
	fun overlaps(a: Rect, b: Rect): Boolean =
		a.col < b.col + b.colSpan &&
			b.col < a.col + a.colSpan &&
			a.row < b.row + b.rowSpan &&
			b.row < a.row + a.rowSpan

	/** Whether [candidate] is within 3x3 bounds and overlaps none of [others]. */
	@JvmStatic
	fun fits(others: List<Rect>, candidate: Rect): Boolean {
		if (candidate.col < 0 || candidate.row < 0 ||
			candidate.colSpan < 1 || candidate.rowSpan < 1 ||
			candidate.col + candidate.colSpan > SIZE ||
			candidate.row + candidate.rowSpan > SIZE
		) {
			return false
		}

		return others.none { this.overlaps(it, candidate) }
	}

	/** First-fit (row-major) free rectangle of the requested size, or null if there is no room. */
	@JvmStatic
	fun findFreeRect(occupied: List<Rect>, colSpan: Int, rowSpan: Int): Rect? {
		val cols = colSpan.coerceIn(1, SIZE)
		val rows = rowSpan.coerceIn(1, SIZE)

		for (row in 0..SIZE - rows) {
			for (col in 0..SIZE - cols) {
				val candidate = Rect(col, row, cols, rows)
				if (this.fits(occupied, candidate)) {
					return candidate
				}
			}
		}

		return null
	}
}

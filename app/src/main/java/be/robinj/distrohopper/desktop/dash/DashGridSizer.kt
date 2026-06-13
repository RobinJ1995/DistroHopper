package be.robinj.distrohopper.desktop.dash

import android.widget.BaseAdapter
import android.widget.GridView

/**
 * Applies [DashGrid]'s unified column count to a dash GridView. Used for every
 * profile page grid and every lens results grid, so they can never disagree on
 * the number of columns. The GridView's stretch mode then divides its own
 * current width across that fixed count, filling the row edge-to-edge.
 */
object DashGridSizer {
	@JvmStatic
	fun apply(grid: GridView) {
		grid.numColumns = DashGrid.dashColumns(grid.context)
		(grid.adapter as? BaseAdapter)?.notifyDataSetChanged()
	}
}

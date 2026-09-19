package be.robinj.distrohopper.icons

import android.content.Context
import be.robinj.distrohopper.R
import be.robinj.distrohopper.desktop.dash.DashGrid
import be.robinj.distrohopper.desktop.dash.FolderPopup
import be.robinj.distrohopper.desktop.launcher.LauncherIconGrid
import be.robinj.distrohopper.widgets.DesktopAppLayout
import be.robinj.distrohopper.widgets.DesktopFolderOverlay
import be.robinj.distrohopper.widgets.WidgetGrid
import kotlin.math.max

/**
 * The pixel size app icons are rasterised at: the largest an icon is actually
 * drawn on any surface under the current settings, never more than the 108dp
 * adaptive-icon canvas (beyond which a render adds no detail).
 *
 * A rendered icon is held for every installed app, so its size is the launcher's
 * memory footprint; rendering at the canvas size regardless of what is drawn
 * cost several times the pixels the screen ever showed.
 *
 * Every term is an upper bound on what its surface draws (a cell's padding and
 * a single line of label text are subtracted, nothing more), so an icon is
 * never rendered smaller than it is shown, only very slightly larger. The terms
 * deliberately depend on nothing but the stable screen edge, the density and the
 * grid preferences: a theme's launcher margins are ignored, for instance, so a
 * theme switch alone leaves the size — and with it the icon cache, which
 * [IconConfig.signature] keys on this size — untouched.
 */
object IconRenderSize {
	/** Adaptive icons are authored on a 108dp canvas; rendering larger adds nothing. */
	const val CANVAS_DP = 108

	/** Floor, guarding against a degenerate configuration; the launcher's own icon minimum. */
	const val MIN_DP = LauncherIconGrid.MIN_ICON_DP

	/** The largest of [surfacesPx], clamped to `minPx..maxPx`. */
	@JvmStatic
	fun px(surfacesPx: Collection<Int>, minPx: Int, maxPx: Int): Int =
		(surfacesPx.maxOrNull() ?: 0).coerceIn(minPx, max(minPx, maxPx))

	/**
	 * The icon drawn in a square icon-over-label cell of [cellPx]: the cell minus
	 * its padding and one line of label. [labelPx] is the label's text size, which
	 * is less than a real line height, so this errs on the large side.
	 */
	@JvmStatic
	fun labelledCellIconPx(cellPx: Int, paddingPx: Int, labelPx: Int): Int =
		(cellPx - 2 * paddingPx - labelPx).coerceAtLeast(0)

	/** The icon drawn in a launcher-bar slot of [slotPx], inset by [marginPx] on each side. */
	@JvmStatic
	fun launcherIconPx(slotPx: Int, marginPx: Int): Int =
		(slotPx - 2 * marginPx).coerceAtLeast(0)

	/** The render size for the current screen and preferences. */
	@JvmStatic
	fun px(context: Context): Int {
		val density = context.resources.displayMetrics.density

		return px(surfacesPx(context), (MIN_DP * density).toInt(), (CANVAS_DP * density).toInt())
	}

	/**
	 * The largest icon each surface draws, in px: the dash grid, the launcher
	 * bar, a desktop app, and the two folder pop-overs. The dash and launcher
	 * terms follow their grid preferences, the desktop term the persisted widget
	 * grid.
	 */
	@JvmStatic
	fun surfacesPx(context: Context): List<Int> {
		val res = context.resources
		val density = res.displayMetrics.density
		// The same stable anchor the dash and launcher grids size themselves from,
		// rather than raw display metrics, which drift in multi-window //
		val shortEdgePx = (res.configuration.smallestScreenWidthDp * density).toInt()

		val cellPadding = res.getDimensionPixelSize(R.dimen.dash_applauncher_padding)
		val labelPx = res.getDimensionPixelSize(R.dimen.dash_applauncher_textsize)
		val launcherMargin = res.getDimensionPixelSize(R.dimen.launcher_applauncher_icon_margin)

		val dashCell = DashGrid.cellSizePx(shortEdgePx, DashGrid.columns(context))
		// The bare screen edge, not the theme's launcher interior: an upper bound
		// that keeps the size independent of the theme (see the class doc) //
		val launcherSlot = LauncherIconGrid.iconSizePx(shortEdgePx, LauncherIconGrid.count(context))
		// The persisted grid rather than WidgetGrid.COLS, so the size does not
		// depend on HomeActivity having initialised the grid in this process //
		val desktopCell = shortEdgePx / WidgetGrid.size(context).first * DesktopAppLayout.SPAN

		return listOf(
			labelledCellIconPx(dashCell, cellPadding, labelPx),
			launcherIconPx(launcherSlot, launcherMargin),
			labelledCellIconPx(desktopCell, cellPadding, labelPx),
			labelledCellIconPx((DesktopFolderOverlay.CELL_DP * density).toInt(), cellPadding, labelPx),
			labelledCellIconPx((FolderPopup.CELL_DP * density).toInt(), cellPadding, labelPx),
		)
	}
}

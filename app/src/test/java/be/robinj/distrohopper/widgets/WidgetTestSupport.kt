package be.robinj.distrohopper.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.view.View
import be.robinj.distrohopper.App
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R

internal object WidgetTestSupport {
	/**
	 * Width of the laid-out test grid in px. [be.robinj.distrohopper.HomeActivity]'s
	 * onCreate runs [WidgetGrid.init], which derives [WidgetGrid.COLS]/[WidgetGrid.ROWS]
	 * from the Robolectric screen (pinned to a modern 360×800dp phone in
	 * robolectric.properties → an 8×17 grid), so the geometry below is whatever a real
	 * device produces rather than a hand-picked grid.
	 */
	const val GRID_SIZE = 800

	/** Width of one cell: the grid width split across the current column count. */
	val CELL get() = GRID_SIZE / WidgetGrid.COLS

	/**
	 * Height of one cell. [WidgetGrid] targets roughly square cells, so the test grid
	 * uses square cells: this equals [CELL]. The grid is [GRID_SIZE] wide and
	 * [gridHeight] tall — taller than it is wide, like a phone's widget area.
	 */
	val CELL_H get() = CELL

	/** Height of the laid-out test grid: square cells across every current row. */
	val gridHeight get() = WidgetGrid.ROWS * CELL_H

	/**
	 * A standalone widget grid (the first page of a fresh pager), detached
	 * from the activity's view tree so the framework cannot re-layout it to
	 * the (small) test window size behind the test's back.
	 */
	fun standaloneGrid(activity: HomeActivity): WidgetsContainer =
		WidgetsPager(activity, null).pageAt(0)

	fun pagerOf(grid: WidgetsContainer): WidgetsPager = grid.parent as WidgetsPager

	fun host(
		activity: HomeActivity,
		grid: WidgetsContainer =
			activity.findViewById<WidgetsPager>(R.id.vgWidgets).pageAt(0),
	): WidgetHost =
		WidgetHost(activity, AppWidgetManager.getInstance(activity), pagerOf(grid))

	fun providerInfo(
		resizeMode: Int = AppWidgetProviderInfo.RESIZE_BOTH,
		minResizeWidth: Int = 0,
		minResizeHeight: Int = 0,
		maxResizeWidth: Int = 0,
		maxResizeHeight: Int = 0,
	): AppWidgetProviderInfo = AppWidgetProviderInfo().apply {
		this.resizeMode = resizeMode
		this.minResizeWidth = minResizeWidth
		this.minResizeHeight = minResizeHeight
		this.maxResizeWidth = maxResizeWidth
		this.maxResizeHeight = maxResizeHeight
	}

	/**
	 * Adds a widget container directly to the given grid, bypassing the
	 * system binding flow, and returns it.
	 */
	fun addWidget(
		activity: HomeActivity,
		host: WidgetHost,
		grid: WidgetsContainer,
		appWidgetId: Int,
		col: Int, row: Int, colSpan: Int, rowSpan: Int,
		info: AppWidgetProviderInfo? = providerInfo(),
	): WidgetContainer {
		val hostView = WidgetHostView(activity.applicationContext, host)
		if (info != null) {
			hostView.setAppWidget(appWidgetId, info)
		}
		val container = WidgetContainer(activity, host, hostView)
		grid.addView(container, WidgetsContainer.LayoutParams(col, row, colSpan, rowSpan))
		return container
	}

	/**
	 * A [DesktopAppHost] bound to the given grid's pager, with the pager's
	 * occupied-desktop supplier widened to count desktop apps too (production
	 * wires this through `home/Desktops`; a standalone test pager otherwise only
	 * counts widgets and would trim the page a desktop app was just placed on).
	 */
	fun desktopHost(
		activity: HomeActivity,
		grid: WidgetsContainer =
			activity.findViewById<WidgetsPager>(R.id.vgWidgets).pageAt(0),
	): DesktopAppHost {
		val pager = pagerOf(grid)
		val host = DesktopAppHost(activity, pager, activity.appManager.repository)
		val widgets = pager.occupiedDesktopSupplier
		pager.occupiedDesktopSupplier = { maxOf(widgets(), host.highestDesktop()) }

		return host
	}

	/** The (first) installed app for a seeded test package. */
	fun app(activity: HomeActivity, packageName: String): App =
		activity.appManager.findAppsByPackageName(packageName).first()

	/** The desktop-app views on a grid page, in child order. */
	fun desktopAppsOn(grid: WidgetsContainer): List<DesktopAppView> =
		(0 until grid.childCount).mapNotNull { grid.getChildAt(it) as? DesktopAppView }

	/** The desktop-folder views on a grid page, in child order. */
	fun foldersOn(grid: WidgetsContainer): List<DesktopFolderView> =
		(0 until grid.childCount).mapNotNull { grid.getChildAt(it) as? DesktopFolderView }

	/** A [DesktopFolderHost] bound to the grid's pager (widening the occupied supplier for folders). */
	fun desktopFolderHost(
		activity: HomeActivity,
		grid: WidgetsContainer,
		appHost: DesktopAppHost,
		widgetHost: WidgetHost,
	): DesktopFolderHost {
		val pager = pagerOf(grid)
		val host = DesktopFolderHost(activity, pager, activity.appManager.repository, widgetHost, appHost)
		val prev = pager.occupiedDesktopSupplier
		pager.occupiedDesktopSupplier = { maxOf(prev(), host.highestDesktop()) }

		return host
	}

	/** Measures and lays out the grid (square cells) so cell sizes are non-zero. */
	fun layoutGrid(grid: WidgetsContainer) {
		grid.setPadding(0, 0, 0, 0)
		grid.measure(
			View.MeasureSpec.makeMeasureSpec(GRID_SIZE, View.MeasureSpec.EXACTLY),
			View.MeasureSpec.makeMeasureSpec(gridHeight, View.MeasureSpec.EXACTLY))
		grid.layout(0, 0, GRID_SIZE, gridHeight)
	}
}

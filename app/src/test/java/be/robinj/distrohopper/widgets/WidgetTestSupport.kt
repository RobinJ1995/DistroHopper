package be.robinj.distrohopper.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.view.View
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R

internal object WidgetTestSupport {
	/** Grid size used by [layoutGrid]; with 8x8 cells this gives 100px cells. */
	const val GRID_SIZE = 800
	const val CELL = GRID_SIZE / WidgetGrid.COLS

	/**
	 * A standalone widget grid, detached from the activity's view tree so the
	 * framework cannot re-layout it to the (small) test window size behind the
	 * test's back.
	 */
	fun standaloneGrid(activity: HomeActivity): WidgetsContainer =
		WidgetsContainer(activity, null)

	fun host(
		activity: HomeActivity,
		grid: WidgetsContainer = activity.findViewById(R.id.vgWidgets),
	): WidgetHost =
		WidgetHost(activity, AppWidgetManager.getInstance(activity), grid)

	fun providerInfo(
		resizeMode: Int = AppWidgetProviderInfo.RESIZE_BOTH,
		minResizeWidth: Int = 0,
		minResizeHeight: Int = 0,
	): AppWidgetProviderInfo = AppWidgetProviderInfo().apply {
		this.resizeMode = resizeMode
		this.minResizeWidth = minResizeWidth
		this.minResizeHeight = minResizeHeight
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

	/** Measures and lays out the grid so cell sizes are non-zero. */
	fun layoutGrid(grid: WidgetsContainer) {
		grid.setPadding(0, 0, 0, 0)
		grid.measure(
			View.MeasureSpec.makeMeasureSpec(GRID_SIZE, View.MeasureSpec.EXACTLY),
			View.MeasureSpec.makeMeasureSpec(GRID_SIZE, View.MeasureSpec.EXACTLY))
		grid.layout(0, 0, GRID_SIZE, GRID_SIZE)
	}
}

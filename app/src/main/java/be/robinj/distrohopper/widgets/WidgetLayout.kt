package be.robinj.distrohopper.widgets

/**
 * Position and size of a widget on the home screen grid, plus which desktop
 * (widget page) it lives on.
 *
 * Persisted (alongside the desktop apps and folders) by [DesktopLayoutStorage].
 */
data class WidgetLayout @JvmOverloads constructor(
	@JvmField var appWidgetId: Int,
	@JvmField var col: Int,
	@JvmField var row: Int,
	@JvmField var colSpan: Int,
	@JvmField var rowSpan: Int,
	@JvmField var page: Int = 0,
)

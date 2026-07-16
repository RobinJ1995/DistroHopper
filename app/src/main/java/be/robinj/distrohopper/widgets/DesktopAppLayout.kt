package be.robinj.distrohopper.widgets

/**
 * Position of an app pinned to the home screen grid, plus which desktop (page)
 * it lives on. A desktop app always occupies a fixed [SPAN]x[SPAN] block of
 * cells (a single cell on the fine 8x8 widget grid would be far too small for a
 * tappable icon and its label), so its size is implicit; it is identified by
 * its app's [App.profileScopedKey] (so the same package in a different profile
 * stays distinct).
 *
 * @see WidgetLayout for the widget equivalent.
 */
data class DesktopAppLayout @JvmOverloads constructor(
	@JvmField var key: String,
	@JvmField var col: Int,
	@JvmField var row: Int,
	@JvmField var page: Int = 0,
) {
	/**
	 * The [SPAN]x[SPAN] grid rectangle this app occupies, as a [WidgetLayout]
	 * (with no widget id), so it can be fed to [WidgetGrid]'s collision maths
	 * alongside the real widgets.
	 */
	fun toGridRect(): WidgetLayout = WidgetLayout(NO_WIDGET_ID, this.col, this.row, SPAN, SPAN, this.page)

	companion object {
		/** The [WidgetLayout.appWidgetId] used for a desktop app's grid rectangle. */
		const val NO_WIDGET_ID = -1

		/**
		 * Cells a desktop app spans on each axis. 2 gives an icon roughly the size
		 * of a dash icon (~2 of the ~48dp widget cells ≈ a ~96dp tap target).
		 */
		const val SPAN = 2
	}
}

package be.robinj.distrohopper.widgets

import org.json.JSONException
import org.json.JSONObject

/**
 * Position of an app pinned to the home screen grid, plus which desktop (page)
 * it lives on. Unlike a widget, a desktop app always occupies a single 1x1
 * cell, so it has no span; it is identified by its app's [App.profileScopedKey]
 * (so the same package in a different profile stays distinct).
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
	 * The 1x1 grid rectangle this app occupies, as a [WidgetLayout] (with no
	 * widget id), so it can be fed to [WidgetGrid]'s collision maths alongside
	 * the real widgets.
	 */
	fun toGridRect(): WidgetLayout = WidgetLayout(NO_WIDGET_ID, this.col, this.row, 1, 1, this.page)

	@Throws(JSONException::class)
	fun toJson(): JSONObject = JSONObject().apply {
		put("key", key)
		put("col", col)
		put("row", row)
		put("page", page)
	}

	companion object {
		/** The [WidgetLayout.appWidgetId] used for a desktop app's grid rectangle. */
		const val NO_WIDGET_ID = -1

		@JvmStatic
		@Throws(JSONException::class)
		fun fromJson(json: JSONObject): DesktopAppLayout = DesktopAppLayout(
			json.getString("key"),
			json.getInt("col"),
			json.getInt("row"),
			json.optInt("page", 0),
		)
	}
}

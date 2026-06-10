package be.robinj.distrohopper.widgets

import org.json.JSONException
import org.json.JSONObject

/**
 * Position and size of a widget on the home screen grid.
 */
data class WidgetLayout(
	@JvmField var appWidgetId: Int,
	@JvmField var col: Int,
	@JvmField var row: Int,
	@JvmField var colSpan: Int,
	@JvmField var rowSpan: Int,
) {
	@Throws(JSONException::class)
	fun toJson(): JSONObject = JSONObject().apply {
		put("id", appWidgetId)
		put("col", col)
		put("row", row)
		put("colSpan", colSpan)
		put("rowSpan", rowSpan)
	}

	companion object {
		@JvmStatic
		@Throws(JSONException::class)
		fun fromJson(json: JSONObject): WidgetLayout = WidgetLayout(
			json.getInt("id"),
			json.getInt("col"),
			json.getInt("row"),
			json.getInt("colSpan"),
			json.getInt("rowSpan"),
		)
	}
}

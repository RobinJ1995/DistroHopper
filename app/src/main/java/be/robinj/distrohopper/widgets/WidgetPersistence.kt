package be.robinj.distrohopper.widgets

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import be.robinj.distrohopper.dev.Log
import be.robinj.distrohopper.preferences.Preferences

/**
 * Saves and loads widget placements as JSON in a dedicated preferences file.
 */
class WidgetPersistence(context: Context) {
	private val prefs = Preferences.getSharedPreferences(context, Preferences.WIDGETS)

	fun save(layouts: List<WidgetLayout>) {
		val json = JSONArray()

		for (layout in layouts) {
			try {
				json.put(layout.toJson())
			} catch (ex: JSONException) {
				Log.getInstance().e(this.javaClass.simpleName,
					"Failed to serialise widget ${layout.appWidgetId}")
			}
		}

		this.prefs.edit().putString(KEY, json.toString()).apply()
	}

	fun load(): List<WidgetLayout> {
		val layouts = mutableListOf<WidgetLayout>()
		val str = this.prefs.getString(KEY, null) ?: return layouts

		try {
			val json = JSONArray(str)

			for (i in 0 until json.length()) {
				try {
					layouts.add(WidgetLayout.fromJson(json.getJSONObject(i)))
				} catch (ex: JSONException) {
					Log.getInstance().e(this.javaClass.simpleName,
						"Skipping malformed widget entry: ${ex.message}")
				}
			}
		} catch (ex: JSONException) {
			Log.getInstance().e(this.javaClass.simpleName,
				"Failed to parse saved widgets: ${ex.message}")
		}

		return layouts
	}

	companion object {
		private const val KEY = "widgets"
	}
}

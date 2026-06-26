package be.robinj.distrohopper.widgets

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import be.robinj.distrohopper.dev.Log
import be.robinj.distrohopper.preferences.Preferences

/**
 * Saves and loads desktop-pinned app placements as JSON in a dedicated
 * preferences file, kept separate from the widgets and the launcher-bar pins.
 *
 * @see WidgetPersistence for the widget equivalent.
 */
class DesktopAppPersistence(context: Context) {
	private val prefs = Preferences.getSharedPreferences(context, Preferences.DESKTOP_APPS)

	fun save(layouts: List<DesktopAppLayout>) {
		val json = JSONArray()

		for (layout in layouts) {
			try {
				json.put(layout.toJson())
			} catch (ex: JSONException) {
				Log.getInstance().e(this.javaClass.simpleName,
					"Failed to serialise desktop app ${layout.key}")
			}
		}

		this.prefs.edit().putString(KEY, json.toString()).apply()
	}

	fun load(): List<DesktopAppLayout> {
		val layouts = mutableListOf<DesktopAppLayout>()
		val str = this.prefs.getString(KEY, null) ?: return layouts

		try {
			val json = JSONArray(str)

			for (i in 0 until json.length()) {
				try {
					layouts.add(DesktopAppLayout.fromJson(json.getJSONObject(i)))
				} catch (ex: JSONException) {
					Log.getInstance().e(this.javaClass.simpleName,
						"Skipping malformed desktop app entry: ${ex.message}")
				}
			}
		} catch (ex: JSONException) {
			Log.getInstance().e(this.javaClass.simpleName,
				"Failed to parse saved desktop apps: ${ex.message}")
		}

		return layouts
	}

	companion object {
		private const val KEY = "desktop_apps"
	}
}

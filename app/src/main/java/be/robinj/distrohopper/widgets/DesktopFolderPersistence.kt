package be.robinj.distrohopper.widgets

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import be.robinj.distrohopper.dev.Log
import be.robinj.distrohopper.preferences.Preferences

/**
 * Saves and loads desktop folder placements (and their contents) as JSON in a
 * dedicated preferences file, kept separate from the desktop apps and widgets.
 *
 * @see DesktopAppPersistence, [WidgetPersistence]
 */
class DesktopFolderPersistence(context: Context) {
	private val prefs = Preferences.getSharedPreferences(context, Preferences.DESKTOP_FOLDERS)

	fun save(layouts: List<DesktopFolderLayout>) {
		val json = JSONArray()

		for (layout in layouts) {
			try {
				json.put(layout.toJson())
			} catch (ex: JSONException) {
				Log.getInstance().e(this.javaClass.simpleName,
					"Failed to serialise desktop folder ${layout.folderId}")
			}
		}

		this.prefs.edit().putString(KEY, json.toString()).apply()
	}

	fun load(): List<DesktopFolderLayout> {
		val layouts = mutableListOf<DesktopFolderLayout>()
		val str = this.prefs.getString(KEY, null) ?: return layouts

		try {
			val json = JSONArray(str)

			for (i in 0 until json.length()) {
				try {
					layouts.add(DesktopFolderLayout.fromJson(json.getJSONObject(i)))
				} catch (ex: JSONException) {
					Log.getInstance().e(this.javaClass.simpleName,
						"Skipping malformed desktop folder entry: ${ex.message}")
				}
			}
		} catch (ex: JSONException) {
			Log.getInstance().e(this.javaClass.simpleName,
				"Failed to parse saved desktop folders: ${ex.message}")
		}

		return layouts
	}

	companion object {
		private const val KEY = "desktop_folders"
	}
}

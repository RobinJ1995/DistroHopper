package be.robinj.distrohopper

import android.content.SharedPreferences
import be.robinj.distrohopper.dev.Log
import be.robinj.distrohopper.folder.Folder
import be.robinj.distrohopper.folder.FolderMember
import org.json.JSONArray
import org.json.JSONObject

/**
 * On-disk format of the dash layout — folders and the manual ("custom") order —
 * stored in the [be.robinj.distrohopper.preferences.Preferences.DASH_LAYOUT]
 * file, the dash counterpart to [PinnedAppsStorage]. Partitioned by profile (the
 * dash shows one swipeable page per profile), keyed by a stable profile key
 * ("personal", or the work profile's serial). Stored as a single JSON object
 * under [KEY]:
 *
 * ```
 * { "personal": { "folders": [ { "id": "folder-…", "apps": ["pkg\nact", …] } ],
 *                 "order":   ["app:pkg\nact", "folder:folder-…", …] }, … }
 * ```
 *
 * `order` is the manual arrangement of item ids; items absent from it sort after
 * the listed ones (alphabetically). Folder members are
 * [App.getProfileScopedKey] strings.
 */
object DashLayoutStorage {
	const val KEY = "dash_layout"

	/** A single profile's persisted layout. */
	data class ProfileLayout(val folders: List<Folder>, val order: List<String>)

	@JvmStatic
	fun read(prefs: SharedPreferences): Map<String, ProfileLayout> {
		val raw = prefs.getString(KEY, null) ?: return emptyMap()

		return try {
			val root = JSONObject(raw)
			val out = LinkedHashMap<String, ProfileLayout>()
			for (profileKey in root.keys()) {
				val profile = root.getJSONObject(profileKey)
				out[profileKey] = ProfileLayout(
					folders = readFolders(profile.optJSONArray("folders")),
					order = readStrings(profile.optJSONArray("order")),
				)
			}

			out
		} catch (ex: Exception) {
			Log.getInstance().w("DashLayoutStorage", "Discarding unreadable dash layout: " + ex.message)
			emptyMap()
		}
	}

	@JvmStatic
	fun write(prefs: SharedPreferences, data: Map<String, ProfileLayout>) {
		val root = JSONObject()
		for ((profileKey, layout) in data) {
			val folders = JSONArray()
			for (folder in layout.folders) {
				val apps = JSONArray()
				folder.appKeys.forEach { apps.put(it) }
				folders.put(JSONObject().put("id", folder.id).put("apps", apps))
			}

			val order = JSONArray()
			layout.order.forEach { order.put(it) }

			root.put(profileKey, JSONObject().put("folders", folders).put("order", order))
		}

		prefs.edit().putString(KEY, root.toString()).apply()
	}

	private fun readFolders(array: JSONArray?): List<Folder> {
		if (array == null) {
			return emptyList()
		}

		val folders = ArrayList<Folder>()
		for (i in 0 until array.length()) {
			val obj = array.optJSONObject(i) ?: continue
			val id = obj.optString("id").ifEmpty { continue }
			val members = readStrings(obj.optJSONArray("apps"))
				.map { FolderMember.AppMember(it) as FolderMember }
			folders.add(Folder(id, members))
		}

		return folders
	}

	private fun readStrings(array: JSONArray?): List<String> {
		if (array == null) {
			return emptyList()
		}

		val out = ArrayList<String>(array.length())
		for (i in 0 until array.length()) {
			array.optString(i, null)?.let { out.add(it) }
		}

		return out
	}
}

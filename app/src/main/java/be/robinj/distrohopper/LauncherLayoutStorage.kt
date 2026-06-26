package be.robinj.distrohopper

import android.content.SharedPreferences
import be.robinj.distrohopper.dev.Log
import be.robinj.distrohopper.folder.Folder
import be.robinj.distrohopper.folder.FolderMember
import org.json.JSONArray
import org.json.JSONObject

/**
 * On-disk format of the launcher's folders + manual item order, in the
 * [be.robinj.distrohopper.preferences.Preferences.LAUNCHER_LAYOUT] file. The
 * launcher counterpart to [DashLayoutStorage], but partitioned per **desktop**
 * (the launcher pins are per-desktop) rather than per profile.
 *
 * It is layered over [PinnedAppsStorage] (which still stores every pinned app
 * key per desktop, unchanged): this file only records which of those apps are
 * grouped into folders (and the in-folder member order). The bar's order is the
 * pinned order itself, so there is no separate order to store. If it is missing
 * or unreadable every pinned app simply renders loose, exactly as before folders
 * existed.
 *
 * Stored as a single JSON object under [KEY], keyed by desktop index:
 * ```
 * { "0": { "folders": [ { "id": "folder-…", "apps": ["pkg\nact", …] } ] }, … }
 * ```
 */
object LauncherLayoutStorage {
	const val KEY = "launcher_layout"

	/** A single desktop's persisted launcher folders. */
	data class DesktopLayout(val folders: List<Folder>)

	@JvmStatic
	fun read(prefs: SharedPreferences): Map<Int, DesktopLayout> {
		val raw = prefs.getString(KEY, null) ?: return emptyMap()

		return try {
			val root = JSONObject(raw)
			val out = LinkedHashMap<Int, DesktopLayout>()
			for (key in root.keys()) {
				val desktop = key.toIntOrNull() ?: continue
				val obj = root.getJSONObject(key)
				out[desktop] = DesktopLayout(readFolders(obj.optJSONArray("folders")))
			}

			out
		} catch (ex: Exception) {
			Log.getInstance().w("LauncherLayoutStorage", "Discarding unreadable launcher layout: " + ex.message)
			emptyMap()
		}
	}

	@JvmStatic
	fun write(prefs: SharedPreferences, data: Map<Int, DesktopLayout>) {
		val root = JSONObject()
		for ((desktop, layout) in data) {
			val folders = JSONArray()
			for (folder in layout.folders) {
				val apps = JSONArray()
				folder.appKeys.forEach { apps.put(it) }
				folders.put(JSONObject().put("id", folder.id).put("apps", apps))
			}

			root.put(desktop.toString(), JSONObject().put("folders", folders))
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

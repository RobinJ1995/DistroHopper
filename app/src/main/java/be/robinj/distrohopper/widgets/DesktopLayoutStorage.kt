package be.robinj.distrohopper.widgets

import android.content.SharedPreferences
import be.robinj.distrohopper.dev.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * The unified on-disk format of the home-screen desktop — its widgets, pinned
 * apps and folders — in a single
 * [be.robinj.distrohopper.preferences.Preferences.DESKTOP_LAYOUT] file. Replaces
 * the three former per-kind files (widgets / desktop_apps / desktop_folders):
 * all three describe items on the same grid with the same `col`/`row`/`page`
 * coordinates, so they now share one schema, the desktop counterpart to
 * [be.robinj.distrohopper.DashLayoutStorage].
 *
 * Partitioned per desktop **page** (the pager child index), each page a single
 * heterogeneous, type-tagged `items` array. The page is the enclosing key rather
 * than a field on each item (so the two can never drift); it is re-applied to
 * each model's `page` on read. Stored as a single JSON object under [KEY]:
 *
 * ```
 * { "0": { "items": [
 *     { "type": "widget", "id": 42,           "col": 0, "row": 0, "colSpan": 2, "rowSpan": 2 },
 *     { "type": "app",    "key": "pkg\nact",  "col": 2, "row": 0 },
 *     { "type": "folder", "id": "folder-…",   "col": 4, "row": 0,
 *       "cells": [ { "app": "pkg\nact", "col": 0, "row": 0, "colSpan": 1, "rowSpan": 1 } ] }
 * ] }, … }
 * ```
 *
 * The three owning hosts ([WidgetHost], [DesktopAppHost], [DesktopFolderHost])
 * each read and write only their own kind through the flat-list helpers below;
 * every writer re-reads first and replaces only its slice, so a single host
 * persisting can never clobber the other two. This is safe because all desktop
 * persistence happens on the UI thread.
 */
object DesktopLayoutStorage {
	const val KEY = "desktop_layout"

	private const val TAG = "DesktopLayoutStorage"

	/** A single desktop page's contents, split by kind. */
	data class PageLayout(
		val widgets: List<WidgetLayout> = emptyList(),
		val apps: List<DesktopAppLayout> = emptyList(),
		val folders: List<DesktopFolderLayout> = emptyList(),
	) {
		fun isEmpty(): Boolean =
			this.widgets.isEmpty() && this.apps.isEmpty() && this.folders.isEmpty()
	}

	// --- Whole-file (de)serialization -------------------------------------

	@JvmStatic
	fun read(prefs: SharedPreferences): Map<Int, PageLayout> {
		val raw = prefs.getString(KEY, null) ?: return emptyMap()

		return try {
			val root = JSONObject(raw)
			val out = LinkedHashMap<Int, PageLayout>()
			for (key in root.keys()) {
				val page = key.toIntOrNull() ?: continue
				val items = root.getJSONObject(key).optJSONArray("items") ?: continue

				val widgets = ArrayList<WidgetLayout>()
				val apps = ArrayList<DesktopAppLayout>()
				val folders = ArrayList<DesktopFolderLayout>()
				for (i in 0 until items.length()) {
					val obj = items.optJSONObject(i) ?: continue
					when (obj.optString("type")) {
						"widget" -> parseWidget(obj, page)?.let { widgets.add(it) }
						"app" -> parseApp(obj, page)?.let { apps.add(it) }
						"folder" -> parseFolder(obj, page)?.let { folders.add(it) }
					}
				}

				out[page] = PageLayout(widgets, apps, folders)
			}

			out
		} catch (ex: Exception) {
			Log.getInstance().w(TAG, "Discarding unreadable desktop layout: " + ex.message)
			emptyMap()
		}
	}

	@JvmStatic
	fun write(prefs: SharedPreferences, data: Map<Int, PageLayout>) {
		val root = JSONObject()
		for (page in data.keys.sorted()) {
			val layout = data.getValue(page)
			if (layout.isEmpty()) {
				// Don't grow the blob with empty pages (e.g. the pager's trailing
				// empty desktop), matching LauncherLayoutStorage.
				continue
			}

			val items = JSONArray()
			layout.widgets.forEach { putItem(items, ::widgetJson, it) }
			layout.apps.forEach { putItem(items, ::appJson, it) }
			layout.folders.forEach { putItem(items, ::folderJson, it) }

			root.put(page.toString(), JSONObject().put("items", items))
		}

		prefs.edit().putString(KEY, root.toString()).apply()
	}

	// --- Per-kind flat-list helpers (page carried on each item) -----------

	@JvmStatic
	fun readWidgets(prefs: SharedPreferences): List<WidgetLayout> =
		read(prefs).values.flatMap { it.widgets }

	@JvmStatic
	fun readApps(prefs: SharedPreferences): List<DesktopAppLayout> =
		read(prefs).values.flatMap { it.apps }

	@JvmStatic
	fun readFolders(prefs: SharedPreferences): List<DesktopFolderLayout> =
		read(prefs).values.flatMap { it.folders }

	/** Replaces the widget slice on every page, preserving the apps and folders. */
	@JvmStatic
	fun writeWidgets(prefs: SharedPreferences, widgets: List<WidgetLayout>) {
		val byPage = widgets.groupBy { it.page }
		write(prefs, merge(read(prefs), byPage.keys) { page, existing ->
			existing.copy(widgets = byPage[page].orEmpty())
		})
	}

	/** Replaces the desktop-app slice on every page, preserving the widgets and folders. */
	@JvmStatic
	fun writeApps(prefs: SharedPreferences, apps: List<DesktopAppLayout>) {
		val byPage = apps.groupBy { it.page }
		write(prefs, merge(read(prefs), byPage.keys) { page, existing ->
			existing.copy(apps = byPage[page].orEmpty())
		})
	}

	/** Replaces the folder slice on every page, preserving the widgets and apps. */
	@JvmStatic
	fun writeFolders(prefs: SharedPreferences, folders: List<DesktopFolderLayout>) {
		val byPage = folders.groupBy { it.page }
		write(prefs, merge(read(prefs), byPage.keys) { page, existing ->
			existing.copy(folders = byPage[page].orEmpty())
		})
	}

	// --- Helpers -----------------------------------------------------------

	private inline fun merge(
		current: Map<Int, PageLayout>,
		newPages: Set<Int>,
		replace: (Int, PageLayout) -> PageLayout,
	): Map<Int, PageLayout> {
		val merged = LinkedHashMap<Int, PageLayout>()
		for (page in (current.keys + newPages).sorted()) {
			merged[page] = replace(page, current[page] ?: PageLayout())
		}

		return merged
	}

	private inline fun <T> putItem(array: JSONArray, toJson: (T) -> JSONObject, item: T) {
		try {
			array.put(toJson(item))
		} catch (ex: JSONException) {
			Log.getInstance().e(TAG, "Failed to serialise desktop item: " + ex.message)
		}
	}

	private fun widgetJson(w: WidgetLayout): JSONObject = JSONObject()
		.put("type", "widget")
		.put("id", w.appWidgetId)
		.put("col", w.col)
		.put("row", w.row)
		.put("colSpan", w.colSpan)
		.put("rowSpan", w.rowSpan)

	private fun appJson(a: DesktopAppLayout): JSONObject = JSONObject()
		.put("type", "app")
		.put("key", a.key)
		.put("col", a.col)
		.put("row", a.row)

	private fun folderJson(f: DesktopFolderLayout): JSONObject = JSONObject()
		.put("type", "folder")
		.put("id", f.folderId)
		.put("col", f.col)
		.put("row", f.row)
		.put("cells", JSONArray().also { array -> f.cells.forEach { array.put(it.toJson()) } })

	private fun parseWidget(obj: JSONObject, page: Int): WidgetLayout? = try {
		WidgetLayout(
			obj.getInt("id"), obj.getInt("col"), obj.getInt("row"),
			obj.getInt("colSpan"), obj.getInt("rowSpan"), page,
		)
	} catch (ex: JSONException) {
		Log.getInstance().e(TAG, "Skipping malformed widget item: " + ex.message)
		null
	}

	private fun parseApp(obj: JSONObject, page: Int): DesktopAppLayout? = try {
		DesktopAppLayout(obj.getString("key"), obj.getInt("col"), obj.getInt("row"), page)
	} catch (ex: JSONException) {
		Log.getInstance().e(TAG, "Skipping malformed desktop app item: " + ex.message)
		null
	}

	private fun parseFolder(obj: JSONObject, page: Int): DesktopFolderLayout? = try {
		val id = obj.getString("id")
		val cellsArray = obj.optJSONArray("cells") ?: JSONArray()
		val cells = ArrayList<DesktopFolderCell>(cellsArray.length())
		for (i in 0 until cellsArray.length()) {
			DesktopFolderCell.fromJson(cellsArray.getJSONObject(i))?.let { cells.add(it) }
		}

		DesktopFolderLayout(id, obj.getInt("col"), obj.getInt("row"), page, cells)
	} catch (ex: JSONException) {
		Log.getInstance().e(TAG, "Skipping malformed desktop folder item: " + ex.message)
		null
	}
}

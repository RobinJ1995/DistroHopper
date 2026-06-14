package be.robinj.distrohopper.widgets

import be.robinj.distrohopper.folder.FolderGrid
import be.robinj.distrohopper.folder.FolderMember
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * A folder placed on the home-screen grid. Like a [DesktopAppLayout] it occupies
 * a fixed [SPAN]x[SPAN] block on the desktop's 8x8 grid (so it reads as one
 * icon), but its contents are themselves laid out on a small [FolderGrid] (3x3):
 * apps as 1x1 [cells][DesktopFolderCell] and widgets at their own span, so an
 * app and a widget must together fit within 3x3 (e.g. four apps leave room only
 * for a ≤2x2 widget). A desktop folder must hold at least one app.
 *
 * @see DesktopAppLayout for a plain desktop app, [WidgetLayout] for a widget.
 */
data class DesktopFolderLayout(
	@JvmField val folderId: String,
	@JvmField var col: Int,
	@JvmField var row: Int,
	@JvmField var page: Int = 0,
	@JvmField val cells: List<DesktopFolderCell> = emptyList(),
) {
	val appKeys: List<String>
		get() = this.cells.mapNotNull { (it.member as? FolderMember.AppMember)?.key }

	val widgetIds: List<Int>
		get() = this.cells.mapNotNull { (it.member as? FolderMember.WidgetMember)?.appWidgetId }

	val appCount: Int get() = this.appKeys.size

	/** The folder's 2x2 footprint on the desktop grid (for collision checks). */
	fun toGridRect(): WidgetLayout =
		WidgetLayout(DesktopAppLayout.NO_WIDGET_ID, this.col, this.row, SPAN, SPAN, this.page)

	private fun occupied(): List<FolderGrid.Rect> = this.cells.map { it.rect() }

	/** Adds an app (1x1) at the first free 3x3 cell, or null if the folder is full. */
	fun withApp(key: String): DesktopFolderLayout? {
		if (this.appKeys.contains(key)) {
			return this
		}
		val rect = FolderGrid.findFreeRect(this.occupied(), 1, 1) ?: return null

		return this.copy(cells = this.cells +
			DesktopFolderCell(FolderMember.AppMember(key), rect.col, rect.row, 1, 1))
	}

	/**
	 * Adds a widget of [colSpan]x[rowSpan] at the first free 3x3 block that fits
	 * alongside the current contents, or null if there is no room.
	 */
	fun withWidget(appWidgetId: Int, colSpan: Int, rowSpan: Int): DesktopFolderLayout? {
		if (this.widgetIds.contains(appWidgetId)) {
			return this
		}
		val rect = FolderGrid.findFreeRect(this.occupied(), colSpan, rowSpan) ?: return null

		return this.copy(cells = this.cells +
			DesktopFolderCell(FolderMember.WidgetMember(appWidgetId), rect.col, rect.row,
				rect.colSpan, rect.rowSpan))
	}

	fun without(member: FolderMember): DesktopFolderLayout =
		this.copy(cells = this.cells.filterNot { it.member == member })

	@Throws(JSONException::class)
	fun toJson(): JSONObject = JSONObject().apply {
		put("id", folderId)
		put("col", col)
		put("row", row)
		put("page", page)
		put("cells", JSONArray().also { array -> cells.forEach { array.put(it.toJson()) } })
	}

	companion object {
		/** Cells a desktop folder spans on each axis (same as a desktop app icon). */
		const val SPAN = DesktopAppLayout.SPAN

		@JvmStatic
		@Throws(JSONException::class)
		fun fromJson(json: JSONObject): DesktopFolderLayout {
			val cellsArray = json.optJSONArray("cells") ?: JSONArray()
			val cells = ArrayList<DesktopFolderCell>(cellsArray.length())
			for (i in 0 until cellsArray.length()) {
				DesktopFolderCell.fromJson(cellsArray.getJSONObject(i))?.let { cells.add(it) }
			}

			return DesktopFolderLayout(
				json.getString("id"),
				json.getInt("col"),
				json.getInt("row"),
				json.optInt("page", 0),
				cells,
			)
		}
	}
}

/** One member of a [DesktopFolderLayout], placed on the folder's 3x3 grid. */
data class DesktopFolderCell(
	val member: FolderMember,
	val col: Int,
	val row: Int,
	val colSpan: Int,
	val rowSpan: Int,
) {
	fun rect(): FolderGrid.Rect = FolderGrid.Rect(this.col, this.row, this.colSpan, this.rowSpan)

	@Throws(JSONException::class)
	fun toJson(): JSONObject = JSONObject().apply {
		when (val m = member) {
			is FolderMember.AppMember -> put("app", m.key)
			is FolderMember.WidgetMember -> put("widget", m.appWidgetId)
		}
		put("col", col)
		put("row", row)
		put("colSpan", colSpan)
		put("rowSpan", rowSpan)
	}

	companion object {
		@JvmStatic
		fun fromJson(json: JSONObject): DesktopFolderCell? {
			val member: FolderMember = when {
				json.has("app") -> FolderMember.AppMember(json.getString("app"))
				json.has("widget") -> FolderMember.WidgetMember(json.getInt("widget"))
				else -> return null
			}

			return DesktopFolderCell(
				member,
				json.getInt("col"),
				json.getInt("row"),
				json.optInt("colSpan", 1),
				json.optInt("rowSpan", 1),
			)
		}
	}
}

package be.robinj.distrohopper.desktop.dash

import be.robinj.distrohopper.App
import be.robinj.distrohopper.folder.Folder

/**
 * One cell in the dash grid: either a standalone app or a folder of apps. The
 * dash grid adapter is fed a list of these (built by
 * [be.robinj.distrohopper.DashLayoutRepository]) instead of raw [App]s.
 */
sealed interface DashItem {
	data class AppItem(val app: App) : DashItem

	/** [apps] is the folder's resolved, installed members in order (always ≥2 when rendered). */
	data class FolderItem(val folder: Folder, val apps: List<App>) : DashItem
}

/**
 * A stable string identity for a dash item, used to match a cell to itself
 * across a drag-reorder preview (so the grid's reflow animation can translate
 * each icon from its old slot to its new one) and to mark the dragged cell as
 * an empty placeholder. App and folder ids are prefixed so they can never
 * collide (mirrors [be.robinj.distrohopper.DashLayoutRepository]'s scheme).
 */
val DashItem.stableKey: String
	get() = when (this) {
		is DashItem.AppItem -> "app:" + this.app.profileScopedKey
		is DashItem.FolderItem -> "folder:" + this.folder.id
	}

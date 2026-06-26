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

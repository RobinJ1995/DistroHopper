package be.robinj.distrohopper.desktop.launcher

import be.robinj.distrohopper.App
import be.robinj.distrohopper.folder.Folder

/**
 * One slot in the launcher's pinned bar for a desktop: a pinned app or a folder
 * of pinned apps. The launcher counterpart to
 * [be.robinj.distrohopper.desktop.dash.DashItem]. Built by
 * [be.robinj.distrohopper.LauncherLayoutRepository] over the desktop's pinned
 * apps plus its persisted folders/order.
 */
sealed interface LauncherItem {
	data class LauncherApp(val app: App) : LauncherItem

	/** [apps] is the folder's resolved, pinned members in order (always ≥2 when rendered). */
	data class LauncherFolder(val folder: Folder, val apps: List<App>) : LauncherItem
}

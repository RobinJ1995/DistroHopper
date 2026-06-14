package be.robinj.distrohopper.desktop.launcher

import be.robinj.distrohopper.App

/**
 * The `localState` for launcher drags that are not a plain pinned app: a whole
 * folder (reposition / drop on trash to delete) or a folder member dragged out
 * of a launcher folder. Plain pinned apps keep using the existing pinned-index
 * `ClipData` drag, so the reorder/trash/desktop paths are unchanged for them.
 */
sealed interface LauncherDragPayload {
	data class FolderDrag(val folderId: String) : LauncherDragPayload

	data class FolderMemberDrag(val folderId: String, val app: App) : LauncherDragPayload
}

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

	/**
	 * A launcher-pinned app picked up from the *dash*. The dash is its own surface,
	 * so to every other surface this is a dash pin-by-drop — dropping it on the
	 * desktop pins a separate desktop copy and leaves the launcher pin intact (it
	 * is NOT the pinned-index reorder clip, which the desktop unpins off the bar).
	 * The dash grid ignores it (not a [DashDragPayload]/[App]), so it falls through
	 * to the desktop just like the old index clip did. The launcher bar's own
	 * reorder still works — its bookkeeping keys off the dragged pin, not this.
	 */
	data class PinnedAppDrag(val app: App) : LauncherDragPayload
}

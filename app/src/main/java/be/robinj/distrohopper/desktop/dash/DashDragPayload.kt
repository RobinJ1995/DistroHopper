package be.robinj.distrohopper.desktop.dash

import be.robinj.distrohopper.App

/**
 * The `localState` for dash drags that are NOT a plain loose app. A loose dash
 * app keeps passing its [App] as the drag's local state (so the launcher's
 * pin-by-drop and the trash listener keep recognising it unchanged); folders and
 * in-folder members carry one of these instead, which only the dash grid and
 * trash listeners interpret.
 */
sealed interface DashDragPayload {
	/** A whole folder being repositioned (custom order) or dropped on the trash. */
	data class FolderDrag(val folderId: String) : DashDragPayload

	/** A single app dragged out of folder [folderId] (extract / reorder within it). */
	data class FolderMemberDrag(val folderId: String, val app: App) : DashDragPayload
}

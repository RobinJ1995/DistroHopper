package be.robinj.distrohopper.widgets

import be.robinj.distrohopper.folder.FolderMember

/**
 * The drag `localState` for a member pulled out of an open desktop folder (see
 * [DesktopFolderOverlay]) — the desktop counterpart of the dash/launcher
 * `*DragPayload.FolderMemberDrag`. [colSpan]/[rowSpan] are the member's footprint
 * once back on the desktop grid (a [FolderMember.AppMember] is
 * [DesktopFolderLayout.SPAN]-square; a [FolderMember.WidgetMember] keeps its own
 * span). Dropping on the desktop extracts it loose; dropping on the trash
 * deletes it.
 */
data class DesktopFolderMemberDrag(
	val folderId: String,
	val member: FolderMember,
	val colSpan: Int,
	val rowSpan: Int,
)

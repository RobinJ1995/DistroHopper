package be.robinj.distrohopper.widgets

import be.robinj.distrohopper.folder.FolderMember

/**
 * The drag `localState` for a member pulled out of an open desktop folder (see
 * [DesktopFolderOverlay]) — the desktop counterpart of the dash/launcher
 * `*DragPayload.FolderMemberDrag`. The member lands as a
 * [DesktopFolderLayout.SPAN]-square desktop icon. Dropping on the desktop
 * extracts it loose; dropping on the trash deletes it.
 */
data class DesktopFolderMemberDrag(
	val folderId: String,
	val member: FolderMember,
)

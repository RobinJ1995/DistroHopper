package be.robinj.distrohopper.folder

/**
 * A member of a [Folder]. Folders (dash, launcher and desktop alike) only hold
 * apps.
 *
 * Apps are identified by their [be.robinj.distrohopper.App.getProfileScopedKey],
 * the same identity used by the pinned-apps and desktop-apps persistence, so the
 * same package in two profiles stays distinct.
 *
 * This stays a `sealed interface` (rather than collapsing to a bare key) because
 * the dash/launcher/desktop folder models all flow their members through it.
 */
sealed interface FolderMember {
	data class AppMember(val key: String) : FolderMember
}

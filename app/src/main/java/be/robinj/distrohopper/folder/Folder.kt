package be.robinj.distrohopper.folder

/**
 * A surface-agnostic group of apps (and, on the desktop, widgets). Folders are
 * never named (see the feature spec) and exist on a single surface — they
 * cannot be dragged between the dash, launcher and desktop, only their contents
 * can. The [members] list is the authoritative order for the apps-only dash and
 * launcher folders; the desktop adds per-member grid placement separately (see
 * [be.robinj.distrohopper.widgets.DesktopFolderLayout]).
 *
 * A folder must hold at least one app to exist; a folder that drops to a single
 * app is dissolved back into a loose app by its host.
 */
data class Folder(
	val id: String,
	val members: List<FolderMember>,
) {
	val appKeys: List<String>
		get() = this.members.filterIsInstance<FolderMember.AppMember>().map { it.key }

	companion object {
		/** A new, collision-resistant folder id. */
		@JvmStatic
		fun newId(): String = "folder-" + java.util.UUID.randomUUID().toString()
	}
}

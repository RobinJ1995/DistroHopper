package be.robinj.distrohopper.folder

/**
 * A member of a [Folder]. The dash and launcher folders only ever hold
 * [AppMember]s; desktop folders may additionally hold [WidgetMember]s.
 *
 * Apps are identified by their [be.robinj.distrohopper.App.getProfileScopedKey],
 * the same identity used by the pinned-apps and desktop-apps persistence, so the
 * same package in two profiles stays distinct. Widgets are identified by their
 * Android `appWidgetId`.
 */
sealed interface FolderMember {
	data class AppMember(val key: String) : FolderMember

	data class WidgetMember(val appWidgetId: Int) : FolderMember
}

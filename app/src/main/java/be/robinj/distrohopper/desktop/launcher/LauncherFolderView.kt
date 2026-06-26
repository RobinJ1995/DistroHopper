package be.robinj.distrohopper.desktop.launcher

import android.content.Context
import android.util.AttributeSet
import be.robinj.distrohopper.App
import be.robinj.distrohopper.folder.FolderIconDrawable

/**
 * A folder icon on the launcher's pinned bar. Extends the launcher [AppLauncher]
 * (via its attrs constructor) purely to inherit the dock icon's exact sizing and
 * theming, then shows the folder's [FolderIconDrawable] mini-grid in place of an
 * app icon and carries no label (folders are unnamed). Its tag is the folder id
 * so the bar can find it; it holds no [App], so it must be given folder-specific
 * click/long-press/drag listeners rather than the app ones.
 */
class LauncherFolderView(
	context: Context,
	val folderId: String,
	val apps: List<App>,
) : AppLauncher(context, null as AttributeSet?) {
	init {
		this.setIcon(FolderIconDrawable(this.apps.map { it.icon.drawable }))
		this.tag = this.folderId
	}
}

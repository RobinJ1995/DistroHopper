package be.robinj.distrohopper.widgets

import android.content.ClipData
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import be.robinj.distrohopper.App
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.folder.FolderIconDrawable
import be.robinj.distrohopper.home.LauncherBarBinder

/**
 * A folder placed on a desktop: a [DesktopFolderLayout.SPAN]-square icon in the
 * [WidgetsContainer] grid, the folder-world counterpart of [DesktopAppView]. It
 * shows a [FolderIconDrawable] mini-grid of member app icons under its first
 * member's label (so it reads like the labelled apps around it on the desktop,
 * rather than a label-less gap). Tap
 * opens the folder overlay; long-press starts a system
 * drag with the view as local state (clip "desktopFolder"), so the desktop drag
 * listeners can move it on the grid or drop it on the trash. A folder can't leave
 * the desktop — only its contents can.
 */
class DesktopFolderView(
	context: Context,
	firstApp: App,
	@JvmField var layout: DesktopFolderLayout,
	iconDrawables: List<Drawable>,
	private val onOpen: (DesktopFolderView) -> Unit,
) : be.robinj.distrohopper.desktop.AppLauncher(
	context, firstApp, R.layout.widget_desktop_applauncher, R.layout.widget_desktop_applauncher,
) {
	internal var dragGrabOffsetX = 0
	internal var dragGrabOffsetY = 0

	private var lastDownRawX = 0F
	private var lastDownRawY = 0F

	val folderId: String get() = this.layout.folderId

	init {
		this.setIcon(FolderIconDrawable(iconDrawables))
		// Keep the first member's label (set by the base AppLauncher from firstApp)
		// so a desktop folder shows a label like the loose apps around it //
		this.tag = this.layout.folderId

		this.isClickable = true
		this.isLongClickable = true
		this.setOnClickListener { this.onOpen(this) }
		this.setOnLongClickListener {
			this.startMoveDrag()
			true
		}
	}

	override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
		if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
			this.lastDownRawX = ev.rawX
			this.lastDownRawY = ev.rawY
		}

		return super.dispatchTouchEvent(ev)
	}

	private fun startMoveDrag() {
		val parent = this.parent as? WidgetsContainer ?: return

		val location = IntArray(2)
		parent.getLocationOnScreen(location)
		this.dragGrabOffsetX = (this.lastDownRawX - location[0]).toInt() - this.left
		this.dragGrabOffsetY = (this.lastDownRawY - location[1]).toInt() - this.top

		val clip = ClipData.newPlainText("desktopFolder", this.folderId)
		if (! this.startDragAndDrop(clip, View.DragShadowBuilder(this), this, 0)) {
			return
		}

		// Show the trash so the folder can be dropped on it; a folder can't be
		// pinned to the bar, so no launcher placeholder is opened //
		(this.context as? HomeActivity)?.let { LauncherBarBinder.startedDragging(it) }
	}
}

package be.robinj.distrohopper.widgets

import android.content.ClipData
import android.content.Context
import android.view.MotionEvent
import android.view.View
import be.robinj.distrohopper.App
import be.robinj.distrohopper.ExceptionHandler
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.home.LauncherBarBinder

/**
 * An app pinned to a desktop: a label-bearing icon living in a single
 * [WidgetsContainer] grid cell — the app-world counterpart of [WidgetContainer].
 * Built on the base [be.robinj.distrohopper.desktop.AppLauncher] (the
 * label-capable one) rather than the no-label launcher-bar subclass, so a
 * desktop app always shows its label.
 *
 * Tap launches the app; long-press starts a system drag with the view itself as
 * the drag's local state (mirroring [WidgetContainer]), so the existing drag
 * listeners can recognise it with a single `instanceof` and let it be moved on
 * the grid, moved to the launcher bar, or dropped on the trash.
 */
class DesktopAppView(
	context: Context,
	private val pinnedApp: App,
	@JvmField var layout: DesktopAppLayout,
) : be.robinj.distrohopper.desktop.AppLauncher(
	context, pinnedApp, R.layout.widget_desktop_applauncher, R.layout.widget_desktop_applauncher,
) {
	// Where the finger grabbed the icon, relative to its top-left corner; used
	// by WidgetsContainer_DragListener to position the landing indicator //
	internal var dragGrabOffsetX = 0
	internal var dragGrabOffsetY = 0

	private var lastDownRawX = 0F
	private var lastDownRawY = 0F

	val key: String get() = this.layout.key

	init {
		this.isClickable = true
		this.isLongClickable = true

		this.setOnClickListener {
			try {
				this.pinnedApp.launch()
			} catch (ex: Exception) {
				ExceptionHandler(ex).show(this.context)
			}
		}
		this.setOnLongClickListener {
			this.startMoveDrag()
			true
		}
	}

	// Remember where the finger went down so the long-press drag can offset the
	// landing indicator from the icon's corner (a long click carries no coords) //
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

		val clip = ClipData.newPlainText("desktopApp", this.key)
		if (! this.startDragAndDrop(clip, View.DragShadowBuilder(this), this, 0)) {
			return
		}

		// Open the launcher bar's placeholder slot too — exactly like a dash-app
		// drag — so dropping on the bar gets the same "create space" preview and
		// drops at the hovered position, not just appended to the end. The bar
		// listeners then complete the move (unpin from the desktop) on drop //
		val appManager = (this.context as? HomeActivity)?.appManager
		if (appManager != null) {
			appManager.startedDraggingDashApp(this.pinnedApp)
		} else {
			(this.context as? HomeActivity)?.let { LauncherBarBinder.startedDragging(it) }
		}
	}
}

package be.robinj.distrohopper.widgets

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import be.robinj.distrohopper.App
import be.robinj.distrohopper.ExceptionHandler
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.folder.FolderMember
import be.robinj.distrohopper.folder.FolderOverlay

/**
 * The popover for a desktop folder. Unlike the dash/launcher folder popups (a
 * PopupWindow) this is an in-activity overlay added to the activity's own
 * content, so a widget member can be shown **live** by reparenting its retained
 * [WidgetContainer] into it within the same window (a widget allows only one host
 * view per id). It lays the folder's contents out on the 3x3 grid exactly as
 * they are packed: apps as tappable icons (1x1), widgets at their span. The shared
 * [FolderOverlay] dims/blurs the backdrop, opens it centred over the folder icon
 * with an animation, and on close detaches the widgets back to the host's retention.
 */
class DesktopFolderOverlay(
	private val activity: HomeActivity,
	private val host: DesktopFolderHost,
	private val layout: DesktopFolderLayout,
	private val appMap: Map<String, App>,
) {
	private val overlay = FolderOverlay(this.activity)

	fun show(anchor: View) {
		val cell = this.dp(88)
		val pad = this.dp(10)

		val grid = FrameLayout(this.activity).apply {
			setPadding(pad, pad, pad, pad)
			isClickable = true // consume taps so they don't fall through to dismiss //
			background = GradientDrawable().apply {
				cornerRadius = dp(20).toFloat()
				setColor(Color.argb(238, 28, 28, 28))
			}
		}

		for (cell0 in this.layout.cells) {
			val child = this.childFor(cell0) ?: continue
			// Long-press a member to pull it out of the folder: start the drag from
			// the decor view (so it survives the overlay closing) and dismiss.
			child.setOnLongClickListener {
				this.startExtractDrag(cell0, child)
				true
			}
			val lp = FrameLayout.LayoutParams(cell0.colSpan * cell, cell0.rowSpan * cell).apply {
				leftMargin = pad + cell0.col * cell
				topMargin = pad + cell0.row * cell
			}
			grid.addView(child, lp)
		}

		val cols = this.layout.cells.maxOf { it.col + it.colSpan }
		val rows = this.layout.cells.maxOf { it.row + it.rowSpan }
		this.overlay.show(grid, pad * 2 + cols * cell, pad * 2 + rows * cell, anchor) { dismiss() }
	}

	fun dismiss() {
		// Detach widgets after the close animation but before the tree is removed,
		// so they return to the host's off-grid retention rather than being
		// destroyed with the overlay.
		this.overlay.dismiss(beforeRemove = {
			for (id in this.layout.widgetIds) {
				this.host.retainedWidget(id)?.let { (it.parent as? ViewGroup)?.removeView(it) }
			}
		})
	}

	private fun childFor(cell: DesktopFolderCell) = when (val member = cell.member) {
		is FolderMember.AppMember -> this.appMap[member.key]?.let { app ->
			ImageView(this.activity).apply {
				setImageDrawable(app.icon.drawable)
				val p = dp(8)
				setPadding(p, p, p, p)
				isClickable = true
				setOnClickListener {
					try {
						app.launch()
						dismiss()
					} catch (ex: Exception) {
						ExceptionHandler(ex).show(activity)
					}
				}
			}
		}
		is FolderMember.WidgetMember -> this.host.retainedWidget(member.appWidgetId)?.also {
			(it.parent as? ViewGroup)?.removeView(it)
		}
	}

	private fun startExtractDrag(cell: DesktopFolderCell, shadowView: android.view.View) {
		// An extracted app lands as a SPAN-square desktop icon; a widget keeps its span.
		val (colSpan, rowSpan) = when (cell.member) {
			is FolderMember.AppMember -> DesktopFolderLayout.SPAN to DesktopFolderLayout.SPAN
			is FolderMember.WidgetMember -> cell.colSpan to cell.rowSpan
		}
		val payload = DesktopFolderMemberDrag(this.layout.folderId, cell.member, colSpan, rowSpan)
		val clip = android.content.ClipData.newPlainText("desktopFolderMember", this.layout.folderId)
		val source = this.activity.window.decorView

		if (source.startDragAndDrop(clip, android.view.View.DragShadowBuilder(shadowView), payload, 0)) {
			be.robinj.distrohopper.home.LauncherBarBinder.startedDragging(this.activity)
			this.dismiss()
		}
	}

	private fun dp(value: Int): Int =
		(value * this.activity.resources.displayMetrics.density).toInt()
}

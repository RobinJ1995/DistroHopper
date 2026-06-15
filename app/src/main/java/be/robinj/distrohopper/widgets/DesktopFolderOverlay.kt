package be.robinj.distrohopper.widgets

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import be.robinj.distrohopper.App
import be.robinj.distrohopper.ExceptionHandler
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.folder.FolderMember
import be.robinj.distrohopper.folder.FolderOverlay

/**
 * The popover for a desktop folder. Like the dash/launcher folder popups it lays
 * the folder's apps out on the 3x3 grid as tappable icons (1x1), exactly as they
 * are packed. The shared [FolderOverlay] dims/blurs the backdrop and opens it
 * centred over the folder icon with an animation.
 */
class DesktopFolderOverlay(
	private val activity: HomeActivity,
	private val layout: DesktopFolderLayout,
	private val appMap: Map<String, App>,
) {
	private val overlay = FolderOverlay(this.activity)

	fun show(anchor: View) {
		val cell = this.dp(88)
		val pad = this.dp(14)

		val grid = FrameLayout(this.activity).apply {
			// No setPadding here: each cell's margin already includes [pad], and a
			// FrameLayout adds its padding ON TOP of child margins — doubling it and
			// shoving the icons to the bottom-right while the panel size budgets only
			// one [pad]. The margins alone keep the grid centred in the panel.
			isClickable = true // consume taps so they don't fall through to dismiss //
			clipToOutline = true // keep icons inside the rounded panel //
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

	fun dismiss() = this.overlay.dismiss()

	private fun childFor(cell: DesktopFolderCell) = when (val member = cell.member) {
		is FolderMember.AppMember -> this.appMap[member.key]?.let { app ->
			ImageView(this.activity).apply {
				setImageDrawable(app.icon.drawable)
				// Shrink the icon within its cell so adjacent icons (cells are packed
				// edge-to-edge) keep a clear gap and don't crowd the panel.
				val p = dp(15)
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
	}

	private fun startExtractDrag(cell: DesktopFolderCell, shadowView: android.view.View) {
		val payload = DesktopFolderMemberDrag(this.layout.folderId, cell.member)
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

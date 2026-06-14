package be.robinj.distrohopper.widgets

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import be.robinj.distrohopper.App
import be.robinj.distrohopper.ExceptionHandler
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.folder.FolderMember

/**
 * The popover for a desktop folder. Unlike the dash/launcher folder popups (a
 * PopupWindow) this is an in-activity overlay added to the activity's own
 * content, so a widget member can be shown **live** by reparenting its retained
 * [WidgetContainer] into it within the same window (a widget allows only one host
 * view per id). It lays the folder's contents out on the 3x3 grid exactly as
 * they are packed: apps as tappable icons (1x1), widgets at their span. Tapping
 * the dim outside closes it, detaching the widgets back to the host's retention.
 */
class DesktopFolderOverlay(
	private val activity: HomeActivity,
	private val host: DesktopFolderHost,
	private val layout: DesktopFolderLayout,
	private val appMap: Map<String, App>,
) {
	private val content: ViewGroup = this.activity.findViewById(android.R.id.content)
	private var root: FrameLayout? = null

	fun show() {
		val cell = this.dp(88)
		val pad = this.dp(10)

		val grid = FrameLayout(this.activity).apply {
			setPadding(pad, pad, pad, pad)
			background = GradientDrawable().apply {
				cornerRadius = dp(20).toFloat()
				setColor(Color.argb(238, 28, 28, 28))
			}
		}

		for (cell0 in this.layout.cells) {
			val child = this.childFor(cell0) ?: continue
			val lp = FrameLayout.LayoutParams(cell0.colSpan * cell, cell0.rowSpan * cell).apply {
				leftMargin = pad + cell0.col * cell
				topMargin = pad + cell0.row * cell
			}
			grid.addView(child, lp)
		}

		val overlay = FrameLayout(this.activity).apply {
			setBackgroundColor(Color.argb(140, 0, 0, 0))
			isClickable = true
			setOnClickListener { dismiss() }
		}
		overlay.addView(grid, FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER))

		this.root = overlay
		this.content.addView(overlay, FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
	}

	fun dismiss() {
		val overlay = this.root ?: return
		// Detach widgets first so they return to the host's off-grid retention,
		// not get destroyed with the overlay view tree.
		for (id in this.layout.widgetIds) {
			this.host.retainedWidget(id)?.let { (it.parent as? ViewGroup)?.removeView(it) }
		}
		this.content.removeView(overlay)
		this.root = null
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

	private fun dp(value: Int): Int =
		(value * this.activity.resources.displayMetrics.density).toInt()
}

package be.robinj.distrohopper.desktop.dash.workspace

import android.content.Context
import android.os.UserHandle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout

/**
 * GNOME-style workspace indicator: a [WorkspacePillView] at the panel's
 * top-left, shown only while the dash is open (and more than one workspace
 * exists). The pill reflects and animates the current dash page; tapping a
 * slot switches to that profile.
 */
class GnomeWorkspacePillIndicator(
	context: Context,
	private val container: FrameLayout,
	private val onSelect: (Int) -> Unit,
) : WorkspaceIndicator {
	private val pill = WorkspacePillView(context)
	private var hasMultipleWorkspaces = false
	private var dashOpen = false

	init {
		this.container.removeAllViews()
		this.container.addView(this.pill, FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.WRAP_CONTENT,
			FrameLayout.LayoutParams.WRAP_CONTENT,
			Gravity.CENTER_VERTICAL or Gravity.START))
		this.pill.onSlotClick = { this.onSelect(it) }
	}

	override fun bind(workspaces: List<UserHandle?>, selected: Int) {
		this.hasMultipleWorkspaces = workspaces.size > 1
		this.pill.count = workspaces.size
		this.pill.position = selected.toFloat()
		this.updateVisibility()
	}

	override fun onPageScrolled(position: Int, positionOffset: Float) {
		this.pill.position = position + positionOffset
	}

	override fun onPageSelected(position: Int) {
		this.pill.position = position.toFloat()
	}

	override fun onDashOpenChanged(open: Boolean) {
		this.dashOpen = open
		this.updateVisibility()
	}

	private fun updateVisibility() {
		this.container.visibility =
			if (this.hasMultipleWorkspaces && this.dashOpen) View.VISIBLE else View.GONE
	}

	override fun clear() {
		this.hasMultipleWorkspaces = false
		this.updateVisibility()
	}
}

package be.robinj.distrohopper.desktop.dash.profile

import android.content.Context
import android.os.UserHandle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout

/**
 * GNOME-style profile indicator: a [ProfilePillView] at the panel's
 * top-left, shown only while the dash is open (and more than one profile
 * exists). The pill reflects and animates the current dash page; tapping a
 * slot switches to that profile.
 */
class GnomeProfilePillIndicator(
	context: Context,
	private val container: FrameLayout,
	private val onSelect: (Int) -> Unit,
) : ProfileIndicator {
	private val pill = ProfilePillView(context)
	private var hasMultipleProfiles = false
	private var dashOpen = false

	init {
		this.container.removeAllViews()
		this.container.addView(this.pill, FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.WRAP_CONTENT,
			FrameLayout.LayoutParams.WRAP_CONTENT,
			Gravity.CENTER_VERTICAL or Gravity.START))
		this.pill.onSlotClick = { this.onSelect(it) }
	}

	override fun bind(profiles: List<UserHandle?>, selected: Int) {
		this.hasMultipleProfiles = profiles.size > 1
		this.pill.count = profiles.size
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
			if (this.hasMultipleProfiles && this.dashOpen) View.VISIBLE else View.GONE
	}

	override fun clear() {
		this.hasMultipleProfiles = false
		this.updateVisibility()
	}
}

package be.robinj.distrohopper.desktop.dash.workspace

import android.content.Context
import android.os.UserHandle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import be.robinj.distrohopper.R

/**
 * Unity-style workspace indicator: one monochrome glyph per profile (a person
 * for the personal profile, a briefcase for others) in the dash's always-
 * visible bottom ribbon. The current profile's glyph is fully opaque and the
 * others dimmed; the highlight tracks the swipe smoothly. Tapping a glyph
 * switches to that profile.
 */
class UnityRibbonIndicator(
	private val context: Context,
	private val container: LinearLayout,
	private val onSelect: (Int) -> Unit,
) : WorkspaceIndicator {
	private val icons = mutableListOf<ImageView>()
	private var position = 0F

	override fun bind(workspaces: List<UserHandle?>, selected: Int) {
		val density = this.context.resources.displayMetrics.density
		val size = (32 * density).toInt()
		val margin = (6 * density).toInt()
		val padding = (4 * density).toInt()

		this.container.removeAllViews()
		this.icons.clear()

		for ((i, workspace) in workspaces.withIndex()) {
			val icon = ImageView(this.context)
			icon.setImageResource(
				if (workspace == null) R.drawable.ic_workspace_personal
				else R.drawable.ic_workspace_work)
			icon.setPadding(padding, padding, padding, padding)
			val params = LinearLayout.LayoutParams(size, size)
			params.leftMargin = margin
			params.rightMargin = margin
			icon.layoutParams = params
			icon.setOnClickListener { this.onSelect(i) }

			this.container.addView(icon)
			this.icons.add(icon)
		}

		this.container.visibility = View.VISIBLE
		this.position = selected.toFloat()
		this.applyHighlight()
	}

	override fun onPageScrolled(position: Int, positionOffset: Float) {
		this.position = position + positionOffset
		this.applyHighlight()
	}

	override fun onPageSelected(position: Int) {
		this.position = position.toFloat()
		this.applyHighlight()
	}

	private fun applyHighlight() {
		for ((i, icon) in this.icons.withIndex()) {
			val distance = Math.min(Math.abs(this.position - i), 1F)
			icon.alpha = INACTIVE_ALPHA + (1F - INACTIVE_ALPHA) * (1F - distance)
		}
	}

	override fun clear() {
		this.container.removeAllViews()
		this.container.visibility = View.GONE
		this.icons.clear()
	}

	companion object {
		private const val INACTIVE_ALPHA = 0.4F
	}
}

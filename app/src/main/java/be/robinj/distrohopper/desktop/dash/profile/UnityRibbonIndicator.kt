package be.robinj.distrohopper.desktop.dash.profile

import android.content.Context
import android.os.UserHandle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import be.robinj.distrohopper.R

/**
 * Unity-style profile indicator: one monochrome glyph per profile in the
 * dash's always-visible bottom ribbon. Other profiles use the generic profile
 * glyph stamped with the system's own profile badge (so a work profile shows
 * the briefcase, a private space its lock, etc., correct for any profile
 * type), while the personal profile uses the theme's personal glyph
 * ([personalGlyphRes]; the house for Unity). The current profile's glyph is
 * fully opaque and the others dimmed, tracking the swipe; tapping switches.
 */
class UnityRibbonIndicator(
	private val context: Context,
	private val container: LinearLayout,
	private val personalGlyphRes: Int,
	private val onSelect: (Int) -> Unit,
) : ProfileIndicator {
	private val icons = mutableListOf<ImageView>()
	private var position = 0F

	override fun bind(profiles: List<UserHandle?>, selected: Int) {
		val density = this.context.resources.displayMetrics.density
		val size = (32 * density).toInt()
		val margin = (6 * density).toInt()
		val padding = (4 * density).toInt()

		this.container.removeAllViews()
		this.icons.clear()

		for ((i, profile) in profiles.withIndex()) {
			val icon = ImageView(this.context)
			icon.setImageDrawable(this.glyphFor(profile))
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

	private fun glyphFor(profile: UserHandle?) =
		if (profile == null) {
			ContextCompat.getDrawable(this.context, this.personalGlyphRes)
		} else {
			// The OS stamps the badge appropriate to the profile's type //
			this.context.packageManager.getUserBadgedIcon(
				ContextCompat.getDrawable(this.context, R.drawable.ic_profile)!!, profile)
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

package be.robinj.distrohopper.desktop.launcher

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView

/**
 * The vertical launcher scroll viewport (left/right edges). When there are more pinned/running
 * apps than fit — i.e. only when the content actually overflows and scrolling kicks in — its
 * height is floored to a whole multiple of the current pinned-icon slot size, so a partial
 * icon can never peek past the visible run; see [LauncherIconGrid.viewportClipPx]. The
 * trailing remainder (< one slot) sits inside the launcher background. While everything fits
 * the measure is left untouched: no partial icon is possible then, and [PinnedAppsBar]'s
 * per-desktop swipe morph measures the bar to a FRACTIONAL length so an auto-sizing launcher
 * resizes smoothly — flooring that would snap the animation in whole-icon steps.
 */
class ClippingScrollView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
) : ScrollView(context, attrs) {
	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec)

		val content = this.getChildAt(0)?.measuredHeight ?: 0
		if (content <= this.measuredHeight) return

		val clipped = LauncherIconGrid.viewportClipPx(
			this.measuredHeight, LauncherIconGrid.iconHeightPx(this.context))
		if (clipped != this.measuredHeight)
			this.setMeasuredDimension(this.measuredWidth, clipped)
	}
}

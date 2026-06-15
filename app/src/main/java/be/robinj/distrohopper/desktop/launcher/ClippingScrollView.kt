package be.robinj.distrohopper.desktop.launcher

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView

/**
 * The vertical launcher scroll viewport (left/right edges). Its height is floored to a whole
 * multiple of the current pinned-icon slot size, so a partial icon can never peek past the
 * visible run when there are more pinned/running apps than fit — see
 * [LauncherIconGrid.viewportClipPx]. The trailing remainder (< one slot) sits inside the
 * launcher background. The size is unchanged when nothing needs clipping or before a slot size
 * is known.
 */
class ClippingScrollView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
) : ScrollView(context, attrs) {
	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec)

		val clipped = LauncherIconGrid.viewportClipPx(
			this.measuredHeight, LauncherIconGrid.iconHeightPx(this.context))
		if (clipped != this.measuredHeight)
			this.setMeasuredDimension(this.measuredWidth, clipped)
	}
}

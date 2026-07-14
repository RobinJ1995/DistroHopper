package be.robinj.distrohopper.desktop.launcher

import android.content.Context
import android.util.AttributeSet
import android.widget.HorizontalScrollView

/**
 * The horizontal launcher scroll viewport (top/bottom edges — the screen's shortest edge on a
 * portrait phone). When the content overflows (scrolling would kick in), its width is floored
 * to a whole multiple of the current pinned-icon slot size so a partial icon can never peek
 * past the visible run; while everything fits the measure is left untouched — see the
 * vertical counterpart [ClippingScrollView] for why (fractional morph measure).
 */
class ClippingHorizontalScrollView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
) : HorizontalScrollView(context, attrs) {
	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec)

		val content = this.getChildAt(0)?.measuredWidth ?: 0
		if (content <= this.measuredWidth) return

		val clipped = LauncherIconGrid.viewportClipPx(
			this.measuredWidth, LauncherIconGrid.iconSizePx(this.context))
		if (clipped != this.measuredWidth)
			this.setMeasuredDimension(clipped, this.measuredHeight)
	}
}

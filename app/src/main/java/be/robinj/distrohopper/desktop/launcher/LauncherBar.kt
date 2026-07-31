package be.robinj.distrohopper.desktop.launcher

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout

/**
 * The launcher bar itself (`llLauncher`): the BFB/spinner, the scrolling app run, and the
 * trailing preferences/trash buttons.
 *
 * It exists to keep a *floating* dock (a theme whose launcher wraps its contents, e.g. GNOME or
 * elementary) tight around what it actually shows. Its scrolling child floors itself to whole
 * icon slots so no partial icon peeks, but LinearLayout sizes itself from an earlier measurement
 * of that child: it measures the weighted viewport, commits its own length, and only then hands
 * out the leftover space — so once the viewport settles on a shorter whole-slot height, the
 * length the parent already committed stays reserved. That surfaced as dead space between the
 * last element and the dock's edge, making a floating dock look stretched along the screen edge.
 *
 * Re-wrapping the measured children here is what keeps "wraps its contents" true whichever
 * measure pass the viewport settles on. An EXPANDED launcher (one that fills the screen edge by
 * theme) keeps its full length: there the leftover is inherent to the edge, not something the
 * dock could shrink away.
 */
class LauncherBar @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {
	private val vertical: Boolean get() = this.orientation == VERTICAL

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec)

		// Only a wrap_content (floating) dock can give the length back; an expanded bar is
		// meant to span the edge.
		val wraps = (if (this.vertical) this.layoutParams?.height else this.layoutParams?.width) ==
			ViewGroup.LayoutParams.WRAP_CONTENT
		if (! wraps) {
			return
		}

		var length = 0
		for (i in 0 until this.childCount) {
			val child = this.getChildAt(i)
			if (child.visibility == GONE) {
				continue
			}

			val lp = child.layoutParams as MarginLayoutParams
			length += if (this.vertical) child.measuredHeight + lp.topMargin + lp.bottomMargin
				else child.measuredWidth + lp.leftMargin + lp.rightMargin
		}
		length += if (this.vertical) this.paddingTop + this.paddingBottom
			else this.paddingLeft + this.paddingRight

		// Never grow: the children were measured against what the parent offered.
		if (this.vertical && length < this.measuredHeight) {
			this.setMeasuredDimension(this.measuredWidth, length)
		} else if (! this.vertical && length < this.measuredWidth) {
			this.setMeasuredDimension(length, this.measuredHeight)
		}
	}
}

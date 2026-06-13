package be.robinj.distrohopper.desktop.dash

import android.content.Context
import android.util.AttributeSet
import android.widget.GridView

/**
 * A GridView measured at its full content height instead of scrolling
 * internally, so several of them can be stacked inside one scrollable
 * container — the per-workspace app lists in the dash.
 */
class ExpandedGridView : GridView {
	constructor(context: Context) : super(context)
	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
	constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
		: super(context, attrs, defStyleAttr)

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		// The largest AT_MOST spec there is: the grid sizes itself to its content //
		super.onMeasure(widthMeasureSpec,
			MeasureSpec.makeMeasureSpec(Int.MAX_VALUE shr 2, MeasureSpec.AT_MOST))
	}
}

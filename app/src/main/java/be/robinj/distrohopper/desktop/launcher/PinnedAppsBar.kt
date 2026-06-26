package be.robinj.distrohopper.desktop.launcher

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import be.robinj.distrohopper.home.LauncherMorph

/**
 * The launcher's pinned-apps bar. In normal use it is an ordinary
 * vertical/horizontal `LinearLayout` (drag-to-reorder, running indicators, …),
 * but during a per-desktop swipe morph it takes over its own measure/layout:
 * it positions each icon at a fractional slot along its axis and measures to a
 * fractional length. That lets the icons slide between two desktops' layouts
 * and an auto-sizing launcher (e.g. GNOME's floating bar) grow/shrink smoothly
 * with them, and — because the morph keeps the same child views and never adds
 * or removes them mid-swipe — without the LayoutTransition "appear" flash a
 * rebuild would cause.
 */
class PinnedAppsBar @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {
	private var morphSlots: List<LauncherMorph.Slot<*>>? = null
	private var morphStride = 0F
	private var morphLengthSlots = 0F

	val isMorphing: Boolean get() = this.morphSlots != null

	private val vertical: Boolean get() = this.orientation == VERTICAL

	/**
	 * Enters/updates morph mode. [slots] map by index to the bar's children,
	 * [stride] is the per-slot advance along the axis, and [lengthSlots] the
	 * bar's overall length (in slots) — interpolate it for a smooth resize.
	 */
	fun setMorph(slots: List<LauncherMorph.Slot<*>>, stride: Float, lengthSlots: Float) {
		this.morphSlots = slots
		this.morphStride = stride
		this.morphLengthSlots = lengthSlots

		for (i in 0 until this.childCount) {
			val child = this.getChildAt(i)
			val slot = slots.getOrNull(i) ?: continue
			child.alpha = slot.alpha
			child.scaleX = slot.scale
			child.scaleY = slot.scale
		}

		this.requestLayout()
	}

	/** Leaves morph mode, restoring the children to a plain LinearLayout flow. */
	fun clearMorph() {
		if (this.morphSlots == null) {
			return
		}

		this.morphSlots = null
		for (i in 0 until this.childCount) {
			val child = this.getChildAt(i)
			child.alpha = 1F
			child.scaleX = 1F
			child.scaleY = 1F
		}

		this.requestLayout()
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		if (this.morphSlots == null) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec)

			return
		}

		var cross = 0
		for (i in 0 until this.childCount) {
			val child = this.getChildAt(i)
			this.measureChild(child, widthMeasureSpec, heightMeasureSpec)
			cross = maxOf(cross, if (this.vertical) child.measuredWidth else child.measuredHeight)
		}
		val length = (this.morphLengthSlots * this.morphStride).toInt().coerceAtLeast(0)

		if (this.vertical) {
			this.setMeasuredDimension(
				View.resolveSize(cross, widthMeasureSpec),
				View.resolveSize(length, heightMeasureSpec))
		} else {
			this.setMeasuredDimension(
				View.resolveSize(length, widthMeasureSpec),
				View.resolveSize(cross, heightMeasureSpec))
		}
	}

	override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
		val slots = this.morphSlots
		if (slots == null) {
			super.onLayout(changed, l, t, r, b)

			return
		}

		for (i in 0 until this.childCount) {
			val child = this.getChildAt(i)
			val slot = slots.getOrNull(i) ?: continue
			val pos = (slot.position * this.morphStride).toInt()

			if (this.vertical) {
				child.layout(0, pos, child.measuredWidth, pos + child.measuredHeight)
			} else {
				child.layout(pos, 0, pos + child.measuredWidth, child.measuredHeight)
			}
		}
	}
}

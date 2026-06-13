package be.robinj.distrohopper.desktop.dash.profile

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

/**
 * Approximates the GNOME Shell profile indicator: one shape per profile, the
 * current one drawn as an elongated capsule and the rest as dimmed dots. The
 * shapes are laid out left to right with a single dot's worth of space between
 * them, so neighbours slide over as the current shape elongates or contracts.
 * Each shape morphs between dot and capsule (and fades between dim and full
 * opacity) based on how close [position] is to it, which gives a smooth slide
 * as the dash pager is swiped — [position] is fractional during a swipe.
 */
class ProfilePillView(context: Context) : View(context) {
	var count: Int = 0
		set(value) {
			field = value
			this.requestLayout()
			this.invalidate()
		}

	/** Current page, fractional while swiping. */
	var position: Float = 0F
		set(value) {
			field = value
			this.invalidate()
		}

	var onSlotClick: ((Int) -> Unit)? = null

	private val density = this.resources.displayMetrics.density
	private val dotSize = 7F * this.density
	private val capsuleWidth = 22F * this.density
	private val shapeHeight = 7F * this.density
	/** A single dot's worth of space between adjacent indicators. */
	private val gap = this.dotSize
	/** Opacity of an inactive (dot) indicator; fades up to 1 as it elongates. */
	private val dimAlpha = 0.4F

	private val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
	private val rect = RectF()

	/** How elongated slot [i] is, 1 when fully selected and 0 when a plain dot. */
	private fun elongationFor(i: Int): Float =
		1F - Math.min(Math.abs(this.position - i), 1F)

	private fun shapeWidthFor(i: Int): Float =
		this.dotSize + (this.capsuleWidth - this.dotSize) * this.elongationFor(i)

	private fun contentWidth(): Float {
		if (this.count <= 0) {
			return 0F
		}

		var width = (this.count - 1) * this.gap
		for (i in 0 until this.count) {
			width += this.shapeWidthFor(i)
		}

		return width
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		// Exactly one shape is elongated at any time (the elongations of two
		// adjacent shapes always sum to 1 mid-swipe), so the total width is
		// constant and we can measure it precisely without any reflow.
		val width = if (this.count <= 0) 0F else
			this.count * this.dotSize + (this.capsuleWidth - this.dotSize) +
				(this.count - 1) * this.gap
		val desiredHeight = this.shapeHeight.toInt()

		this.setMeasuredDimension(
			resolveSize(Math.ceil(width.toDouble()).toInt(), widthMeasureSpec),
			resolveSize(desiredHeight, heightMeasureSpec))
	}

	override fun onDraw(canvas: Canvas) {
		if (this.count <= 0) {
			return
		}

		val centerY = this.height / 2F
		val radius = this.shapeHeight / 2F
		var x = (this.width - this.contentWidth()) / 2F

		for (i in 0 until this.count) {
			val elongation = this.elongationFor(i)
			val shapeWidth = this.dotSize + (this.capsuleWidth - this.dotSize) * elongation

			val alpha = this.dimAlpha + (1F - this.dimAlpha) * elongation
			this.shapePaint.alpha = Math.round(alpha * 255F)

			this.rect.set(x, centerY - radius, x + shapeWidth, centerY + radius)
			canvas.drawRoundRect(this.rect, radius, radius, this.shapePaint)

			x += shapeWidth + this.gap
		}
	}

	override fun onTouchEvent(event: MotionEvent): Boolean {
		if (event.action == MotionEvent.ACTION_UP && this.count > 0) {
			var x = (this.width - this.contentWidth()) / 2F

			for (i in 0 until this.count) {
				val shapeWidth = this.shapeWidthFor(i)
				// Hit-test the slot including the trailing gap so taps between
				// indicators still resolve to the nearest profile.
				if (event.x <= x + shapeWidth + this.gap / 2F) {
					this.onSlotClick?.invoke(i)

					return true
				}

				x += shapeWidth + this.gap
			}

			// A tap past the last indicator falls through to it.
			this.onSlotClick?.invoke(this.count - 1)

			return true
		}

		return super.onTouchEvent(event)
	}
}

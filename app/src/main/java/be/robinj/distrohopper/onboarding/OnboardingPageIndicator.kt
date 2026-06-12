package be.robinj.distrohopper.onboarding

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Dot page indicator for the wizard's pager: the active dot stretches into a
 * pill, animating smoothly between pages as [setPosition] is fed scroll
 * offsets from the pager.
 */
class OnboardingPageIndicator @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
) : View(context, attrs) {
	var count: Int = 0
		set(value) {
			field = value
			this.requestLayout()
		}

	private var position: Int = 0
	private var offset: Float = 0f

	private val density = this.resources.displayMetrics.density
	private val dotRadius = 4f * this.density
	private val gap = 8f * this.density
	private val pillExtraWidth = 16f * this.density

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

	fun setPosition(position: Int, offset: Float) {
		this.position = position
		this.offset = offset
		this.invalidate()
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val width = this.paddingLeft + this.paddingRight +
			(this.count * 2f * this.dotRadius + (this.count - 1) * this.gap + this.pillExtraWidth).toInt()
		val height = this.paddingTop + this.paddingBottom + (2f * this.dotRadius).toInt()

		this.setMeasuredDimension(
			resolveSize(width, widthMeasureSpec),
			resolveSize(height, heightMeasureSpec),
		)
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)

		val cy = this.paddingTop + this.dotRadius
		var x = this.paddingLeft.toFloat()

		for (i in 0 until this.count) {
			val active = this.activeFraction(i)
			val width = 2f * this.dotRadius + this.pillExtraWidth * active
			this.paint.color = 0xFFFFFF or (((0x66 + (0xFF - 0x66) * active).toInt()) shl 24)

			canvas.drawRoundRect(
				x, cy - this.dotRadius, x + width, cy + this.dotRadius,
				this.dotRadius, this.dotRadius, this.paint,
			)

			x += width + this.gap
		}
	}

	/** How "selected" dot [i] currently is, in [0, 1]; mid-swipe it is split between two dots. */
	private fun activeFraction(i: Int): Float = when (i) {
		this.position -> 1f - this.offset
		this.position + 1 -> this.offset
		else -> 0f
	}
}

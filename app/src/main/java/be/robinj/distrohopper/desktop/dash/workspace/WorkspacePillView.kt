package be.robinj.distrohopper.desktop.dash.workspace

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

/**
 * Approximates the GNOME Shell workspace pill: a dark rounded container with
 * one shape per workspace, the current one drawn as an elongated capsule and
 * the rest as dots. Each shape sits in a fixed-width cell (so the layout never
 * reflows) and morphs between dot and capsule based on how close [position] is
 * to it, which gives a smooth slide as the dash pager is swiped — [position]
 * is fractional during a swipe.
 */
class WorkspacePillView(context: Context) : View(context) {
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
	private val cellWidth = this.capsuleWidth
	private val cellGap = 7F * this.density
	private val padH = 9F * this.density
	private val padV = 5F * this.density
	private val backgroundRadius = 9F * this.density

	private val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
	private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.argb(0x59, 0, 0, 0)
	}
	private val rect = RectF()

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val width = if (this.count <= 0) 0F else
			this.padH * 2 + this.count * this.cellWidth + (this.count - 1) * this.cellGap
		val desiredHeight = (this.shapeHeight + this.padV * 2).toInt()

		this.setMeasuredDimension(
			resolveSize(Math.ceil(width.toDouble()).toInt(), widthMeasureSpec),
			resolveSize(desiredHeight, heightMeasureSpec))
	}

	override fun onDraw(canvas: Canvas) {
		if (this.count <= 0) {
			return
		}

		val contentWidth =
			this.padH * 2 + this.count * this.cellWidth + (this.count - 1) * this.cellGap
		val left = (this.width - contentWidth) / 2F
		val centerY = this.height / 2F

		this.rect.set(left, centerY - this.backgroundRadius - this.padV / 2F,
			left + contentWidth, centerY + this.backgroundRadius + this.padV / 2F)
		canvas.drawRoundRect(this.rect, this.backgroundRadius, this.backgroundRadius,
			this.backgroundPaint)

		for (i in 0 until this.count) {
			val cellLeft = left + this.padH + i * (this.cellWidth + this.cellGap)
			val cellCenterX = cellLeft + this.cellWidth / 2F

			val elongation = 1F - Math.min(Math.abs(this.position - i), 1F)
			val shapeWidth = this.dotSize + (this.capsuleWidth - this.dotSize) * elongation
			val radius = this.shapeHeight / 2F

			this.rect.set(cellCenterX - shapeWidth / 2F, centerY - radius,
				cellCenterX + shapeWidth / 2F, centerY + radius)
			canvas.drawRoundRect(this.rect, radius, radius, this.shapePaint)
		}
	}

	override fun onTouchEvent(event: MotionEvent): Boolean {
		if (event.action == MotionEvent.ACTION_UP && this.count > 0) {
			val contentWidth =
				this.padH * 2 + this.count * this.cellWidth + (this.count - 1) * this.cellGap
			val left = (this.width - contentWidth) / 2F + this.padH
			val slot = ((event.x - left) / (this.cellWidth + this.cellGap)).toInt()

			if (slot in 0 until this.count) {
				this.onSlotClick?.invoke(slot)

				return true
			}
		}

		return super.onTouchEvent(event)
	}
}

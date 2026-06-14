package be.robinj.distrohopper.folder

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import kotlin.math.min

/**
 * A folder's icon: a miniature of its contents grid. The member drawables (app
 * icons, or widget placeholders) are drawn into the adaptive [FolderGrid] layout
 * on a rounded translucent background, so a folder reads as a "grid of icons"
 * the way the popover lays them out (see the spec's 1..9 mapping).
 */
class FolderIconDrawable(
	private val members: List<Drawable>,
) : Drawable() {
	private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.argb(60, 255, 255, 255)
	}

	override fun draw(canvas: Canvas) {
		val bounds = this.bounds
		if (bounds.isEmpty || this.members.isEmpty()) {
			return
		}

		val radius = bounds.width() * BACKGROUND_RADIUS_FRACTION
		canvas.drawRoundRect(RectF(bounds), radius, radius, this.backgroundPaint)

		val shown = this.members.take(FolderGrid.MAX_CELLS)
		val columns = FolderGrid.columns(shown.size)
		val rows = FolderGrid.rows(shown.size)
		if (columns <= 0 || rows <= 0) {
			return
		}

		val padding = (bounds.width() * PADDING_FRACTION).toInt()
		val gridWidth = bounds.width() - padding * 2
		val gridHeight = bounds.height() - padding * 2
		// Square cells sized to the tighter axis so the mini-grid stays centred.
		val cell = min(gridWidth / columns, gridHeight / rows)
		val iconSize = (cell * ICON_FRACTION).toInt().coerceAtLeast(1)
		val iconGap = (cell - iconSize) / 2

		val gridLeft = bounds.left + (bounds.width() - cell * columns) / 2
		val gridTop = bounds.top + (bounds.height() - cell * rows) / 2

		shown.forEachIndexed { index, drawable ->
			val col = index % columns
			val row = index / columns
			val left = gridLeft + col * cell + iconGap
			val top = gridTop + row * cell + iconGap
			drawable.setBounds(left, top, left + iconSize, top + iconSize)
			drawable.draw(canvas)
		}
	}

	override fun setAlpha(alpha: Int) {
		this.backgroundPaint.alpha = alpha
	}

	override fun setColorFilter(colorFilter: ColorFilter?) {
		// Member drawables keep their own colours; nothing to filter on the frame.
	}

	@Deprecated("Deprecated in Drawable", ReplaceWith("PixelFormat.TRANSLUCENT"))
	override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

	companion object {
		private const val BACKGROUND_RADIUS_FRACTION = 0.18f
		private const val PADDING_FRACTION = 0.10f
		private const val ICON_FRACTION = 0.82f
	}
}

package be.robinj.distrohopper.desktop

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.view.Window
import be.robinj.distrohopper.R

/**
 * Cross-window blur is optional on Android. When an OEM disables it (notably Samsung), a
 * translucent tint plus fine grain reduces background detail showing through a semi-transparent
 * surface, without needing the wallpaper bitmap. The dash uses this full-bleed behind its
 * content; pop-up dialogs use it (with a [cornerRadius]) in place of their blurred card.
 *
 * [fraction] scales both layers so a ValueAnimator can ramp the effect in or out.
 */
internal class FrostedFallbackDrawable(
    private val tint: Int,
    private val cornerRadius: Float = 0F,
) : Drawable() {
    private val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tint }
    private val grainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = BitmapShader(GRAIN, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    }

    var fraction: Float = 0F
        set(value) {
            field = value
            this.invalidateSelf()
        }

    override fun draw(canvas: Canvas) {
        this.tintPaint.alpha = (Color.alpha(this.tint) * this.fraction).toInt()
        this.fill(canvas, this.tintPaint)

        this.grainPaint.alpha = (18 * this.fraction).toInt()
        this.fill(canvas, this.grainPaint)
    }

    private fun fill(canvas: Canvas, paint: Paint) {
        if (this.cornerRadius > 0F) {
            canvas.drawRoundRect(RectF(this.bounds), this.cornerRadius, this.cornerRadius, paint)
        } else {
            canvas.drawRect(this.bounds, paint)
        }
    }

    override fun setAlpha(alpha: Int) {
        this.fraction = alpha / 255F
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        this.tintPaint.colorFilter = colorFilter
        this.grainPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Android")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        // The grain is identical everywhere it's used, so it's generated once for the process.
        private val GRAIN: Bitmap by lazy { createGrain() }

        private fun createGrain(): Bitmap {
            val size = 48
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            var state = 0x4d595df4

            for (y in 0 until size) {
                for (x in 0 until size) {
                    state = state * 1664525 + 1013904223
                    val white = if ((state ushr 28) >= 8) 255 else 0
                    bitmap.setPixel(x, y, Color.argb(255, white, white, white))
                }
            }

            return bitmap
        }
    }
}

/**
 * Applies the frosted [FrostedFallbackDrawable] to pop-up dialogs that rely on cross-window
 * blur (via [be.robinj.distrohopper.R.style] `ModernDialogTheme`) wherever that blur is
 * unavailable, keeping their text legible over a busy wallpaper.
 */
object FrostedGlass {
    /** The same capability the dash checks; injectable so tests can drive both branches. */
    internal var crossWindowBlurEnabled: (Window) -> Boolean = {
        it.windowManager.isCrossWindowBlurEnabled
    }

    /**
     * When blur is unavailable, replace the dialog's window background (the rounded
     * `ModernDialogTheme` card) with the grain drawable, and return it. The surface colour and
     * corner radius match the theme's card, so blur-capable and blur-less devices look identical
     * apart from the added grain. A no-op returning null when blur works.
     */
    fun applyDialogFallback(window: Window): Drawable? {
        if (this.crossWindowBlurEnabled(window)) {
            return null
        }

        val context = window.context
        val tint = context.getColor(R.color.dialog_surface)
        val radius = context.resources.getDimension(R.dimen.dialog_corner_radius)

        val fallback = FrostedFallbackDrawable(tint, radius).apply { fraction = 1F }
        window.setBackgroundDrawable(fallback)

        return fallback
    }
}

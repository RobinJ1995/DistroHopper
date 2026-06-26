package be.robinj.distrohopper.widget.bfb

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import be.robinj.distrohopper.home.LauncherTileColour
import be.robinj.distrohopper.theme.Theme
import kotlin.math.roundToInt

/**
 * Renders the BFB widget's bitmap: the launcher's menu button (BFB) inside the
 * same themed tile it has in the launcher bar, fully driven by the active theme.
 *
 * Mirrors [be.robinj.distrohopper.desktop.launcher.AppLauncher]'s tile: the
 * themed `launcher_applauncher_background` shape filled with the resolved tile
 * colour (static themed colour, or the chameleonic wallpaper colour — see
 * [LauncherTileColour]), the gloss gradient overlay, then the BFB image. Stateless
 * and prefs-driven so every render reflects the current theme/wallpaper.
 */
object BfbWidgetRenderer {
	/** Largest bitmap edge; keeps the RemoteViews well under the ~1MB Binder limit. */
	const val MAX_DIMENSION_PX = 320

	/** Fraction of the tile left as padding around the BFB image (≈ the 4dp icon margin). */
	private const val ICON_PADDING_FRACTION = 0.06f

	fun render(context: Context, theme: Theme, widthPx: Int, heightPx: Int): Bitmap {
		val size = minOf(widthPx, heightPx)
			.coerceAtMost(MAX_DIMENSION_PX)
			.coerceAtLeast(1)

		val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
		val canvas = Canvas(bitmap)

		val colour = LauncherTileColour.resolve(context, theme)

		// Background tile: themed shape, tinted with the resolved tile colour. Only
		// a GradientDrawable (every current theme's shape) takes the colour; any
		// other drawable is drawn as-is rather than risk a bad cast.
		context.getDrawable(theme.launcher_applauncher_background)?.let { background ->
			val tile = background.mutate()
			if (tile is GradientDrawable)
				tile.setColor(colour)
			this.drawFilling(tile, canvas, size)
		}

		// Gloss gradient overlay, then the BFB image with a small proportional inset.
		context.getDrawable(theme.launcher_applauncher_gradient)?.let {
			this.drawFilling(it.mutate(), canvas, size)
		}

		context.getDrawable(theme.launcher_bfb_image)?.let { bfb ->
			val padding = (size * ICON_PADDING_FRACTION).roundToInt()
			bfb.setBounds(padding, padding, size - padding, size - padding)
			bfb.draw(canvas)
		}

		return bitmap
	}

	private fun drawFilling(drawable: Drawable, canvas: Canvas, size: Int) {
		drawable.setBounds(0, 0, size, size)
		drawable.draw(canvas)
	}
}

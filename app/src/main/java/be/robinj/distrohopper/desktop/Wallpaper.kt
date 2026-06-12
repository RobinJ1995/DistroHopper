package be.robinj.distrohopper.desktop

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Window
import android.widget.ImageView
import androidx.core.graphics.ColorUtils
import be.robinj.distrohopper.ExceptionHandler
import be.robinj.distrohopper.R
import be.robinj.distrohopper.dev.Log

/**
 * Created by robin on 8/21/14.
 */
class Wallpaper : ImageView {
    private val context: Context

    var isLiveWallpaper: Boolean = false
        internal set

    constructor(context: Context) : super(context) {
        this.context = context
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        this.context = context
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        this.context = context
    }

    fun init() {
        val wpman = WallpaperManager.getInstance(this.context)

        val info = wpman.wallpaperInfo
        this.isLiveWallpaper = (info != null
                && !info.packageName
            .startsWith("net.oneplus.launcher")) // OnePlus 5T always seems to use a live wallpaper, presumably for the blur animation when opening OnePlus' "shelf"
    }

    fun set() {
        this.setImageDrawable(null)
        this.setBackgroundColor(this.resources.getColor(R.color.transparent))
    }

    /*
     * The system wallpaper lives in a separate window behind the (transparent) activity, so
     * blurring it is only possible with cross-window blur. That can be unavailable at runtime
     * (battery saver, device config), in which case this view darkens it instead.
     */
    fun blur(window: Window, radiusPx: Int) {
        if (window.windowManager.isCrossWindowBlurEnabled) {
            window.setBackgroundBlurRadius(radiusPx)
            this.setBackgroundColor(this.resources.getColor(R.color.transparent))
        } else {
            window.setBackgroundBlurRadius(0)
            this.setBackgroundColor(this.resources.getColor(R.color.transparentblack60))
        }
    }

    fun unblur(window: Window) {
        window.setBackgroundBlurRadius(0)
        this.setBackgroundColor(this.resources.getColor(R.color.transparent))
    }

    /**
     * Animated counterpart of [blur]/[unblur]: applies the given fraction of the full blur
     * radius, so a ValueAnimator can ramp the blur up or down. When cross-window blur is
     * unavailable the fallback darkening is ramped instead, by scaling its alpha.
     */
    fun applyBlurFraction(window: Window, fraction: Float, maxRadiusPx: Int) {
        if (window.windowManager.isCrossWindowBlurEnabled) {
            window.setBackgroundBlurRadius((fraction * maxRadiusPx).toInt())
            this.setBackgroundColor(this.resources.getColor(R.color.transparent))
        } else {
            window.setBackgroundBlurRadius(0)
            val darken = this.resources.getColor(R.color.transparentblack60)
            this.setBackgroundColor(ColorUtils.setAlphaComponent(
                darken, (Color.alpha(darken) * fraction).toInt()))
        }
    }

    /*
     * WallpaperColors needs no permission, unlike reading the wallpaper bitmap (which stopped
     * being possible on Android 13 altogether). It can still be null, e.g. right after boot.
     */
    fun getAverageColour(alpha: Int): Int {
        try {
            val primaryColour = WallpaperManager.getInstance(this.context)
                .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.primaryColor

            if (primaryColour != null) {
                return Color.argb(
                    alpha.toFloat(),
                    primaryColour.red(),
                    primaryColour.green(),
                    primaryColour.blue()
                )
            }
        } catch (ex: Exception) {
            ExceptionHandler(ex).logAndTrack()
        }

        LOG.v("Wallpaper", "Falling back to \"Ubuntu orange\" as dominant colour.")
        return COLOUR_UBUNTU_ORANGE
    }

    companion object {
        private val COLOUR_UBUNTU_ORANGE = Color.rgb(180, 60, 18)
        private val LOG: Log = Log.getInstance()
    }
}

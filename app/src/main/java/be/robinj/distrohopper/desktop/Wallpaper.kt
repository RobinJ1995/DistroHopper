package be.robinj.distrohopper.desktop

import android.Manifest
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Window
import android.widget.ImageView
import androidx.core.graphics.ColorUtils
import be.robinj.distrohopper.ExceptionHandler
import be.robinj.distrohopper.Image
import be.robinj.distrohopper.Permission
import be.robinj.distrohopper.R
import be.robinj.distrohopper.dev.Log

/**
 * Created by robin on 8/21/14.
 */
class Wallpaper : ImageView {
    private val context: Context
    private var img: Drawable? = null

    var isLiveWallpaper: Boolean = false
        private set

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
        val permissionExternalStorage =
            Permission(this.context, Manifest.permission.READ_EXTERNAL_STORAGE)

        if (permissionExternalStorage.check()) {
            LOG.i(
                "Wallpaper",
                "READ_EXTERNAL_STORAGE permission granted. Trying to obtain wallpaper..."
            )
            try {
                /*
				 * This will never succeed on Android 13, as Google in their wisdom deprecated the
				 * READ_EXTERNAL_STORAGE permission, but still requires it to get obtain the user
				 * wallpaper.
				 * Very useful for home screen replacements like these.
				 */
                this.img = wpman.getDrawable()
            } catch (ex: Exception) // Only needed for the average colour; not worth crashing over //
            {
                this.img = null

                ExceptionHandler(ex).logAndTrack()
            }
        } else {
            LOG.i(
                "Wallpaper",
                "READ_EXTERNAL_STORAGE permission not granted or Android version >= 13."
            )
            this.img = null
        }

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

    fun getAverageColour(alpha: Int): Int {
        try {
            if (this.img != null) {
                LOG.v("Wallpaper", "Calculating dominant colour of wallpaper...")
                val image = Image(this.img)

                return image.getAverageColour(alpha)
            } else {
                LOG.v("Wallpaper", "Trying to obtain primary wallpaper colour from Android...")
                val wpman = WallpaperManager.getInstance(this.context)
                val primaryColour =
                    wpman.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)!!.primaryColor

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

        /*
		 * What with Google crippling the APIs faster than they can invent new methods of achieving
		 * similar results, this will have to do.
		 */
        LOG.v("Wallpaper", "Falling back to \"Ubuntu orange\" as dominant colour.")
        return COLOUR_UBUNTU_ORANGE
    }

    companion object {
        private val COLOUR_UBUNTU_ORANGE = Color.rgb(180, 60, 18)
        private val LOG: Log = Log.getInstance()
    }
}

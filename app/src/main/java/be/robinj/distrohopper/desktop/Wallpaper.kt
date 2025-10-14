package be.robinj.distrohopper.desktop

import android.Manifest
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.widget.ImageView
import be.robinj.distrohopper.ExceptionHandler
import be.robinj.distrohopper.Image
import be.robinj.distrohopper.Permission
import be.robinj.distrohopper.R
import be.robinj.distrohopper.dev.Log
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import androidx.core.graphics.scale

/**
 * Created by robin on 8/21/14.
 */
class Wallpaper : ImageView {
    private val context: Context
    private var img: Drawable? = null
    private var blurred: Drawable? = null
    private var mode: String? = null

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
                "READ_EXTERNAL_STORAGE permission granted. Trying to obtain and blur wallpaper..."
            )
            try {
                /*
				 * This will never succeed on Android 13, as Google in their wisdom deprecated the
				 * READ_EXTERNAL_STORAGE permission, but still requires it to get obtain the user
				 * wallpaper.
				 * Very useful for home screen replacements like these.
				 */
                this.img = wpman.getDrawable()

                //TODO// Huge memory hog! Need to get rid of this. //
                val prefs = Preferences.getSharedPreferences(this.context, Preferences.PREFERENCES)
                this.mode = prefs.getString(Preference.WALLPAPER_BLUR_MODE.getName(), "darken")

                if (mode == "scale") {
                    val blurred = wpman.getDrawable()

                    var bmdBlurred = blurred as BitmapDrawable?
                    var bmBlurred = bmdBlurred!!.bitmap

                    val origWidth = bmBlurred.width.toFloat()
                    val origHeight = bmBlurred.height.toFloat()

                    val width = 200
                    val height = (origHeight * (200f / origWidth)).toInt()

                    bmBlurred = bmBlurred.scale(width, height)
                        .scale(origWidth.toInt(), origHeight.toInt())

                    bmdBlurred = BitmapDrawable(bmBlurred)
                    this.blurred = bmdBlurred
                }
            } catch (ex: OutOfMemoryError) // I'd prefer the image not being blurred over the app crashing //
            {
                this.img = null
                this.blurred = null

                ExceptionHandler(ex).logAndTrack()
            }
        } else {
            LOG.i(
                "Wallpaper",
                "READ_EXTERNAL_STORAGE permission not granted or Android version >= 13."
            )
            this.img = null
            this.blurred = null
        }

        val info = wpman.wallpaperInfo
        this.isLiveWallpaper = (info != null
                && !info.packageName
            .startsWith("net.oneplus.launcher")) // OnePlus 5T always seems to use a live wallpaper, presumably for the blur animation when opening OnePlus' "shelf"
    }

    fun set() {
        if (this.img == null && this.blurred == null) {
            return
        }

        if (this.mode != "no") {
            if (this.isLiveWallpaper || this.blurred == null || this.mode == "darken") {
                this.setImageDrawable(null)
                this.setBackgroundColor(this.resources.getColor(R.color.transparent))
            } else {
                this.setImageDrawable(this.img)
                this.setBackgroundDrawable(null) // setBackgroundDrawable is deprecated, but setBackground is unspported on older versions of Android //
            }
        }
    }

    fun blur() {
        if (this.img == null && this.blurred == null) {
            return
        }

        if (this.mode != "no") {
            if (this.isLiveWallpaper || this.blurred == null || this.mode == "darken") {
                this.setImageDrawable(null)
                this.setBackgroundColor(this.resources.getColor(R.color.transparentblack60))
            } else {
                this.setImageDrawable(this.blurred)
                this.setBackgroundDrawable(null) // setBackgroundDrawable is deprecated, but setBackground is unspported on older versions of Android //
            }
        }
    }

    fun unblur() {
        if (this.img == null && this.blurred == null) {
            return
        }

        if (this.mode != "no") {
            if (this.isLiveWallpaper || this.blurred == null || this.mode == "darken") {
                this.setImageDrawable(null)
                this.setBackgroundColor(this.resources.getColor(R.color.transparent))
            } else {
                this.setImageDrawable(this.img)
                this.setBackgroundDrawable(null) // setBackgroundDrawable is deprecated, but setBackground is unspported on older versions of Android //
            }
        }
    }

    fun getAverageColour(alpha: Int): Int {
        try {
            if (this.img != null) {
                LOG.v("Wallpaper", "Calculating dominant colour of wallpaper...")
                val image = Image(this.img)

                return image.getAverageColour(alpha)
            } else if (Build.VERSION.SDK_INT >= 27) {
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

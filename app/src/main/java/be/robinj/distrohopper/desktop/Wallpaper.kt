package be.robinj.distrohopper.desktop;

import android.Manifest;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageView;

import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.Image;
import be.robinj.distrohopper.Permission;
import be.robinj.distrohopper.R;
import be.robinj.distrohopper.dev.Log;
import be.robinj.distrohopper.preferences.Preference;
import be.robinj.distrohopper.preferences.Preferences;

/**
 * Created by robin on 8/21/14.
 */
public class Wallpaper extends ImageView {
	private static final int COLOUR_UBUNTU_ORANGE = Color.rgb(180, 60 ,18);
	private static final Log LOG = Log.getInstance();

	private Context context;
	@Nullable private Drawable img;
	@Nullable private Drawable blurred;
	private String mode;

	private boolean liveWallpaper = false;

	public Wallpaper (Context context)
	{
		super (context);

		this.context = context;
	}

	public Wallpaper (Context context, AttributeSet attrs)
	{
		super (context, attrs);

		this.context = context;
	}

	public Wallpaper (Context context, AttributeSet attrs, int defStyle)
	{
		super (context, attrs, defStyle);

		this.context = context;
	}

	public void init ()
	{
		final WallpaperManager wpman = WallpaperManager.getInstance(this.context);
		final Permission permissionExternalStorage = new Permission(this.context, Manifest.permission.READ_EXTERNAL_STORAGE);

		if (permissionExternalStorage.check()) {
			LOG.i("Wallpaper", "READ_EXTERNAL_STORAGE permission granted. Trying to obtain and blur wallpaper...");
			try
			{
				/*
				 * This will never succeed on Android 13, as Google in their wisdom deprecated the
				 * READ_EXTERNAL_STORAGE permission, but still requires it to get obtain the user
				 * wallpaper.
				 * Very useful for home screen replacements like these.
				 */
				this.img = wpman.getDrawable();

				//TODO// Huge memory hog! Need to get rid of this. //
				SharedPreferences prefs = Preferences.getSharedPreferences(this.context, Preferences.PREFERENCES);
				this.mode = prefs.getString (Preference.WALLPAPER_BLUR_MODE.getName(), "darken");

				if (mode.equals ("scale"))
				{
					Drawable blurred = wpman.getDrawable ();

					BitmapDrawable bmdBlurred = (BitmapDrawable) blurred;
					Bitmap bmBlurred = bmdBlurred.getBitmap ();

					float origWidth = bmBlurred.getWidth ();
					float origHeight = bmBlurred.getHeight ();

					int width = 200;
					int height = (int) (origHeight * (200F / origWidth));

					bmBlurred = Bitmap.createScaledBitmap (bmBlurred, width, height, true);
					bmBlurred = Bitmap.createScaledBitmap (bmBlurred, (int) origWidth, (int) origHeight, true);

					bmdBlurred = new BitmapDrawable (bmBlurred);
					this.blurred = bmdBlurred;
				}
			}
			catch (OutOfMemoryError ex) // I'd prefer the image not being blurred over the app crashing //
			{
				this.img = null;
				this.blurred = null;

				new ExceptionHandler(ex).logAndTrack();
			}
		} else {
			LOG.i("Wallpaper", "READ_EXTERNAL_STORAGE permission not granted or Android version >= 13.");
			this.img = null;
			this.blurred = null;
		}

		final WallpaperInfo info = wpman.getWallpaperInfo ();
		this.liveWallpaper = (info != null
				&& !info.getPackageName().startsWith("net.oneplus.launcher")); // OnePlus 5T always seems to use a live wallpaper, presumably for the blur animation when opening OnePlus' "shelf"
	}

	public void set ()
	{
		if (this.img == null && this.blurred == null) {
			return;
		}

		if (! this.mode.equals ("no"))
		{
			if (this.liveWallpaper || this.blurred == null || this.mode.equals ("darken"))
			{
				this.setImageDrawable (null);
				this.setBackgroundColor (this.getResources ().getColor (R.color.transparent));
			}
			else
			{
				this.setImageDrawable (this.img);
				this.setBackgroundDrawable (null); // setBackgroundDrawable is deprecated, but setBackground is unspported on older versions of Android //
			}
		}
	}

	public void blur ()
	{
		if (this.img == null && this.blurred == null) {
			return;
		}

		if (! this.mode.equals ("no"))
		{
			if (this.liveWallpaper || this.blurred == null || this.mode.equals ("darken"))
			{
				this.setImageDrawable (null);
				this.setBackgroundColor (this.getResources ().getColor (R.color.transparentblack60));
			}
			else
			{
				this.setImageDrawable (this.blurred);
				this.setBackgroundDrawable (null); // setBackgroundDrawable is deprecated, but setBackground is unspported on older versions of Android //
			}
		}
	}

	public void unblur ()
	{
		if (this.img == null && this.blurred == null) {
			return;
		}

		if (! this.mode.equals ("no"))
		{
			if (this.liveWallpaper || this.blurred == null || this.mode.equals ("darken"))
			{
				this.setImageDrawable (null);
				this.setBackgroundColor (this.getResources ().getColor (R.color.transparent));
			}
			else
			{
				this.setImageDrawable (this.img);
				this.setBackgroundDrawable (null); // setBackgroundDrawable is deprecated, but setBackground is unspported on older versions of Android //
			}
		}
	}

	public int getAverageColour(final int alpha)
	{
		try {
			if (this.img != null) {
				LOG.v("Wallpaper", "Calculating dominant colour of wallpaper...");
				final Image image = new Image(this.img);

				return image.getAverageColour(alpha);
			} else if (Build.VERSION.SDK_INT >= 27) {
				LOG.v("Wallpaper", "Trying to obtain primary wallpaper colour from Android...");
				final WallpaperManager wpman = WallpaperManager.getInstance(this.context);
				final Color primaryColour = wpman.getWallpaperColors(WallpaperManager.FLAG_SYSTEM).getPrimaryColor();

				return Color.argb(alpha, primaryColour.red(), primaryColour.green(), primaryColour.blue());
			}
		} catch (final Exception ex) {
			new ExceptionHandler(ex).logAndTrack();
		}

		/*
		 * What with Google crippling the APIs faster than they can invent new methods of achieving
		 * similar results, this will have to do.
		 */
		LOG.v("Wallpaper", "Falling back to \"Ubuntu orange\" as dominant colour.");
		return COLOUR_UBUNTU_ORANGE;
	}

	public boolean isLiveWallpaper ()
	{
		return this.liveWallpaper;
	}
}

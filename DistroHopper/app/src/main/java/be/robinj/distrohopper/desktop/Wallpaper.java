package be.robinj.distrohopper.desktop;

import android.Manifest;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.ImageView;

import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.Image;
import be.robinj.distrohopper.Permission;
import be.robinj.distrohopper.R;
import be.robinj.distrohopper.preferences.Preference;
import be.robinj.distrohopper.preferences.Preferences;

/**
 * Created by robin on 8/21/14.
 */
public class Wallpaper extends ImageView
{
	private Context context;
	private Drawable img;
	private Drawable blurred;
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
			this.img = wpman.getDrawable();
		} else {
			this.img = this.getResources().getDrawable(R.drawable.wallpaper);
		}

		try
		{
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
			this.blurred = null;

			new ExceptionHandler(ex).logAndTrack();
		}

		WallpaperInfo info = wpman.getWallpaperInfo ();
		this.liveWallpaper = (info != null
				&& !info.getPackageName().startsWith("net.oneplus.launcher")); // OnePlus 5T always seems to use a live wallpaper, presumably for the blur animation when opening OnePlus' "shelf"
	}

	public void set ()
	{
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
		final Image image = new Image(this.img);

		return image.getAverageColour(alpha);
	}

	public boolean isLiveWallpaper ()
	{
		return this.liveWallpaper;
	}
}

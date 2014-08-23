package be.robinj.ubuntu.unity;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import be.robinj.ubuntu.Image;

/**
 * Created by robin on 8/21/14.
 */
public class Wallpaper extends ImageView
{
	private Context context;
	private Drawable img;
	private Drawable blurred;

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
		WallpaperManager wpman = WallpaperManager.getInstance (this.context);
		this.img = wpman.getDrawable ();
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

	public void set ()
	{
		this.setImageDrawable (this.img);
	}

	public void blur ()
	{
		this.setImageDrawable (this.blurred);
	}

	public void unblur ()
	{
		this.setImageDrawable (this.img);
	}

	public int getAverageColour (int alpha)
	{
		Image image = new Image (this.img);

		return image.getAverageColour (true, true, alpha);
	}
}

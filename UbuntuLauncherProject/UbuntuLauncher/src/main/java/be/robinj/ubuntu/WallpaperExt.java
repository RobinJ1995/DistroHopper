package be.robinj.ubuntu;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.Drawable;
import android.webkit.JavascriptInterface;

public class WallpaperExt extends ImageExt
{
	private WallpaperManager manager;

	public WallpaperExt (Context context)
	{
		super (context);
		
		this.manager = WallpaperManager.getInstance (context);
	}
	
	@Override
    @JavascriptInterface
	public Drawable get ()
	{
		return this.manager.getDrawable ();
	}
	
	@Override
    @JavascriptInterface
	public String getPath ()
	{
		return this.getPath ("wallpaper.jpg", 100, CompressFormat.JPEG);
	}
}

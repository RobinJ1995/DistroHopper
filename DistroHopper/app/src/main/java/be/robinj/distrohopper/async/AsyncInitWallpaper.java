package be.robinj.distrohopper.async;

import android.os.AsyncTask;

import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.HomeActivity;
import be.robinj.distrohopper.desktop.Wallpaper;

/**
 * Created by robin on 8/21/14.
 */
public class AsyncInitWallpaper extends AsyncTask<Wallpaper, Integer, Wallpaper>
{
	private HomeActivity parent;

	public AsyncInitWallpaper (HomeActivity parent)
	{
		this.parent = parent;
	}

	@Override
	protected Wallpaper doInBackground (Wallpaper... wallpaper)
	{
		Wallpaper wpWallpaper = wallpaper[0];

		wpWallpaper.init ();

		return wpWallpaper;
	}

	@Override
	protected void onPostExecute (Wallpaper wpWallpaper)
	{
		try {
			if (this.isCancelled()) {
				return;
			}

			wpWallpaper.set();

			this.parent.asyncInitWallpaperDone(wpWallpaper);
		} catch (Exception ex) {
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this.parent);
		}
	}
}

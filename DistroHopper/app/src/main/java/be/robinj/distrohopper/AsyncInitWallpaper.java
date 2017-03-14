package be.robinj.distrohopper;

import android.os.AsyncTask;

import be.robinj.distrohopper.unity.Wallpaper;

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
		wpWallpaper.set ();

		this.parent.asyncInitWallpaperDone (wpWallpaper);
	}
}

package be.robinj.ubuntu.unity.launcher;

import android.view.View;

import be.robinj.ubuntu.App;
import be.robinj.ubuntu.ExceptionHandler;

/**
 * Created by robin on 8/21/14.
 */
public class AppLauncherLongClickListener implements View.OnLongClickListener
{
	@Override
	public boolean onLongClick (View view)
	{
		try
		{
			AppLauncher appLauncher = (AppLauncher) view;
			App app = appLauncher.getApp ();

			app.getAppManager ().unpin (app);
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (view.getContext (), ex);
			exh.show ();
		}

		return true;
	}
}

package be.robinj.ubuntu.unity.launcher;

import android.view.View;
import android.view.View.OnClickListener;

import be.robinj.ubuntu.ExceptionHandler;

/**
 * Created by robin on 8/21/14.
 */
public class AppLauncherClickListener implements OnClickListener
{
	@Override
	public void onClick (View view)
	{
		try
		{
			AppLauncher appLauncher = (AppLauncher) view;
			appLauncher.getApp ().launch ();
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (view.getContext (), ex);
			exh.show ();
		}
	}
}

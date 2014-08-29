package be.robinj.ubuntu.unity.launcher.service;

import android.view.View;

import be.robinj.ubuntu.ExceptionHandler;

/**
 * Created by robin on 8/28/14.
 */
public class AppLauncherClickListener implements View.OnClickListener
{
	@Override
	public void onClick (View view)
	{
		try
		{
			be.robinj.ubuntu.unity.launcher.service.AppLauncher appLauncher = (be.robinj.ubuntu.unity.launcher.service.AppLauncher) view;
			appLauncher.launch ();
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (view.getContext (), ex);
			exh.show ();
		}
	}
}
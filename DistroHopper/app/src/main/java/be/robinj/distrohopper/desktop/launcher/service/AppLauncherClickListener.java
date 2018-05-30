package be.robinj.distrohopper.desktop.launcher.service;

import android.view.View;

/**
 * Created by robin on 8/28/14.
 */
public class AppLauncherClickListener implements View.OnClickListener
{
	@Override
	public void onClick (View view)
	{
		/*try
		{*/
			be.robinj.distrohopper.desktop.launcher.service.AppLauncher appLauncher = (be.robinj.distrohopper.desktop.launcher.service.AppLauncher) view;
			appLauncher.launch ();
		/*}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex); // Doesn't work //
			exh.show (view.getContext());
		}*/
	}
}
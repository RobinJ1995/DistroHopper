package be.robinj.distrohopper.desktop.launcher;

import android.view.View;
import android.view.View.OnClickListener;

import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.HomeActivity;

/**
 * Created by robin on 8/21/14.
 */
public class AppLauncherClickListener implements OnClickListener
{
	private HomeActivity parent;
	
	public AppLauncherClickListener (HomeActivity parent)
	{
		this.parent = parent;
	}
	
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
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this.parent);
		}
	}
}

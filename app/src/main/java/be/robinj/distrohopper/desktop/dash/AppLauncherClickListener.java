package be.robinj.distrohopper.desktop.dash;

import android.view.View;
import android.widget.AdapterView;

import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.HomeActivity;
import be.robinj.distrohopper.desktop.AppLauncher;

/**
 * Created by robin on 8/21/14.
 */
public class AppLauncherClickListener implements AdapterView.OnItemClickListener
{
	private HomeActivity parent;
	
	public AppLauncherClickListener (HomeActivity parent)
	{
		this.parent = parent;
	}
	
	@Override
	public void onItemClick (AdapterView<?> parent, View view, int position, long id)
	{
		try
		{
			AppLauncher appLauncher = (AppLauncher) view.getTag ();
			appLauncher.getApp ().launch ();
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this.parent);
		}
	}
}

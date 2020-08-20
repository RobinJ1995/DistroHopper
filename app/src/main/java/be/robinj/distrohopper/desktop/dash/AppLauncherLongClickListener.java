package be.robinj.distrohopper.desktop.dash;

import android.view.View;
import android.widget.AdapterView;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.HomeActivity;

/**
 * Created by robin on 8/21/14.
 */
public class AppLauncherLongClickListener implements AdapterView.OnItemLongClickListener
{
	private HomeActivity parent;
	
	public AppLauncherLongClickListener (HomeActivity parent)
	{
		this.parent = parent;
	}
	
	@Override
	public boolean onItemLongClick (AdapterView<?> parent, View view, int position, long id)
	{
		try
		{
			AppLauncher appLauncher = (AppLauncher) view.getTag ();
			App app = appLauncher.getApp ();

			app.getAppManager ().pin (app);
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this.parent);
		}

		return true;
	}
}

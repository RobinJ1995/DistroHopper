package be.robinj.distrohopper.unity.dash;

import android.view.View;
import android.widget.AdapterView;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.ExceptionHandler;

/**
 * Created by robin on 8/21/14.
 */
public class AppLauncherLongClickListener implements AdapterView.OnItemLongClickListener
{
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
			ExceptionHandler exh = new ExceptionHandler (view.getContext (), ex);
			exh.show ();
		}

		return true;
	}
}

package be.robinj.distrohopper.unity.dash;

import android.view.View;
import android.widget.AdapterView;

import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.unity.AppLauncher;

/**
 * Created by robin on 8/21/14.
 */
public class AppLauncherClickListener implements AdapterView.OnItemClickListener
{
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
			ExceptionHandler exh = new ExceptionHandler (view.getContext (), ex);
			exh.show ();
		}
	}
}

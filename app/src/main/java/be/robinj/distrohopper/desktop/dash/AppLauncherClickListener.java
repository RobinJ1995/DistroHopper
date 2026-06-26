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
			Object tag = view.getTag ();

			if (tag instanceof DashItem.AppItem)
			{
				((DashItem.AppItem) tag).getApp ().launch ();
			}
			else if (tag instanceof DashItem.FolderItem)
			{
				DashItem.FolderItem folder = (DashItem.FolderItem) tag;
				new FolderPopup (this.parent, folder.getFolder ().getId (), folder.getApps ())
					.showAt (view);
			}
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this.parent);
		}
	}
}

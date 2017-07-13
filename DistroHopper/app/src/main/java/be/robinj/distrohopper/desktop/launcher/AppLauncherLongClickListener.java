package be.robinj.distrohopper.desktop.launcher;

import android.content.ClipData;
import android.view.View;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.AppManager;
import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.HomeActivity;

/**
 * Created by robin on 8/21/14.
 */
public class AppLauncherLongClickListener implements View.OnLongClickListener
{
	private HomeActivity parent;
	
	public AppLauncherLongClickListener (HomeActivity parent)
	{
		this.parent = parent;
	}
	
	@Override
	public boolean onLongClick (View view)
	{
		try
		{
			App app = (App) view.getTag ();
			AppManager appManager = app.getAppManager ();
			
			int index = appManager.indexOfPinned (app);

			ClipData.Item item = new ClipData.Item (Integer.toString (index));
			ClipData data = new ClipData (Integer.toString (index), new String[]{"text/plain"}, item);
			View.DragShadowBuilder dragShadowBuilder = new View.DragShadowBuilder (view);

			view.startDrag (data, dragShadowBuilder, item, 0);
			appManager.startedDraggingPinnedApp ();
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this.parent, ex);
			exh.show ();
		}

		return true;
	}
}

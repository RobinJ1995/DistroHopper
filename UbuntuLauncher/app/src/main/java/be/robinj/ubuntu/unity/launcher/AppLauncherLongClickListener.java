package be.robinj.ubuntu.unity.launcher;

import android.content.ClipData;
import android.os.Build;
import android.view.View;

import be.robinj.ubuntu.App;
import be.robinj.ubuntu.AppManager;
import be.robinj.ubuntu.ExceptionHandler;

/**
 * Created by robin on 8/21/14.
 */
public class AppLauncherLongClickListener implements View.OnLongClickListener
{
	@Override
	public boolean onLongClick (View view)
	{
		try
		{
			App app = (App) view.getTag ();
			AppManager appManager = app.getAppManager ();

			if (Build.VERSION.SDK_INT >= 11)
			{
				int index = appManager.indexOfPinned (app);

				ClipData.Item item = new ClipData.Item (Integer.toString (index));
				ClipData data = new ClipData (Integer.toString (index), new String[]{"text/plain"}, item);
				View.DragShadowBuilder dragShadowBuilder = new View.DragShadowBuilder (view);

				view.startDrag (data, dragShadowBuilder, item, 0);
				appManager.startedDraggingPinnedApp ();
			}
			else
			{
				appManager.unpin (app);
			}
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (view.getContext (), ex);
			exh.show ();
		}

		return true;
	}
}

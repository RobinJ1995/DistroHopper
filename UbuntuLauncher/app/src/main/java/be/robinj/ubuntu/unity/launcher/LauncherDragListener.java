package be.robinj.ubuntu.unity.launcher;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;

import be.robinj.ubuntu.AppManager;
import be.robinj.ubuntu.ExceptionHandler;

/**
 * Created by robin on 03/09/14.
 */
@TargetApi (Build.VERSION_CODES.HONEYCOMB)
public class LauncherDragListener implements ViewGroup.OnDragListener
{
	private AppManager appManager;

	public LauncherDragListener (AppManager appManager)
	{
		this.appManager = appManager;
	}

	@Override
	public boolean onDrag (View view, DragEvent event)
	{
		try
		{
			switch (event.getAction ())
			{
				case DragEvent.ACTION_DRAG_ENTERED:
					this.appManager.startedDraggingPinnedApp ();
					break;
				case DragEvent.ACTION_DROP:
				case DragEvent.ACTION_DRAG_EXITED:
					this.appManager.stoppedDraggingPinnedApp ();
					break;
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

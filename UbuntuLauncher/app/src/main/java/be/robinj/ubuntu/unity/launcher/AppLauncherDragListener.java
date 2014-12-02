package be.robinj.ubuntu.unity.launcher;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;

import be.robinj.ubuntu.App;
import be.robinj.ubuntu.AppManager;
import be.robinj.ubuntu.ExceptionHandler;

/**
 * Created by robin on 03/09/14.
 */
@TargetApi (Build.VERSION_CODES.HONEYCOMB)
public class AppLauncherDragListener implements ViewGroup.OnDragListener
{
	private AppManager appManager;

	public AppLauncherDragListener (AppManager appManager)
	{
		this.appManager = appManager;
	}

	@Override
	public boolean onDrag (View view, DragEvent event)
	{
		try
		{
			AppLauncher appLauncher = (AppLauncher) view;
			App app = appLauncher.getApp ();

			switch (event.getAction ())
			{
				case DragEvent.ACTION_DRAG_ENTERED:
					if (Build.VERSION.SDK_INT >= 12)
						appLauncher.animate ().setStartDelay (0).setDuration (120).alpha (0.2F);
					else
						appLauncher.setAlpha (0.2F);
					break;
				case DragEvent.ACTION_DROP: // Falls through //
					int oldIndex = Integer.parseInt (event.getClipData ().getDescription ().getLabel ().toString ());
					int newIndex = this.appManager.indexOfPinned (app);

					this.appManager.movePinnedApp (appLauncher, oldIndex, newIndex);
					this.appManager.refreshPinnedView ();

					this.appManager.savePinnedApps ();
					this.appManager.stoppedDraggingPinnedApp ();
				case DragEvent.ACTION_DRAG_EXITED:
					if (Build.VERSION.SDK_INT >= 12)
						appLauncher.animate ().setStartDelay (0).setDuration (120).alpha (0.9F);
					else
						appLauncher.setAlpha (0.9F);
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

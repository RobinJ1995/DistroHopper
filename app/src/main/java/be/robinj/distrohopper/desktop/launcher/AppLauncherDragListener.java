package be.robinj.distrohopper.desktop.launcher;

import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.AppManager;
import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.widgets.WidgetContainer;

/**
 * Created by robin on 03/09/14.
 */
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
			// Widget drags carry a non-numeric clip label and are handled by
			// WidgetsContainer_DragListener and the trash's own listener //
			if (event.getLocalState () instanceof WidgetContainer)
				return false;

			AppLauncher appLauncher = (AppLauncher) view;
			App app = appLauncher.getApp ();

			switch (event.getAction ())
			{
				case DragEvent.ACTION_DRAG_ENTERED:
					appLauncher.animate ().setStartDelay (0).setDuration (120).alpha (0.2F);
					break;
				case DragEvent.ACTION_DROP: // Falls through //
					int oldIndex = Integer.parseInt (event.getClipData ().getDescription ().getLabel ().toString ());
					int newIndex = this.appManager.indexOfPinned (app);

					this.appManager.movePinnedApp (oldIndex, newIndex);
					this.appManager.refreshPinnedView ();

					this.appManager.savePinnedApps ();
				case DragEvent.ACTION_DRAG_ENDED: // Falls through //
					this.appManager.stoppedDraggingPinnedApp ();
				case DragEvent.ACTION_DRAG_EXITED:
					appLauncher.animate ().setStartDelay (0).setDuration (120).alpha (0.9F);
					break;
			}
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this.appManager.getContext ());
		}

		return true;
	}
}

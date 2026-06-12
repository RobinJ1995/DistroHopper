package be.robinj.distrohopper.desktop.launcher;

import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;

import be.robinj.distrohopper.AppManager;
import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.widgets.WidgetContainer;

/**
 * Created by robin on 03/09/14.
 */
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
			// Widget drags are handled by WidgetsContainer_DragListener and the trash's
			// own listener; reacting here would hide the trash mid-drag //
			if (event.getLocalState () instanceof WidgetContainer)
				return false;

			switch (event.getAction ())
			{
				case DragEvent.ACTION_DRAG_ENTERED:
					this.appManager.startedDraggingPinnedApp ();
					break;
				case DragEvent.ACTION_DROP:
					// A drop on the bar itself — most often on the empty slot kept
					// open for the dragged icon — commits the previewed order //
					this.appManager.droppedPinnedApp ();
					this.appManager.stoppedDraggingPinnedApp ();
					break;
				case DragEvent.ACTION_DRAG_EXITED:
					this.appManager.stoppedDraggingPinnedApp ();
					break;
				case DragEvent.ACTION_DRAG_ENDED:
					// Restores the bar if the drag ended without a drop on it //
					this.appManager.endedDraggingPinnedApp ();
					this.appManager.stoppedDraggingPinnedApp ();
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

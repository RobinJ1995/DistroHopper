package be.robinj.distrohopper.desktop.launcher;

import android.graphics.Color;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;

import be.robinj.distrohopper.AppManager;
import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.HomeActivity;
import be.robinj.distrohopper.home.LauncherBarBinder;
import be.robinj.distrohopper.widgets.WidgetContainer;

/**
 * Created by robin on 03/09/14.
 *
 * Attached in onCreate so widgets (draggable before app loading finishes) can
 * always be dropped on the trash; the appManager is resolved per event because
 * it only exists once loading is done, and only app drags need it.
 */
public class TrashDragListener implements ViewGroup.OnDragListener
{
	private final HomeActivity activity;
	private int colour = -1;

	public TrashDragListener (final HomeActivity activity)
	{
		this.activity = activity;
	}

	@Override
	public boolean onDrag (View view, DragEvent event)
	{
		try
		{
			AppLauncher lalTrash = (AppLauncher) view;
			if (this.colour == -1)
				this.colour = lalTrash.getColour ();

			switch (event.getAction ())
			{
				case DragEvent.ACTION_DRAG_ENTERED:
					LauncherBarBinder.startedDragging (this.activity);
					lalTrash.setColour (Color.rgb (255, 40, 40));
					break;
				case DragEvent.ACTION_DROP: // Falls through //
					if (event.getLocalState () instanceof WidgetContainer)
					{
						((WidgetContainer) event.getLocalState ()).removeWidget ();
					}
					else
					{
						int index = Integer.parseInt (event.getClipData ().getDescription ().getLabel ().toString ());
						final AppManager appManager = this.activity.getAppManager ();
						if (appManager != null)
							appManager.unpin (index);
					}

					LauncherBarBinder.stoppedDragging (this.activity);
				case DragEvent.ACTION_DRAG_EXITED:
					lalTrash.setColour (this.colour);
					break;
			}
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this.activity);
		}

		return true;
	}
}

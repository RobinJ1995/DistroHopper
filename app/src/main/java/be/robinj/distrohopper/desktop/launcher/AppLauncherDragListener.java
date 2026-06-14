package be.robinj.distrohopper.desktop.launcher;

import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.AppManager;
import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.widgets.DesktopAppView;
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
			// Widget drags are handled by WidgetsContainer_DragListener and the trash's
			// own listener. A desktop app dragged onto the bar is a move-to-bar,
			// handled by the bar container's LauncherDragListener (and the trash) —
			// not here, where it would be misread as a reorder of this icon //
			if (event.getLocalState () instanceof WidgetContainer
					|| event.getLocalState () instanceof DesktopAppView)
				return false;

			AppLauncher appLauncher = (AppLauncher) view;
			App app = appLauncher.getApp ();

			switch (event.getAction ())
			{
				case DragEvent.ACTION_DRAG_ENTERED:
					// Shift the empty slot over to this icon's position so that the
					// other icons slide over and preview the new order //
					this.appManager.draggedPinnedAppOver (app);
					break;
				case DragEvent.ACTION_DROP:
					this.appManager.droppedPinnedApp ();
					this.appManager.stoppedDraggingPinnedApp ();
					break;
				case DragEvent.ACTION_DRAG_ENDED:
					// Posted: ENDED is dispatched by iterating each container's
					// drag-interested children, and mutating views (even just
					// visibility) modifies that set — a ConcurrentModificationException //
					view.post (() ->
					{
						this.appManager.endedDraggingPinnedApp ();
						this.appManager.stoppedDraggingPinnedApp ();
					});
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

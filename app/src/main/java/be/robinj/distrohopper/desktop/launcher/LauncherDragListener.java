package be.robinj.distrohopper.desktop.launcher;

import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;

import be.robinj.distrohopper.AppManager;
import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.widgets.DesktopAppHost;
import be.robinj.distrohopper.widgets.DesktopAppView;
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

			// A desktop app dragged onto the bar is a move: unpin it from the
			// desktop and pin it to the bar. The reorder placeholder machinery
			// doesn't apply (it was never a bar icon), so handle it separately //
			if (event.getLocalState () instanceof DesktopAppView)
			{
				final DesktopAppView appView = (DesktopAppView) event.getLocalState ();

				switch (event.getAction ())
				{
					case DragEvent.ACTION_DROP:
						final DesktopAppHost host = this.appManager.getParent ().getDesktopAppHost ();
						if (host != null)
						{
							host.remove (appView);
							this.appManager.pin (appView.getApp (), true, false, true);
						}
						this.appManager.stoppedDraggingPinnedApp ();
						break;
					case DragEvent.ACTION_DRAG_ENDED:
						// Restores the bar chrome (trash/bfb) once the drag is over //
						view.post (() -> this.appManager.stoppedDraggingPinnedApp ());
						break;
				}

				return true;
			}

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
				// No ACTION_DRAG_EXITED case: spurious exits fire while the icons
				// animate out of the way (and when hovering the trash), briefly
				// flickering the bar out of drag mode; ENDED always follows anyway //
				case DragEvent.ACTION_DRAG_ENDED:
					// Restores the bar if the drag ended without a drop on it.
					// Posted: mutating views (even just visibility) during ENDED
					// dispatch throws a ConcurrentModificationException //
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

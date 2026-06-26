package be.robinj.distrohopper.desktop.launcher;

import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;

import be.robinj.distrohopper.AppManager;
import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.widgets.DesktopAppView;
import be.robinj.distrohopper.widgets.WidgetContainer;

/**
 * Drag handling for a single pinned-bar item (an app icon or a folder). Hovering
 * shifts the dragged item's empty slot to this item's position to preview a
 * reorder; pausing (a dwell) over the item instead arms a fold, so releasing
 * creates a folder with this app — or adds to this folder. A folder being
 * dragged only ever reorders (folders can't be nested).
 *
 * The fold's dwell timer and armed target live centrally on the binder (via
 * {@link AppManager#hoverPinnedItem} / {@link AppManager#dropPinnedFold}), not
 * per-listener: the reorder preview slides the dragged item's own invisible
 * placeholder under the finger, which fires a spurious EXITED on the icon we
 * just dwelled over — a per-listener dwell would cancel itself, and the drop
 * would land on the placeholder rather than the target.
 *
 * Created by robin on 03/09/14.
 */
public class AppLauncherDragListener implements ViewGroup.OnDragListener
{
	private final AppManager appManager;

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
			// own listener //
			if (event.getLocalState () instanceof WidgetContainer)
				return false;

			switch (event.getAction ())
			{
				case DragEvent.ACTION_DRAG_ENTERED:
					// Preview the reorder and (re)arm the dwell-fold onto this item //
					this.appManager.hoverPinnedItem (view);
					break;
				case DragEvent.ACTION_DROP:
					// A dwell-armed drop folds onto the armed target; otherwise it
					// commits the previewed reorder. A desktop app dragged here rode
					// in on a dash-style placeholder, so it is pinned by the commit
					// then removed from the desktop to complete the move //
					if (! this.appManager.dropPinnedFold ())
						this.appManager.droppedPinnedApp ();
					if (event.getLocalState () instanceof DesktopAppView
							&& this.appManager.getParent ().getDesktopAppHost () != null)
						this.appManager.getParent ().getDesktopAppHost ()
							.remove ((DesktopAppView) event.getLocalState ());
					this.appManager.stoppedDraggingPinnedApp ();
					break;
				case DragEvent.ACTION_DRAG_ENDED:
					this.appManager.cancelPinnedFold ();
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

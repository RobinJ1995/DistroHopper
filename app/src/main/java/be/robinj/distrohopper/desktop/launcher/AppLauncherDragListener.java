package be.robinj.distrohopper.desktop.launcher;

import android.os.Handler;
import android.os.Looper;
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
 * dragged only ever reorders (folders can't be nested), which
 * {@link AppManager#foldDraggedOnto} enforces.
 *
 * Created by robin on 03/09/14.
 */
public class AppLauncherDragListener implements ViewGroup.OnDragListener
{
	private static final long FOLDER_DWELL_MS = 550L;

	private final AppManager appManager;
	private final Handler handler = new Handler (Looper.getMainLooper ());
	private boolean armedForFold = false;

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
					// Preview the reorder immediately; arm a fold if the drag lingers //
					this.appManager.draggedPinnedItemOver (view);
					this.armedForFold = false;
					this.handler.postDelayed (() -> this.armedForFold = true, FOLDER_DWELL_MS);
					break;
				case DragEvent.ACTION_DRAG_EXITED:
					this.handler.removeCallbacksAndMessages (null);
					this.armedForFold = false;
					break;
				case DragEvent.ACTION_DROP:
					this.handler.removeCallbacksAndMessages (null);
					// A dwell-armed drop folds onto this item; otherwise it commits the
					// previewed reorder. A desktop app dragged here rode in on a
					// dash-style placeholder, so it is pinned by the commit then removed
					// from the desktop to complete the move //
					if (! (this.armedForFold && this.appManager.foldDraggedOnto (view)))
						this.appManager.droppedPinnedApp ();
					this.armedForFold = false;
					if (event.getLocalState () instanceof DesktopAppView
							&& this.appManager.getParent ().getDesktopAppHost () != null)
						this.appManager.getParent ().getDesktopAppHost ()
							.remove ((DesktopAppView) event.getLocalState ());
					this.appManager.stoppedDraggingPinnedApp ();
					break;
				case DragEvent.ACTION_DRAG_ENDED:
					this.handler.removeCallbacksAndMessages (null);
					this.armedForFold = false;
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

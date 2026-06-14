package be.robinj.distrohopper.desktop.launcher;

import android.graphics.Color;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.AppManager;
import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.HomeActivity;
import be.robinj.distrohopper.desktop.dash.DashDragPayload;
import be.robinj.distrohopper.home.LauncherBarBinder;
import be.robinj.distrohopper.widgets.DesktopAppHost;
import be.robinj.distrohopper.widgets.DesktopAppView;
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
					else if (event.getLocalState () instanceof DesktopAppView)
					{
						final DesktopAppHost host = this.activity.getDesktopAppHost ();
						if (host != null)
							host.remove ((DesktopAppView) event.getLocalState ());
					}
					else if (event.getLocalState () instanceof DashDragPayload)
					{
						// Dropping a dash folder on the trash deletes it (members return
						// to the dash); a folder member dropped here leaves its folder //
						final AppManager appManager = this.activity.getAppManager ();
						if (appManager != null)
						{
							DashDragPayload payload = (DashDragPayload) event.getLocalState ();
							if (payload instanceof DashDragPayload.FolderDrag)
								appManager.getDashLayout ().deleteFolder (
									((DashDragPayload.FolderDrag) payload).getFolderId ());
							else if (payload instanceof DashDragPayload.FolderMemberDrag)
								appManager.getDashLayout ().removeFromFolder (
									((DashDragPayload.FolderMemberDrag) payload).getFolderId (),
									((DashDragPayload.FolderMemberDrag) payload).getApp ().getProfileScopedKey ());

							appManager.dashLayoutChanged ();
						}
					}
					else if (event.getLocalState () instanceof App)
					{
						// A not-yet-pinned app dragged from the dash: nothing to unpin //
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

package be.robinj.distrohopper.desktop.launcher;

import android.content.ClipData;
import android.view.View;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.AppManager;
import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.HomeActivity;

/**
 * Created by robin on 8/21/14.
 */
public class AppLauncherLongClickListener implements View.OnLongClickListener
{
	private HomeActivity parent;
	
	public AppLauncherLongClickListener (HomeActivity parent)
	{
		this.parent = parent;
	}
	
	@Override
	public boolean onLongClick (View view)
	{
		try
		{
			App app = (App) view.getTag ();
			AppManager appManager = app.getAppManager ();
			
			int index = appManager.indexOfPinned (app);

			ClipData.Item item = new ClipData.Item (Integer.toString (index));
			ClipData data = new ClipData (Integer.toString (index), new String[]{"text/plain"}, item);
			View.DragShadowBuilder dragShadowBuilder = new View.DragShadowBuilder (view);

			// Only enter drag mode if the drag really started: doing so without
			// an active drag would leave the bar stuck, as no ACTION_DRAG_ENDED
			// will ever restore it //
			if (view.startDragAndDrop (data, dragShadowBuilder, item, 0))
				appManager.startedDraggingPinnedApp (app);
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this.parent);
		}

		return true;
	}

	/**
	 * Starts dragging a pinned folder: a reposition (committed via the launcher
	 * layout's item order) or a drop on the trash to delete it (and unpin its
	 * members). A folder can't leave the launcher, so this carries a
	 * {@link LauncherDragPayload} rather than the pinned-index ClipData.
	 */
	public static void startFolderDrag (HomeActivity parent, View view, String folderId)
	{
		AppManager appManager = parent.getAppManager ();
		View source = view.isAttachedToWindow () ? view : parent.getWindow ().getDecorView ();

		LauncherDragPayload payload = new LauncherDragPayload.FolderDrag (folderId);
		ClipData data = ClipData.newPlainText ("launcherFolder", folderId);

		if (source.startDragAndDrop (data, new View.DragShadowBuilder (view), payload, 0))
			appManager.startedDraggingFolder (folderId);
	}
}

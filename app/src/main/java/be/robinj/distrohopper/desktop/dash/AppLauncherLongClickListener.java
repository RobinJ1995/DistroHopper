package be.robinj.distrohopper.desktop.dash;

import android.content.ClipData;
import android.view.View;
import android.widget.AdapterView;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.AppManager;
import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.HomeActivity;

/**
 * Created by robin on 8/21/14.
 */
public class AppLauncherLongClickListener implements AdapterView.OnItemLongClickListener
{
	private HomeActivity parent;

	public AppLauncherLongClickListener (HomeActivity parent)
	{
		this.parent = parent;
	}

	@Override
	public boolean onItemLongClick (AdapterView<?> parent, View view, int position, long id)
	{
		try
		{
			AppLauncher appLauncher = (AppLauncher) view.getTag ();

			startAppDrag (view, appLauncher.getApp ());
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this.parent);
		}

		return true;
	}

	/**
	 * Starts dragging an app towards the launcher bar: a reorder of its
	 * existing icon if it is already pinned, a pin-by-drop otherwise.
	 */
	public static void startAppDrag (View view, App app)
	{
		AppManager appManager = app.getAppManager ();

		// The pressed view may have been detached by the time the long press
		// fires — lens results are re-rendered as the slower lenses stream
		// their results in. A drag can only start from an attached view, so
		// start it from the window instead, still drawing the pressed view
		// as the drag shadow //
		View source = view.isAttachedToWindow ()
			? view
			: appManager.getContext ().getWindow ().getDecorView ();

		if (appManager.isPinned (app))
		{
			// Already on the launcher: dragging moves its existing icon,
			// exactly like a long press on the launcher itself //
			int index = appManager.indexOfPinned (app);

			ClipData.Item item = new ClipData.Item (Integer.toString (index));
			ClipData data = new ClipData (Integer.toString (index), new String[]{"text/plain"}, item);

			// Only enter drag mode if the drag really started: doing so
			// without an active drag would leave the bar stuck, as no
			// ACTION_DRAG_ENDED will ever restore it //
			if (source.startDrag (data, new View.DragShadowBuilder (view), item, 0))
				appManager.startedDraggingPinnedApp (app);
		}
		else
		{
			// Not pinned yet: drag it onto the launcher to pin it at the
			// position it is dropped on. The App local state tells the
			// trash's listener this is a pin-by-drop, not a reorder //
			ClipData data = ClipData.newPlainText ("dash", "dash");

			if (source.startDrag (data, new View.DragShadowBuilder (view), app, 0))
				appManager.startedDraggingDashApp (app);
		}
	}
}

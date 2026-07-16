package be.robinj.distrohopper.desktop.launcher;

import android.graphics.Color;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.HomeActivity;
import be.robinj.distrohopper.home.LauncherBarBinder;

/**
 * The launcher bar's "app info" drop target, shown in the trash's place while
 * an app icon is dragged from the dash (where the trash would have nothing to
 * delete — the dash always shows every installed app). Dropping the icon here
 * opens the system's App info screen for it.
 */
public class AppInfoDragListener implements ViewGroup.OnDragListener
{
	private final HomeActivity activity;
	private int colour = -1;

	public AppInfoDragListener (final HomeActivity activity)
	{
		this.activity = activity;
	}

	@Override
	public boolean onDrag (View view, DragEvent event)
	{
		try
		{
			AppLauncher lalAppInfo = (AppLauncher) view;
			if (this.colour == -1)
				this.colour = lalAppInfo.getColour ();

			switch (event.getAction ())
			{
				case DragEvent.ACTION_DRAG_ENTERED:
					lalAppInfo.setColour (Color.rgb (40, 120, 255));
					break;
				case DragEvent.ACTION_DROP: // Falls through //
					final App app = draggedApp (event);
					if (app != null)
						app.openAppInfo ();

					LauncherBarBinder.stoppedDragging (this.activity);
				case DragEvent.ACTION_DRAG_EXITED:
					lalAppInfo.setColour (this.colour);
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

	/**
	 * The app behind a dash icon drag: a plain {@link App} (not pinned yet) or a
	 * {@link LauncherDragPayload.PinnedAppDrag} (already on the launcher bar).
	 * Anything else (folders, widgets, ...) never shows this target; ignore it.
	 */
	private static App draggedApp (final DragEvent event)
	{
		final Object localState = event.getLocalState ();
		if (localState instanceof App)
			return (App) localState;
		else if (localState instanceof LauncherDragPayload.PinnedAppDrag)
			return ((LauncherDragPayload.PinnedAppDrag) localState).getApp ();

		return null;
	}
}

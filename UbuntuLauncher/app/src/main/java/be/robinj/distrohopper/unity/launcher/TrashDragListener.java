package be.robinj.distrohopper.unity.launcher;

import android.annotation.TargetApi;
import android.graphics.Color;
import android.os.Build;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;

import be.robinj.distrohopper.AppManager;
import be.robinj.distrohopper.ExceptionHandler;

/**
 * Created by robin on 03/09/14.
 */
@TargetApi (Build.VERSION_CODES.HONEYCOMB)
public class TrashDragListener implements ViewGroup.OnDragListener
{
	private AppManager appManager;
	private int colour = -1;

	public TrashDragListener (AppManager appManager)
	{
		this.appManager = appManager;
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
					lalTrash.setColour (Color.rgb (255, 40, 40));
					break;
				case DragEvent.ACTION_DROP: // Falls through //
					int index = Integer.parseInt (event.getClipData ().getDescription ().getLabel ().toString ());
					this.appManager.unpin (index);

					this.appManager.stoppedDraggingPinnedApp ();
				case DragEvent.ACTION_DRAG_EXITED:
					lalTrash.setColour (this.colour);
					break;
			}
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (view.getContext (), ex);
			exh.show ();
		}

		return true;
	}
}

package be.robinj.distrohopper.desktop;

import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by robin on 03/09/14.
 */
public class ReturnFalseDragListener implements ViewGroup.OnDragListener
{
	@Override
	public boolean onDrag (View v, DragEvent event)
	{
		return false;
	}
}

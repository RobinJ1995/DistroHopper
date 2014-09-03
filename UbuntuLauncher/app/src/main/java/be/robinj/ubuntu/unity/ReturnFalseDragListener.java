package be.robinj.ubuntu.unity;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by robin on 03/09/14.
 */
@TargetApi (Build.VERSION_CODES.HONEYCOMB)
public class ReturnFalseDragListener implements ViewGroup.OnDragListener
{
	@Override
	public boolean onDrag (View v, DragEvent event)
	{
		return false;
	}
}

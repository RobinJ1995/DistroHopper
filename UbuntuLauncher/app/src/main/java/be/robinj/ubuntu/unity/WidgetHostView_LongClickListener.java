package be.robinj.ubuntu.unity;

import android.appwidget.AppWidgetHostView;
import android.view.View;

import be.robinj.ubuntu.HomeActivity;

/**
 * Created by robin on 8/25/14.
 */
public class WidgetHostView_LongClickListener implements AppWidgetHostView.OnLongClickListener
{
	private HomeActivity parent;

	public WidgetHostView_LongClickListener (HomeActivity parent)
	{
		this.parent = parent;
	}

	@Override
	public boolean onLongClick (View view)
	{
		this.parent.removeWidget ((WidgetHostView) view);

		return true;
	}
}

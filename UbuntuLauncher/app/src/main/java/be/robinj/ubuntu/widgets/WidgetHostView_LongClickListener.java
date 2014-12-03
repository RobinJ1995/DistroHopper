package be.robinj.ubuntu.widgets;

import android.appwidget.AppWidgetHostView;
import android.view.View;

import be.robinj.ubuntu.HomeActivity;

/**
 * Created by robin on 8/25/14.
 */
public class WidgetHostView_LongClickListener implements AppWidgetHostView.OnLongClickListener
{
	private HomeActivity parent;
	private WidgetHostView hostView;

	public WidgetHostView_LongClickListener (HomeActivity parent, WidgetHostView hostView)
	{
		this.parent = parent;
		this.hostView = hostView;
	}

	@Override
	public boolean onLongClick (View view)
	{
		//this.parent.removeWidget ((WidgetHostView) view);
		this.hostView.setEditMode (! this.hostView.getEditMode ());

		return true;
	}
}

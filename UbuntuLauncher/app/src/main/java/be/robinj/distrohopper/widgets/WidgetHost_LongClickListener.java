package be.robinj.distrohopper.widgets;

import android.view.View;
import android.widget.GridLayout;

/**
 * Created by robin on 8/24/14.
 */
public class WidgetHost_LongClickListener implements GridLayout.OnLongClickListener
{
	private WidgetHost widgetHost;

	public WidgetHost_LongClickListener (WidgetHost widgetHost)
	{
		this.widgetHost = widgetHost;
	}

	@Override
	public boolean onLongClick (View view)
	{
		this.widgetHost.selectWidget ();

		return false;
	}
}

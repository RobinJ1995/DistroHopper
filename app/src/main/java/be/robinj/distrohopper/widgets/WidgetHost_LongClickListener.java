package be.robinj.distrohopper.widgets;

import android.view.View;

/**
 * Created by robin on 8/24/14.
 */
public class WidgetHost_LongClickListener implements View.OnLongClickListener
{
	private WidgetHost widgetHost;

	public WidgetHost_LongClickListener (WidgetHost widgetHost)
	{
		this.widgetHost = widgetHost;
	}

	@Override
	public boolean onLongClick (View view)
	{
		if (view instanceof WidgetsContainer && ((WidgetsContainer) view).hasEditModeChild ())
			((WidgetsContainer) view).exitEditMode ();
		else
			this.widgetHost.showPicker ();

		return true;
	}
}

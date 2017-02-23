package be.robinj.distrohopper.widgets;

import android.view.View;

/**
 * Created by robin on 23/02/17.
 */

public class WidgetContainerRemove_ClickListener implements View.OnClickListener
{
	private WidgetContainer widgetContainer;

	public WidgetContainerRemove_ClickListener (WidgetContainer widgetContainer)
	{
		this.widgetContainer = widgetContainer;
	}

	@Override
	public void onClick (View view)
	{
		this.widgetContainer.removeWidget ();
	}
}

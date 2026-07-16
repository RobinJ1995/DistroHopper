package be.robinj.distrohopper.widgets;

import android.view.View;

import be.robinj.distrohopper.HomeActivity;

/**
 * Long-press on empty desktop space: exits widget edit mode if a widget is
 * being edited, otherwise opens the desktop menu (add widget / settings).
 */
public class WidgetsPager_LongClickListener implements View.OnLongClickListener
{
	private final HomeActivity activity;

	public WidgetsPager_LongClickListener (final HomeActivity activity)
	{
		this.activity = activity;
	}

	@Override
	public boolean onLongClick (final View view)
	{
		if (view instanceof WidgetsPager && ((WidgetsPager) view).hasEditModeChild ())
			((WidgetsPager) view).exitEditMode ();
		else
			this.activity.showDesktopMenu ();

		return true;
	}
}

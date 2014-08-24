package be.robinj.ubuntu.unity;

import android.view.View;
import android.widget.GridLayout;

import be.robinj.ubuntu.HomeActivity;

/**
 * Created by robin on 8/24/14.
 */
public class WidgetHost_LongClickListener implements GridLayout.OnLongClickListener
{
	private HomeActivity parent;

	public WidgetHost_LongClickListener (HomeActivity parent)
	{
		this.parent = parent;
	}

	@Override
	public boolean onLongClick (View view)
	{
		this.parent.selectWidget ();

		return false;
	}
}

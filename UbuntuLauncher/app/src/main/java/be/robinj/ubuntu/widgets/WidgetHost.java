package be.robinj.ubuntu.widgets;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;

import be.robinj.ubuntu.HomeActivity;

/**
 * Created by robin on 8/25/14.
 */
public class WidgetHost extends AppWidgetHost
{
	private HomeActivity parent;

	public WidgetHost (HomeActivity parent, int hostId)
	{
		super (parent.getApplicationContext (), hostId);

		this.parent = parent;
	}

	@Override
	protected AppWidgetHostView onCreateView (Context context, int id, AppWidgetProviderInfo info)
	{
		return new WidgetHostView (context, this.parent);
	}
}

package be.robinj.ubuntu.widgets;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;

/**
 * Created by robin on 8/25/14.
 */
public class WidgetHost extends AppWidgetHost
{
	public WidgetHost (Context context, int hostId)
	{
		super (context, hostId);
	}

	@Override
	protected AppWidgetHostView onCreateView (Context context, int id, AppWidgetProviderInfo info)
	{
		return new WidgetHostView (context);
	}
}

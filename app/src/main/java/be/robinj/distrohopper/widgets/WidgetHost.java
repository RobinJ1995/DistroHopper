package be.robinj.distrohopper.widgets;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.GridLayout;
import android.widget.RelativeLayout;

import be.robinj.distrohopper.HomeActivity;
import be.robinj.distrohopper.RequestCode;
import be.robinj.distrohopper.dev.Log;

/**
 * Created by robin on 8/25/14.
 */
public class WidgetHost extends AppWidgetHost
{
	private HomeActivity parent;
	private AppWidgetManager widgetManager;
	private WidgetsContainer vgWidgets;

	public WidgetHost (HomeActivity parent, AppWidgetManager widgetManager, int hostId)
	{
		super (parent.getApplicationContext (), hostId);

		this.parent = parent;
		this.widgetManager = widgetManager;
		this.vgWidgets = parent.findViewById (hostId);
	}

	@Override
	protected AppWidgetHostView onCreateView (Context context, int id, AppWidgetProviderInfo info)
	{
		return new WidgetHostView (context, this);
	}

	public void removeWidget (AppWidgetHostView hostView)
	{
		this.deleteAppWidgetId (hostView.getAppWidgetId ());
		this.vgWidgets.removeView (hostView);
	}

	public void removeWidget (Intent data) throws Exception
	{
		if (data != null)
		{
			Bundle bundle = data.getExtras ();
			int id = bundle.getInt (AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

			if (id == -1)
				throw new Exception ("Didn't receive a widget ID");

			this.deleteAppWidgetId (id);
		}
	}

	public void createWidget (Intent data) throws Exception
	{
		Bundle bundle = data.getExtras ();
		int id = bundle.getInt (AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

		if (id == -1)
			throw new Exception ("Didn't receive a widget ID");

		AppWidgetProviderInfo info = this.widgetManager.getAppWidgetInfo (id);
		WidgetHostView hostView = (WidgetHostView) this.createView (this.parent, id, info);

		WidgetContainer container = new WidgetContainer (this.parent.getApplicationContext (), null, hostView);

		this.vgWidgets.addView (container);

		Log.getInstance().v(this.getClass().getSimpleName(), "Widget created: " + id);

		hostView.setOnLongClickListener (new WidgetHostView_LongClickListener (container));
	}

	public void configureWidget (Intent data) throws Exception
	{
		Bundle bundle = data.getExtras ();
		int id = bundle.getInt (AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

		AppWidgetProviderInfo info = this.widgetManager.getAppWidgetInfo (id);

		if (id == -1)
			throw new Exception ("Didn't receive a widget ID");

		if (info.configure == null)
		{
			this.createWidget (data);
		}
		else
		{
			Log.getInstance().v(this.getClass().getSimpleName(), "Widget requires configuration: " + id);

			Intent intent = new Intent (AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
			intent.setComponent (info.configure);
			intent.putExtra (AppWidgetManager.EXTRA_APPWIDGET_ID, id);

			this.parent.startActivityForResult (intent, RequestCode.WIDGET_CONFIGURED);
		}
	}

	public void selectWidget ()
	{
		int id = this.allocateAppWidgetId ();

		Intent intent = new Intent (AppWidgetManager.ACTION_APPWIDGET_PICK);
		intent.putExtra (AppWidgetManager.EXTRA_APPWIDGET_ID, id);

		/*
		ArrayList customInfo = new ArrayList();
		pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo);
		ArrayList customExtras = new ArrayList();
		pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras);

		addEmptyData (pickIntent);
		*/

		this.parent.startActivityForResult (intent, RequestCode.WIDGET_PICKED);
	}
}

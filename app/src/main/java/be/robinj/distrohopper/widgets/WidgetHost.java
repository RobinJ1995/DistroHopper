package be.robinj.distrohopper.widgets;

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import java.util.List;

import be.robinj.distrohopper.HomeActivity;
import be.robinj.distrohopper.R;
import be.robinj.distrohopper.RequestCode;
import be.robinj.distrohopper.dev.Log;

/**
 * Created by robin on 8/25/14.
 */
public class WidgetHost extends AppWidgetHost
{
	/** Must stay stable across builds; never derive this from a resource id. */
	public static final int HOST_ID = 0xD1570;

	private final HomeActivity parent;
	private final AppWidgetManager widgetManager;
	private final WidgetsContainer vgWidgets;
	private final WidgetPersistence persistence;

	private int pendingAppWidgetId = -1;
	private AppWidgetProviderInfo pendingInfo;

	public WidgetHost (HomeActivity parent, AppWidgetManager widgetManager, WidgetsContainer vgWidgets)
	{
		super (parent.getApplicationContext (), HOST_ID);

		this.parent = parent;
		this.widgetManager = widgetManager;
		this.vgWidgets = vgWidgets;
		this.persistence = new WidgetPersistence (parent.getApplicationContext ());
	}

	@Override
	protected AppWidgetHostView onCreateView (Context context, int id, AppWidgetProviderInfo info)
	{
		return new WidgetHostView (context, this);
	}

	/**
	 * Recreate the views for all persisted widgets, pruning any whose provider is gone.
	 */
	public void restoreWidgets ()
	{
		final List<WidgetLayout> layouts = this.persistence.load ();
		boolean pruned = false;

		for (final WidgetLayout layout : layouts)
		{
			if (this.widgetManager.getAppWidgetInfo (layout.appWidgetId) == null)
			{
				this.deleteAppWidgetId (layout.appWidgetId);
				pruned = true;

				Log.getInstance ().w (this.getClass ().getSimpleName (), "Pruned stale widget: " + layout.appWidgetId);
			}
			else
			{
				this.addWidget (layout.appWidgetId, layout, false);
			}
		}

		if (pruned)
			this.persist ();
	}

	private void addWidget (final int appWidgetId, final WidgetLayout layout, final boolean persist)
	{
		final AppWidgetProviderInfo info = this.widgetManager.getAppWidgetInfo (appWidgetId);

		if (info == null)
		{
			this.deleteAppWidgetId (appWidgetId);

			return;
		}

		final WidgetHostView hostView = (WidgetHostView) this.createView (this.parent, appWidgetId, info);
		final WidgetContainer container = new WidgetContainer (this.parent, this, hostView);

		this.vgWidgets.addView (container, new WidgetsContainer.LayoutParams (layout));

		hostView.setOnLongClickListener (new WidgetHostView_LongClickListener (container));

		Log.getInstance ().v (this.getClass ().getSimpleName (), "Widget added: " + appWidgetId);

		if (persist)
			this.persist ();
	}

	public void removeWidget (final WidgetContainer container)
	{
		this.deleteAppWidgetId (container.getAppWidgetId ());
		this.vgWidgets.removeView (container);
		this.persist ();
	}

	public void persist ()
	{
		this.persistence.save (this.vgWidgets.collectLayouts (null));
	}

	public void showPicker ()
	{
		new WidgetPickerDialog (this.parent, this).show ();
	}

	public void onProviderChosen (final AppWidgetProviderInfo info)
	{
		this.pendingAppWidgetId = this.allocateAppWidgetId ();
		this.pendingInfo = info;

		final boolean bound = this.widgetManager.bindAppWidgetIdIfAllowed (
			this.pendingAppWidgetId, info.getProfile (), info.provider, null);

		if (bound)
		{
			this.configurePendingWidget ();
		}
		else
		{
			final Intent intent = new Intent (AppWidgetManager.ACTION_APPWIDGET_BIND);
			intent.putExtra (AppWidgetManager.EXTRA_APPWIDGET_ID, this.pendingAppWidgetId);
			intent.putExtra (AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider);
			intent.putExtra (AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.getProfile ());

			this.parent.startActivityForResult (intent, RequestCode.WIDGET_BOUND);
		}
	}

	public void onBindResult (final int resultCode)
	{
		if (resultCode == HomeActivity.RESULT_OK)
			this.configurePendingWidget ();
		else
			this.cancelPendingWidget ();
	}

	public void onConfigureResult (final int resultCode)
	{
		if (resultCode == HomeActivity.RESULT_OK)
			this.placePendingWidget ();
		else
			this.cancelPendingWidget ();
	}

	private void configurePendingWidget ()
	{
		if (this.pendingInfo != null && this.pendingInfo.configure != null)
		{
			Log.getInstance ().v (this.getClass ().getSimpleName (), "Widget requires configuration: " + this.pendingAppWidgetId);

			this.startAppWidgetConfigureActivityForResult (
				this.parent, this.pendingAppWidgetId, 0, RequestCode.WIDGET_CONFIGURED, null);
		}
		else
		{
			this.placePendingWidget ();
		}
	}

	private void placePendingWidget ()
	{
		final int appWidgetId = this.pendingAppWidgetId;
		final AppWidgetProviderInfo info = this.pendingInfo;

		this.pendingAppWidgetId = -1;
		this.pendingInfo = null;

		if (appWidgetId == -1 || info == null)
			return;

		final int colSpan = WidgetGrid.spanForSize (info.minWidth, this.vgWidgets.getCellWidth (), WidgetGrid.COLS);
		final int rowSpan = WidgetGrid.spanForSize (info.minHeight, this.vgWidgets.getCellHeight (), WidgetGrid.ROWS);

		final WidgetLayout layout = WidgetGrid.findFreeRect (this.vgWidgets.collectLayouts (null), colSpan, rowSpan);

		if (layout == null)
		{
			this.deleteAppWidgetId (appWidgetId);

			Toast.makeText (this.parent, R.string.widget_no_room, Toast.LENGTH_LONG).show ();

			return;
		}

		layout.appWidgetId = appWidgetId;

		this.addWidget (appWidgetId, layout, true);
	}

	private void cancelPendingWidget ()
	{
		if (this.pendingAppWidgetId != -1)
			this.deleteAppWidgetId (this.pendingAppWidgetId);

		this.pendingAppWidgetId = -1;
		this.pendingInfo = null;
	}
}

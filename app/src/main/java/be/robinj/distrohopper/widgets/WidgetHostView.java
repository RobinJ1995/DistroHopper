package be.robinj.distrohopper.widgets;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.os.Bundle;
import android.util.SizeF;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

/**
 * Created by robin on 8/25/14.
 */
public class WidgetHostView extends AppWidgetHostView
{
	private int longPressTimeout;
	private LongPressCheck longPressCheck;
	private boolean performedLongPress = false;
	private WidgetHost widgetHost;
	private WidgetContainer widgetContainer;

	public WidgetHostView (Context context, WidgetHost widgetHost)
	{
		super (context);

		this.widgetHost = widgetHost;

		this.longPressTimeout = ViewConfiguration.getLongPressTimeout ();
	}

	public void setWidgetContainer (WidgetContainer widgetContainer)
	{
		this.widgetContainer = widgetContainer;
	}

	@Override
	public boolean onInterceptTouchEvent (MotionEvent e)
	{
		if (this.performedLongPress || (this.widgetContainer != null && this.widgetContainer.getEditMode ()))
		{
			this.performedLongPress = false;

			return true;
		}

		switch (e.getAction ())
		{
			case MotionEvent.ACTION_DOWN:
				this.postLongPressCheck ();
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				this.performedLongPress = false;

				if (this.longPressCheck != null)
					this.removeCallbacks (this.longPressCheck);

				break;
		}

		return false;
	}

	@Override
	public void cancelLongPress ()
	{
		super.cancelLongPress ();

		this.performedLongPress = false;
		if (this.longPressCheck != null)
			this.removeCallbacks (this.longPressCheck);
	}

	private void postLongPressCheck ()
	{
		this.performedLongPress = false;

		if (this.longPressCheck == null)
			this.longPressCheck = new LongPressCheck ();

		this.longPressCheck.setNWindowsAttached (this.getWindowAttachCount ());
		this.postDelayed (this.longPressCheck, this.longPressTimeout);
	}

	@Override
	protected void onSizeChanged (final int w, final int h, final int oldw, final int oldh)
	{
		super.onSizeChanged (w, h, oldw, oldh);

		if (w <= 0 || h <= 0)
			return;

		// Tell the provider its actual size so it re-renders for the new bounds
		// (this fires after every committed resize, since the commit relayouts).
		// The activity handles orientation changes itself, so this never goes
		// through an activity recreate. Use the API 31 SizeF list overload,
		// which lets the provider pick the best RemoteViews for the size //
		final float density = this.getResources ().getDisplayMetrics ().density;
		final SizeF size = new SizeF (w / density, h / density);

		// A fresh Bundle, not Bundle.EMPTY: updateAppWidgetSize writes the sizes
		// into the bundle it is given, and Bundle.EMPTY is immutable //
		this.updateAppWidgetSize (new Bundle (), java.util.Collections.singletonList (size));
	}

	@Override
	public int getDescendantFocusability ()
	{
		return ViewGroup.FOCUS_BLOCK_DESCENDANTS;
	}

	class LongPressCheck implements Runnable
	{
		private int nWindowsAttached;

		public void run ()
		{
			if (
				getParent () != null
				&& hasWindowFocus ()
				&& this.nWindowsAttached == getWindowAttachCount ()
				&& ! performedLongPress
			)
			{
				if (performLongClick ())
				{
					performedLongPress = true;
				}
			}
		}

		public void setNWindowsAttached (int n)
		{
			this.nWindowsAttached = n;
		}
	}
}

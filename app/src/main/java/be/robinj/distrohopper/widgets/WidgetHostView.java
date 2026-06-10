package be.robinj.distrohopper.widgets;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
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

		// Keep the provider informed of the widget's actual size; the activity handles
		// orientation changes itself, so this never happens through an activity recreate //
		final float density = this.getResources ().getDisplayMetrics ().density;
		final int wDp = (int) (w / density);
		final int hDp = (int) (h / density);

		this.updateAppWidgetSize (null, wDp, hDp, wDp, hDp);
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

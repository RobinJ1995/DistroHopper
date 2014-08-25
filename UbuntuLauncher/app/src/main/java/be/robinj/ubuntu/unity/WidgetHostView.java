package be.robinj.ubuntu.unity;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/**
 * Created by robin on 8/25/14.
 */
public class WidgetHostView extends AppWidgetHostView
{
	private LayoutInflater inflater;
	private int longPressTimeout;
	private boolean cancelLongPress = false;

	public WidgetHostView (Context context)
	{
		super (context);

		this.inflater = (LayoutInflater) context.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
		this.longPressTimeout = ViewConfiguration.getLongPressTimeout ();
	}

	@Override
	public boolean onInterceptTouchEvent (MotionEvent e)
	{
		switch (e.getAction ())
		{
			case MotionEvent.ACTION_DOWN:
				this.postLongPressCheck ();
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				this.cancelLongPress = true; // performLongClick () gets triggered before this line, because who needs logic anyway //
				break;
		}

		return false;
	}

	@Override
	public void cancelLongPress ()
	{
		super.cancelLongPress ();

		this.cancelLongPress = true;
	}

	private void postLongPressCheck ()
	{
		final int windowAttachCount = this.getWindowAttachCount ();

		Runnable runnable = new Runnable ()
		{
			@Override
			public void run ()
			{
				if (getParent () != null && hasWindowFocus () && windowAttachCount == getWindowAttachCount () && (! cancelLongPress))
					performLongClick ();
				else
					cancelLongPress = false;
			}
		};

		this.postDelayed (runnable, this.longPressTimeout);
	}
}

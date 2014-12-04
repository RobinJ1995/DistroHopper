package be.robinj.ubuntu.widgets;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * Created by robin on 8/25/14.
 */
public class WidgetHostView extends AppWidgetHostView
{
	private LayoutInflater inflater;
	private int longPressTimeout;
	private boolean cancelLongPress = false;

	private boolean editMode = false;

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
				this.cancelLongPress = true;
				break;
			case MotionEvent.ACTION_CANCEL:
				this.cancelLongPress = true;

				return true;
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

	@Override
	protected boolean drawChild (Canvas canvas, View child, long drawingTime)
	{
		boolean returnValue = super.drawChild (canvas, child, drawingTime);

		Paint red = new Paint ();
		red.setColor (Color.RED);
		red.setStrokeWidth (10);

		int width = canvas.getWidth ();
		int height = canvas.getHeight ();

		if (this.editMode)
		{
			canvas.drawLine (20, 20, width - 20, 20, red);
			canvas.drawLine (width - 20, 20, width - 20, height - 20, red);
			canvas.drawLine (width - 20, height - 20, 20, height - 20, red);
			canvas.drawLine (20, height - 20, 20, 20, red);
		}

		return returnValue;
	}

	public boolean getEditMode ()
	{
		return this.editMode;
	}

	public void setEditMode (boolean editMode)
	{
		this.editMode = editMode;

		this.invalidate ();
	}
}

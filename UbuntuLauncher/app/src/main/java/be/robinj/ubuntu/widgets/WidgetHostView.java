package be.robinj.ubuntu.widgets;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

/**
 * Created by robin on 8/25/14.
 */
public class WidgetHostView extends AppWidgetHostView
{
	private LayoutInflater inflater;
	private int longPressTimeout;
	private boolean cancelLongPress = false;
	private ViewGroup.LayoutParams layoutParams;

	private boolean editMode = true;

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
			case MotionEvent.ACTION_HOVER_MOVE:
			case MotionEvent.ACTION_MOVE:
				if (this.editMode)
				{
					MotionEvent.PointerCoords firstCoords = new MotionEvent.PointerCoords ();
					MotionEvent.PointerCoords latestCoords = new MotionEvent.PointerCoords ();
					e.getPointerCoords (0, firstCoords);
					e.getPointerCoords (e.getPointerCount () - 1, latestCoords);

					if (firstCoords.x != latestCoords.x && firstCoords.x > this.getWidth () - 250)
						this.getLayoutParams ().width = (int) latestCoords.x;

					this.invalidate ();
				}
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

	@Override
	protected boolean drawChild (Canvas canvas, View child, long drawingTime)
	{
		boolean returnValue = super.drawChild (canvas, child, drawingTime);

		Paint red = new Paint ();
		red.setColor (Color.RED);
		red.setStrokeWidth (10);

		Paint paintOverlay = new Paint ();
		paintOverlay.setColor (Color.argb (50, 0, 0, 0));

		Paint paintOverlayLight = new Paint ();
		paintOverlayLight.setColor (Color.argb (50, 255, 255, 255));

		int width = canvas.getWidth ();
		int height = canvas.getHeight ();

		if (this.editMode)
		{
			canvas.drawRect (0, 0, width, height, paintOverlay);

			RectF smallTop = new RectF (width / 2 - 25, -5, width / 2 + 25, 45);
			RectF bigTop = new RectF (width / 2 - 75, -35, width / 2 + 75, 115);
			RectF smallRight = new RectF (width - 45, height / 2 - 25, width + 5, height / 2 + 25);
			RectF bigRight = new RectF (width - 115, height / 2 - 75, width + 35, height / 2 + 75);
			RectF smallBottom = new RectF (width / 2 - 25, height - 45, width / 2 + 25, height + 5);
			RectF bigBottom = new RectF (width / 2 - 75, height - 115, width / 2 + 75, height + 35);
			RectF smallLeft = new RectF (-5, height / 2 - 25, 45, height / 2 + 25);
			RectF bigLeft = new RectF (-35, height / 2 - 75, 115, height / 2 + 75);

			canvas.drawArc (bigTop, 0, 360, true, paintOverlay);
			canvas.drawArc (smallTop, 0, 360, true, paintOverlayLight);
			canvas.drawArc (bigRight, 0, 360, true, paintOverlay);
			canvas.drawArc (smallRight, 0, 360, true, paintOverlayLight);
			canvas.drawArc (bigBottom, 0, 360, true, paintOverlay);
			canvas.drawArc (smallBottom, 0, 360, true, paintOverlayLight);
			canvas.drawArc (bigLeft, 0, 360, true, paintOverlay);
			canvas.drawArc (smallLeft, 0, 360, true, paintOverlayLight);
		}

		return returnValue;
	}

	public boolean getEditMode ()
	{
		return this.editMode;
	}

	public void setEditMode (boolean editMode)
	{
		this.editMode =true;// editMode;

		this.invalidate ();
	}
}

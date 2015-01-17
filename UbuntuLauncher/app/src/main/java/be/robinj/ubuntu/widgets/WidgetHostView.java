package be.robinj.ubuntu.widgets;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.Date;

import be.robinj.ubuntu.HomeActivity;

/**
 * Created by robin on 8/25/14.
 */
public class WidgetHostView extends AppWidgetHostView
{
	private LayoutInflater inflater;
	private int longPressTimeout;
	private LongPressCheck longPressCheck;
	private boolean performedLongPress = false;
	private ViewGroup.LayoutParams layoutParams;
	private HomeActivity parent;

	private boolean editMode = false;

	public WidgetHostView (Context context, HomeActivity parent)
	{
		super (context);

		this.parent = parent;

		this.inflater = (LayoutInflater) context.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
		this.longPressTimeout = ViewConfiguration.getLongPressTimeout ();
	}

	@Override
	public boolean onInterceptTouchEvent (MotionEvent e)
	{
		if (this.performedLongPress)
		{
			this.performedLongPress = false;

			return true;
		}

		switch (e.getAction ())
		{
			case MotionEvent.ACTION_DOWN:
				this.postLongPressCheck ();
				break;
			case MotionEvent.ACTION_MOVE:
				if (this.editMode)
				{
					int width = this.getWidth ();
					int height = this.getHeight ();

					RectF bigTop = new RectF (width / 2 - 75, -35, width / 2 + 75, 115);
					RectF bigRight = new RectF (width - 115, height / 2 - 75, width + 35, height / 2 + 75);
					RectF bigBottom = new RectF (width / 2 - 75, height - 115, width / 2 + 75, height + 35);
					RectF bigLeft = new RectF (-35, height / 2 - 75, 115, height / 2 + 75);

					RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.getLayoutParams ();

					if (bigRight.contains (e.getX (), e.getY ()))
						layoutParams.width = (int) e.getX (e.getPointerCount () - 1);

					this.requestLayout ();
				}

				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				this.performedLongPress = false;

				if (this.longPressCheck != null)
					this.removeCallbacks (this.longPressCheck);

				if (this.editMode)
				{
					int width = this.getWidth ();
					int height = this.getHeight ();

					RectF bigMiddle = new RectF (width / 2 - 53, height / 2 - 53, width / 2 + 53, height / 2 + 53);
					if (bigMiddle.contains (e.getX (), e.getY ()))
						this.parent.removeWidget (this);

					return true;
				}
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

	@Override
	protected boolean drawChild (Canvas canvas, View child, long drawingTime)
	{
		boolean returnValue = super.drawChild (canvas, child, drawingTime);

		Paint paintOverlay = new Paint ();
		paintOverlay.setColor (Color.argb (50, 0, 0, 0));

		Paint paintOverlayLight = new Paint ();
		paintOverlayLight.setColor (Color.argb (50, 255, 255, 255));

		Paint paintLight = new Paint ();
		paintLight.setColor (Color.argb (200, 255, 255, 255));

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
			RectF smallMiddle = new RectF (width / 2 - 50, height / 2 - 50, width / 2 + 50, height / 2 + 50);
			RectF bigMiddle = new RectF (width / 2 - 53, height / 2 - 53, width / 2 + 53, height / 2 + 53);

			canvas.drawArc (bigTop, 0, 360, true, paintOverlay);
			canvas.drawArc (smallTop, 0, 360, true, paintOverlayLight);
			canvas.drawArc (bigRight, 0, 360, true, paintOverlay);
			canvas.drawArc (smallRight, 0, 360, true, paintOverlayLight);
			canvas.drawArc (bigBottom, 0, 360, true, paintOverlay);
			canvas.drawArc (smallBottom, 0, 360, true, paintOverlayLight);
			canvas.drawArc (bigLeft, 0, 360, true, paintOverlay);
			canvas.drawArc (smallLeft, 0, 360, true, paintOverlayLight);
			canvas.drawArc (bigMiddle, 0, 360, true, paintOverlayLight);
			canvas.drawArc (smallMiddle, 0, 360, true, paintOverlay);
			canvas.drawArc (smallMiddle, 0, 360, true, paintOverlay);
			canvas.drawLine (smallMiddle.left + smallMiddle.width () / 4, smallMiddle.top + smallMiddle.height () / 4, smallMiddle.right - smallMiddle.width () / 4, smallMiddle.bottom - smallMiddle.height () / 4, paintLight);
			canvas.drawLine (smallMiddle.left + smallMiddle.width () / 4, smallMiddle.bottom - smallMiddle.height () / 4, smallMiddle.right - smallMiddle.width () / 4, smallMiddle.top + smallMiddle.height () / 4, paintLight);
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

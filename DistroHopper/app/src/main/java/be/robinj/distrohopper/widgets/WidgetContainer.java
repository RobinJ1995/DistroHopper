package be.robinj.distrohopper.widgets;

import android.app.Service;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import be.robinj.distrohopper.R;

/**
 * Created by robin on 18/01/15.
 */
public class WidgetContainer extends FrameLayout implements View.OnTouchListener
{
	private WidgetHostView widget;
	private FrameLayout container;
	private ViewGroup overlay;
	private boolean editMode = false;
	private View view;

	protected WidgetContainer (Context context, AttributeSet attrs, WidgetHostView widget)
	{
		super (context, attrs);

		widget.setWidgetContainer (this);
		this.widget = widget;

		LayoutInflater inflater = (LayoutInflater) context.getSystemService (Service.LAYOUT_INFLATER_SERVICE);
		this.view = inflater.inflate (R.layout.widget_container, this, true);

		FrameLayout widgetContainer = (FrameLayout) this.findViewById (R.id.widgetContainer);
		widgetContainer.addView (widget);
		this.container = widgetContainer;
		this.overlay = (ViewGroup) this.findViewById (R.id.widgetOverlayCenter);
		ImageButton ibRemove = (ImageButton) this.findViewById (R.id.ibRemove);

		ViewGroup llEdgeTop = (ViewGroup) this.findViewById (R.id.llEdgeTop);
		ViewGroup llEdgeRight = (ViewGroup) this.findViewById (R.id.llEdgeRight);
		ViewGroup llEdgeBottom = (ViewGroup) this.findViewById (R.id.llEdgeBottom);
		ViewGroup llEdgeLeft = (ViewGroup) this.findViewById (R.id.llEdgeLeft);

		llEdgeTop.setOnTouchListener (this);
		llEdgeRight.setOnTouchListener (this);
		llEdgeBottom.setOnTouchListener (this);
		llEdgeLeft.setOnTouchListener (this);
		ibRemove.setOnClickListener (new WidgetContainerRemove_ClickListener (this));
	}

	public boolean getEditMode ()
	{
		return this.editMode;
	}

	public void setEditMode (boolean editMode)
	{
		this.editMode = editMode;

		if (Build.VERSION.SDK_INT >= 11)
			this.container.setAlpha (editMode ? 0.8F : 1.0F);

		this.overlay.setVisibility (editMode ? VISIBLE : GONE);
		this.container.setVisibility (editMode ? GONE : VISIBLE);
		this.widget.invalidate ();
	}

	public void removeWidget ()
	{
		((ViewGroup) this.view.getParent ()).removeView (this.view);
	}

	@Override
	public boolean onTouch (View view, MotionEvent e)
	{
		int id = view.getId ();

		RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.getLayoutParams ();

		if (id == R.id.llEdgeRight)
		{
			if (e.getAction () == MotionEvent.ACTION_MOVE)
			{
				layoutParams.width = (int) e.getX (e.getPointerCount () - 1) + (view.getWidth () / 2);
			}
		}

		this.requestLayout ();

		return false;
	}
}

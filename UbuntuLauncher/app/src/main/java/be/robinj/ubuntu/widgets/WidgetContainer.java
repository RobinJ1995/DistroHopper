package be.robinj.ubuntu.widgets;

import android.app.Service;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import be.robinj.ubuntu.R;
import be.robinj.ubuntu.unity.AppIcon;

/**
 * Created by robin on 18/01/15.
 */
public class WidgetContainer extends FrameLayout
{
	private WidgetHostView widget;
	private FrameLayout container;
	private ViewGroup overlay;
	private boolean editMode = false;

	protected WidgetContainer (Context context, AttributeSet attrs, WidgetHostView widget)
	{
		super (context, attrs);

		widget.setWidgetContainer (this);
		this.widget = widget;

		LayoutInflater inflater = (LayoutInflater) context.getSystemService (Service.LAYOUT_INFLATER_SERVICE);
		inflater.inflate (R.layout.widget_container, this, true);

		FrameLayout widgetContainer = (FrameLayout) this.findViewById (R.id.widgetContainer);
		widgetContainer.addView (widget);
		this.container = widgetContainer;
		this.overlay = (ViewGroup) this.findViewById (R.id.widgetOverlay);
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
		this.widget.invalidate ();
	}
}

package be.robinj.distrohopper.widgets;

import android.app.Service;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import be.robinj.distrohopper.R;

/**
 * Created by robin on 18/01/15.
 */
public class WidgetContainer extends FrameLayout implements View.OnTouchListener
{
	private final WidgetHost widgetHost;
	private final WidgetHostView widget;
	private final FrameLayout container;
	private final ViewGroup overlay;
	private final ViewGroup llEdgeTop;
	private final ViewGroup llEdgeRight;
	private final ViewGroup llEdgeBottom;
	private final ViewGroup llEdgeLeft;
	private final int touchSlop;
	private boolean editMode = false;

	// Touch state for moving/resizing //
	private float startRawX;
	private float startRawY;
	private int startLeft;
	private int startTop;
	private int startWidth;
	private int startHeight;
	private boolean dragging = false;

	protected WidgetContainer (Context context, WidgetHost widgetHost, WidgetHostView widget)
	{
		super (context);

		this.widgetHost = widgetHost;

		widget.setWidgetContainer (this);
		this.widget = widget;

		LayoutInflater inflater = (LayoutInflater) context.getSystemService (Service.LAYOUT_INFLATER_SERVICE);
		inflater.inflate (R.layout.widget_container, this, true);

		this.container = this.findViewById (R.id.widgetContainer);
		this.container.addView (widget);
		this.overlay = this.findViewById (R.id.widgetOverlayCenter);
		ImageButton ibRemove = this.findViewById (R.id.ibRemove);

		this.llEdgeTop = this.findViewById (R.id.llEdgeTop);
		this.llEdgeRight = this.findViewById (R.id.llEdgeRight);
		this.llEdgeBottom = this.findViewById (R.id.llEdgeBottom);
		this.llEdgeLeft = this.findViewById (R.id.llEdgeLeft);

		this.llEdgeTop.setOnTouchListener (this);
		this.llEdgeRight.setOnTouchListener (this);
		this.llEdgeBottom.setOnTouchListener (this);
		this.llEdgeLeft.setOnTouchListener (this);
		this.overlay.setOnTouchListener (this);
		ibRemove.setOnClickListener (new WidgetContainerRemove_ClickListener (this));

		this.touchSlop = ViewConfiguration.get (context).getScaledTouchSlop ();
	}

	public int getAppWidgetId ()
	{
		return this.widget.getAppWidgetId ();
	}

	public boolean getEditMode ()
	{
		return this.editMode;
	}

	public void setEditMode (boolean editMode)
	{
		if (editMode && this.getParent () instanceof WidgetsContainer)
			((WidgetsContainer) this.getParent ()).exitEditMode (); // Only one widget in edit mode at a time //

		this.editMode = editMode;

		this.container.setAlpha (editMode ? 0.8F : 1.0F);
		this.overlay.setVisibility (editMode ? VISIBLE : GONE);
		this.llEdgeTop.setVisibility (editMode ? VISIBLE : GONE);
		this.llEdgeRight.setVisibility (editMode ? VISIBLE : GONE);
		this.llEdgeBottom.setVisibility (editMode ? VISIBLE : GONE);
		this.llEdgeLeft.setVisibility (editMode ? VISIBLE : GONE);
	}

	public void removeWidget ()
	{
		this.widgetHost.removeWidget (this);
	}

	@Override
	public boolean onTouch (View view, MotionEvent e)
	{
		if (! (this.getParent () instanceof WidgetsContainer))
			return false;

		final WidgetsContainer parent = (WidgetsContainer) this.getParent ();
		final WidgetsContainer.LayoutParams lp = (WidgetsContainer.LayoutParams) this.getLayoutParams ();
		final int id = view.getId ();

		switch (e.getActionMasked ())
		{
			case MotionEvent.ACTION_DOWN:
				this.startRawX = e.getRawX ();
				this.startRawY = e.getRawY ();
				this.startLeft = this.getLeft ();
				this.startTop = this.getTop ();
				this.startWidth = this.getWidth ();
				this.startHeight = this.getHeight ();
				this.dragging = false;

				return true;
			case MotionEvent.ACTION_MOVE:
				final int dx = (int) (e.getRawX () - this.startRawX);
				final int dy = (int) (e.getRawY () - this.startRawY);

				if (! this.dragging)
				{
					if (Math.abs (dx) < this.touchSlop && Math.abs (dy) < this.touchSlop)
						return true;

					this.dragging = true;
					lp.previewLeftPx = this.startLeft;
					lp.previewTopPx = this.startTop;
					lp.previewWidthPx = this.startWidth;
					lp.previewHeightPx = this.startHeight;
				}

				final int minSize = Math.min (parent.getCellWidth (), parent.getCellHeight ()) / 2;

				if (id == R.id.widgetOverlayCenter)
				{
					lp.previewLeftPx = this.startLeft + dx;
					lp.previewTopPx = this.startTop + dy;
				}
				else if (id == R.id.llEdgeRight)
				{
					lp.previewWidthPx = Math.max (minSize, this.startWidth + dx);
				}
				else if (id == R.id.llEdgeBottom)
				{
					lp.previewHeightPx = Math.max (minSize, this.startHeight + dy);
				}
				else if (id == R.id.llEdgeLeft)
				{
					final int clampedDx = Math.min (dx, this.startWidth - minSize);
					lp.previewLeftPx = this.startLeft + clampedDx;
					lp.previewWidthPx = this.startWidth - clampedDx;
				}
				else if (id == R.id.llEdgeTop)
				{
					final int clampedDy = Math.min (dy, this.startHeight - minSize);
					lp.previewTopPx = this.startTop + clampedDy;
					lp.previewHeightPx = this.startHeight - clampedDy;
				}

				parent.requestLayout ();

				return true;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				if (this.dragging)
				{
					this.dragging = false;

					if (e.getActionMasked () == MotionEvent.ACTION_UP)
						this.commitPreview (parent, lp);

					lp.clearPreview ();
					parent.requestLayout ();
				}

				return true;
		}

		return false;
	}

	/**
	 * Snap the pixel preview position back to grid cells; keep it only if it fits.
	 */
	private void commitPreview (final WidgetsContainer parent, final WidgetsContainer.LayoutParams lp)
	{
		final int cellWidth = parent.getCellWidth ();
		final int cellHeight = parent.getCellHeight ();

		if (cellWidth <= 0 || cellHeight <= 0)
			return;

		final int col = WidgetGrid.snapToCell (lp.previewLeftPx - parent.getPaddingLeft (), cellWidth, WidgetGrid.COLS - 1);
		final int row = WidgetGrid.snapToCell (lp.previewTopPx - parent.getPaddingTop (), cellHeight, WidgetGrid.ROWS - 1);
		final int colEnd = WidgetGrid.snapToCell (lp.previewLeftPx + lp.previewWidthPx - parent.getPaddingLeft (), cellWidth, WidgetGrid.COLS);
		final int rowEnd = WidgetGrid.snapToCell (lp.previewTopPx + lp.previewHeightPx - parent.getPaddingTop (), cellHeight, WidgetGrid.ROWS);

		final WidgetLayout candidate = new WidgetLayout (
			this.getAppWidgetId (), col, row, Math.max (1, colEnd - col), Math.max (1, rowEnd - row));

		if (! WidgetGrid.fits (parent.collectLayouts (this), candidate))
			return; // Revert to the previous position //

		lp.col = candidate.col;
		lp.row = candidate.row;
		lp.colSpan = candidate.colSpan;
		lp.rowSpan = candidate.rowSpan;

		this.widgetHost.persist ();
	}
}

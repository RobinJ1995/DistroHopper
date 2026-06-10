package be.robinj.distrohopper.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * The widget area of the home screen. Positions its children on an invisible
 * {@link WidgetGrid#COLS} x {@link WidgetGrid#ROWS} grid spanning the part of
 * the screen that is not covered by the launcher or the panel (the covered
 * strips are excluded via padding set by HomeActivity).
 */
public class WidgetsContainer extends ViewGroup
{
	public WidgetsContainer (Context context, AttributeSet attrs)
	{
		super (context, attrs);
	}

	public int getCellWidth ()
	{
		return (this.getMeasuredWidth () - this.getPaddingLeft () - this.getPaddingRight ()) / WidgetGrid.COLS;
	}

	public int getCellHeight ()
	{
		return (this.getMeasuredHeight () - this.getPaddingTop () - this.getPaddingBottom ()) / WidgetGrid.ROWS;
	}

	@Override
	protected void onMeasure (final int widthMeasureSpec, final int heightMeasureSpec)
	{
		this.setMeasuredDimension (
			getDefaultSize (this.getSuggestedMinimumWidth (), widthMeasureSpec),
			getDefaultSize (this.getSuggestedMinimumHeight (), heightMeasureSpec));

		final int cellWidth = this.getCellWidth ();
		final int cellHeight = this.getCellHeight ();

		for (int i = 0; i < this.getChildCount (); i++)
		{
			final View child = this.getChildAt (i);

			if (child.getVisibility () == GONE)
				continue;

			final LayoutParams lp = (LayoutParams) child.getLayoutParams ();
			final int width = lp.previewWidthPx >= 0 ? lp.previewWidthPx : lp.colSpan * cellWidth;
			final int height = lp.previewHeightPx >= 0 ? lp.previewHeightPx : lp.rowSpan * cellHeight;

			child.measure (
				MeasureSpec.makeMeasureSpec (width, MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec (height, MeasureSpec.EXACTLY));
		}
	}

	@Override
	protected void onLayout (final boolean changed, final int l, final int t, final int r, final int b)
	{
		final int cellWidth = this.getCellWidth ();
		final int cellHeight = this.getCellHeight ();

		for (int i = 0; i < this.getChildCount (); i++)
		{
			final View child = this.getChildAt (i);

			if (child.getVisibility () == GONE)
				continue;

			final LayoutParams lp = (LayoutParams) child.getLayoutParams ();
			final int left = lp.previewLeftPx >= 0 ? lp.previewLeftPx : this.getPaddingLeft () + lp.col * cellWidth;
			final int top = lp.previewTopPx >= 0 ? lp.previewTopPx : this.getPaddingTop () + lp.row * cellHeight;

			child.layout (left, top, left + child.getMeasuredWidth (), top + child.getMeasuredHeight ());
		}
	}

	/**
	 * Grid placements of all child widgets, optionally excluding one (e.g. the one being moved).
	 */
	public List<WidgetLayout> collectLayouts (final View exclude)
	{
		final List<WidgetLayout> layouts = new ArrayList<> ();

		for (int i = 0; i < this.getChildCount (); i++)
		{
			final View child = this.getChildAt (i);

			if (child == exclude || ! (child instanceof WidgetContainer))
				continue;

			final LayoutParams lp = (LayoutParams) child.getLayoutParams ();

			layouts.add (new WidgetLayout (
				((WidgetContainer) child).getAppWidgetId (), lp.col, lp.row, lp.colSpan, lp.rowSpan));
		}

		return layouts;
	}

	public void exitEditMode ()
	{
		for (int i = 0; i < this.getChildCount (); i++)
		{
			final View child = this.getChildAt (i);

			if (child instanceof WidgetContainer && ((WidgetContainer) child).getEditMode ())
				((WidgetContainer) child).setEditMode (false);
		}
	}

	public boolean hasEditModeChild ()
	{
		for (int i = 0; i < this.getChildCount (); i++)
		{
			final View child = this.getChildAt (i);

			if (child instanceof WidgetContainer && ((WidgetContainer) child).getEditMode ())
				return true;
		}

		return false;
	}

	@Override
	protected ViewGroup.LayoutParams generateDefaultLayoutParams ()
	{
		return new LayoutParams (0, 0, 1, 1);
	}

	@Override
	public ViewGroup.LayoutParams generateLayoutParams (final AttributeSet attrs)
	{
		return new LayoutParams (this.getContext (), attrs);
	}

	@Override
	protected ViewGroup.LayoutParams generateLayoutParams (final ViewGroup.LayoutParams p)
	{
		return p instanceof LayoutParams ? p : this.generateDefaultLayoutParams ();
	}

	@Override
	protected boolean checkLayoutParams (final ViewGroup.LayoutParams p)
	{
		return p instanceof LayoutParams;
	}

	public static class LayoutParams extends ViewGroup.MarginLayoutParams
	{
		public int col;
		public int row;
		public int colSpan;
		public int rowSpan;

		// Pixel overrides used while the widget is being dragged or resized; -1 means "use the grid cells" //
		public int previewLeftPx = -1;
		public int previewTopPx = -1;
		public int previewWidthPx = -1;
		public int previewHeightPx = -1;

		public LayoutParams (final int col, final int row, final int colSpan, final int rowSpan)
		{
			super (MATCH_PARENT, MATCH_PARENT);

			this.col = col;
			this.row = row;
			this.colSpan = colSpan;
			this.rowSpan = rowSpan;
		}

		public LayoutParams (final WidgetLayout layout)
		{
			this (layout.col, layout.row, layout.colSpan, layout.rowSpan);
		}

		public LayoutParams (final Context context, final AttributeSet attrs)
		{
			super (context, attrs);

			this.col = 0;
			this.row = 0;
			this.colSpan = 1;
			this.rowSpan = 1;
		}

		public void clearPreview ()
		{
			this.previewLeftPx = -1;
			this.previewTopPx = -1;
			this.previewWidthPx = -1;
			this.previewHeightPx = -1;
		}
	}
}

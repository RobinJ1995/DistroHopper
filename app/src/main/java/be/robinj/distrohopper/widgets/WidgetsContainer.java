package be.robinj.distrohopper.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import be.robinj.distrohopper.R;
import be.robinj.distrohopper.preferences.Preference;
import be.robinj.distrohopper.preferences.Preferences;

/**
 * The widget area of the home screen. Positions its children on an invisible
 * {@link WidgetGrid#COLS} x {@link WidgetGrid#ROWS} grid spanning the part of
 * the screen that is not covered by the launcher or the panel (the covered
 * strips are excluded via padding set by HomeActivity).
 */
public class WidgetsContainer extends ViewGroup
{
	private final Paint snapLinePaint = new Paint (Paint.ANTI_ALIAS_FLAG);
	private final Paint moveTargetFillPaint = new Paint (Paint.ANTI_ALIAS_FLAG);
	private final Paint moveTargetStrokePaint = new Paint (Paint.ANTI_ALIAS_FLAG);
	private final Paint gridDotPaint = new Paint (Paint.ANTI_ALIAS_FLAG);
	private final float gridDotRadius;

	// When the developer option is on, dots are drawn at every grid intersection
	// while a widget or app is being dragged or a widget is being resized //
	private boolean gridOverlayVisible = false;

	// While an edge is being dragged, the line shows where it will snap on release //
	private boolean snapLineVisible = false;
	private boolean snapLineVertical = false;
	private float snapLinePos;
	private float snapLineFrom;
	private float snapLineTo;
	private boolean moveTargetVisible = false;
	private int moveTargetCol;
	private int moveTargetRow;
	private int moveTargetColSpan;
	private int moveTargetRowSpan;

	public WidgetsContainer (Context context, AttributeSet attrs)
	{
		super (context, attrs);

		this.snapLinePaint.setColor (context.getColor (R.color.transparent50));
		this.snapLinePaint.setStrokeWidth (TypedValue.applyDimension (
			TypedValue.COMPLEX_UNIT_DIP, 2, context.getResources ().getDisplayMetrics ()));
		this.snapLinePaint.setStrokeCap (Paint.Cap.ROUND);
		this.moveTargetFillPaint.setColor (Color.argb (48, 255, 255, 255));
		this.moveTargetStrokePaint.setColor (Color.argb (190, 255, 255, 255));
		this.moveTargetStrokePaint.setStyle (Paint.Style.STROKE);
		this.moveTargetStrokePaint.setStrokeWidth (TypedValue.applyDimension (
				TypedValue.COMPLEX_UNIT_DIP, 2, context.getResources ().getDisplayMetrics ()));
		this.gridDotPaint.setColor (Color.argb (190, 255, 255, 255));
		this.gridDotPaint.setStyle (Paint.Style.FILL);
		this.gridDotRadius = TypedValue.applyDimension (
				TypedValue.COMPLEX_UNIT_DIP, 2.5f, context.getResources ().getDisplayMetrics ());
	}

	/**
	 * Shows the grid-intersection dot overlay, if the developer option is enabled.
	 * No-op otherwise, so callers can fire it unconditionally on every drag/resize.
	 */
	public void showGridOverlay ()
	{
		if (this.gridOverlayVisible || ! this.isGridOverlayEnabled ())
			return;

		this.gridOverlayVisible = true;
		this.invalidate ();
	}

	public void hideGridOverlay ()
	{
		if (! this.gridOverlayVisible)
			return;

		this.gridOverlayVisible = false;
		this.invalidate ();
	}

	boolean isGridOverlayVisible ()
	{
		return this.gridOverlayVisible;
	}

	private boolean isGridOverlayEnabled ()
	{
		return Preferences.getSharedPreferences (this.getContext ())
				.getBoolean (Preference.DEV_SHOW_GRID_ON_DRAG.getName (), false);
	}

	public void showMoveTarget (final int col, final int row, final int colSpan, final int rowSpan,
			final boolean valid)
	{
		this.moveTargetVisible = true;
		this.moveTargetCol = col;
		this.moveTargetRow = row;
		this.moveTargetColSpan = colSpan;
		this.moveTargetRowSpan = rowSpan;
		this.moveTargetFillPaint.setColor (valid
				? Color.argb (48, 255, 255, 255)
				: Color.argb (64, 255, 40, 40));
		this.moveTargetStrokePaint.setColor (valid
				? Color.argb (190, 255, 255, 255)
				: Color.argb (220, 255, 40, 40));

		this.invalidate ();
	}

	public void hideMoveTarget ()
	{
		if (! this.moveTargetVisible)
			return;

		this.moveTargetVisible = false;
		this.invalidate ();
	}

	boolean isMoveTargetVisible ()
	{
		return this.moveTargetVisible;
	}

	int getMoveTargetCol ()
	{
		return this.moveTargetCol;
	}

	int getMoveTargetRow ()
	{
		return this.moveTargetRow;
	}

	public void showSnapLine (final boolean vertical, final float pos, final float from, final float to)
	{
		this.snapLineVisible = true;
		this.snapLineVertical = vertical;
		this.snapLinePos = pos;
		this.snapLineFrom = from;
		this.snapLineTo = to;

		this.invalidate ();
	}

	public void hideSnapLine ()
	{
		if (! this.snapLineVisible)
			return;

		this.snapLineVisible = false;

		this.invalidate ();
	}

	@Override
	protected void dispatchDraw (final Canvas canvas)
	{
		super.dispatchDraw (canvas);

		if (this.gridOverlayVisible)
		{
			final int cellWidth = this.getCellWidth ();
			final int cellHeight = this.getCellHeight ();

			for (int col = 0; col <= WidgetGrid.COLS; col++)
			{
				final float x = this.getPaddingLeft () + col * cellWidth;

				for (int row = 0; row <= WidgetGrid.ROWS; row++)
				{
					final float y = this.getPaddingTop () + row * cellHeight;
					canvas.drawCircle (x, y, this.gridDotRadius, this.gridDotPaint);
				}
			}
		}

		if (this.moveTargetVisible)
		{
			final int cellWidth = this.getCellWidth ();
			final int cellHeight = this.getCellHeight ();
			final float radius = TypedValue.applyDimension (
					TypedValue.COMPLEX_UNIT_DIP, 12, this.getResources ().getDisplayMetrics ());
			final RectF target = new RectF (
					this.getPaddingLeft () + this.moveTargetCol * cellWidth,
					this.getPaddingTop () + this.moveTargetRow * cellHeight,
					this.getPaddingLeft () + (this.moveTargetCol + this.moveTargetColSpan) * cellWidth,
					this.getPaddingTop () + (this.moveTargetRow + this.moveTargetRowSpan) * cellHeight);

			canvas.drawRoundRect (target, radius, radius, this.moveTargetFillPaint);
			canvas.drawRoundRect (target, radius, radius, this.moveTargetStrokePaint);
		}

		// On top of the children, so the line stays visible over the dragged widget //
		if (! this.snapLineVisible)
			return;

		if (this.snapLineVertical)
			canvas.drawLine (this.snapLinePos, this.snapLineFrom, this.snapLinePos, this.snapLineTo, this.snapLinePaint);
		else
			canvas.drawLine (this.snapLineFrom, this.snapLinePos, this.snapLineTo, this.snapLinePos, this.snapLinePaint);
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

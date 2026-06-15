package be.robinj.distrohopper.desktop.launcher;

import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import be.robinj.distrohopper.AppManager;
import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.R;
import be.robinj.distrohopper.desktop.dash.DashDragPayload;
import be.robinj.distrohopper.desktop.launcher.PinnedAppsBar;
import be.robinj.distrohopper.widgets.DesktopAppView;
import be.robinj.distrohopper.widgets.WidgetContainer;

/**
 * The launcher bar's drag handler, attached to the whole launcher container.
 *
 * Reorder and fold are driven from here at the CONTAINER level — hit-testing
 * which pinned icon/folder is under the drag on each ACTION_DRAG_LOCATION —
 * rather than from per-icon listeners: a nested child view reliably receives
 * only ACTION_DRAG_STARTED/ENDED, never the LOCATION/ENTERED/DROP in between
 * (the container gets those), so a per-icon dwell-fold never armed. This mirrors
 * the dash grid's single-listener approach.
 *
 * Created by robin on 03/09/14.
 */
public class LauncherDragListener implements ViewGroup.OnDragListener
{
	/** Fraction of an icon's main-axis extent that counts as its "fold" centre. */
	private static final float FOLD_ZONE_LO = 0.3f;
	private static final float FOLD_ZONE_HI = 0.7f;

	private AppManager appManager;
	// Last resolved drag intent, so the preview is only updated when it changes
	// (a fold onto lastFoldTarget, else an insert before/after lastInsertTarget) //
	private View lastFoldTarget = null;
	private View lastInsertTarget = null;
	private boolean lastInsertAfter = false;
	private boolean lastWasInsert = false;

	public LauncherDragListener (AppManager appManager)
	{
		this.appManager = appManager;
	}

	private void resetDragIntent ()
	{
		this.lastFoldTarget = null;
		this.lastInsertTarget = null;
		this.lastInsertAfter = false;
		this.lastWasInsert = false;
	}

	@Override
	public boolean onDrag (View view, DragEvent event)
	{
		try
		{
			// Widget drags are handled by WidgetsContainer_DragListener and the trash's
			// own listener; reacting here would hide the trash mid-drag //
			if (event.getLocalState () instanceof WidgetContainer)
				return false;

			// Dash folders / folder members never pin to the launcher (a folder
			// can't leave the dash); let the dash grid listener handle them //
			if (event.getLocalState () instanceof DashDragPayload)
				return false;

			switch (event.getAction ())
			{
				case DragEvent.ACTION_DRAG_ENTERED:
					this.appManager.startedDraggingPinnedApp ();
					// Cross-surface drag: hovering the launcher asks for the dash to
					// close (resolved with BFB-open precedence by the controller), so
					// the app can continue onto the desktop; dropping here still pins //
					this.appManager.getParent ().getDashCrossSurface ()
						.entered (view.getId (), false);
					break;
				case DragEvent.ACTION_DRAG_LOCATION:
					// Resolve the drag spatially (container-level; nested icons don't
					// get LOCATION). Over an icon's centre folds (with a ring); over a
					// gap/edge opens space to pin there //
					this.resolvePinnedDrag (view, event);
					break;
				case DragEvent.ACTION_DROP:
					// A drop on the bar itself — most often on the empty slot kept
					// open for the dragged icon — commits a dwell-armed fold, else the
					// previewed order. A desktop app rode in on a dash-style
					// placeholder, so the same commit pins it to the bar; then remove
					// it from the desktop to complete the move //
					if (! this.appManager.dropPinnedFold ())
						this.appManager.droppedPinnedApp ();
					if (event.getLocalState () instanceof DesktopAppView
							&& this.appManager.getParent ().getDesktopAppHost () != null)
						this.appManager.getParent ().getDesktopAppHost ()
							.remove ((DesktopAppView) event.getLocalState ());
					this.appManager.stoppedDraggingPinnedApp ();
					break;
				// No ACTION_DRAG_EXITED case: spurious exits fire while the icons
				// animate out of the way (and when hovering the trash), briefly
				// flickering the bar out of drag mode; ENDED always follows anyway //
				case DragEvent.ACTION_DRAG_ENDED:
					this.appManager.cancelPinnedFold ();
					this.resetDragIntent ();
					this.appManager.getParent ().getDashCrossSurface ()
						.exited (view.getId (), false);
					// Restores the bar if the drag ended without a drop on it.
					// Posted: mutating views (even just visibility) during ENDED
					// dispatch throws a ConcurrentModificationException //
					view.post (() ->
					{
						this.appManager.endedDraggingPinnedApp ();
						this.appManager.stoppedDraggingPinnedApp ();
					});
					break;
			}
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this.appManager.getContext ());
		}

		return true;
	}

	/**
	 * Resolves the drag's position over the pinned bar into a fold (over an icon's
	 * centre) or an insertion (over a gap/edge), and previews it — only when the
	 * resolved intent changes, so the placeholder/ring don't thrash every frame.
	 *
	 * Coordinates are converted to screen space (correct regardless of how deeply
	 * the bar is nested, and of the launcher's edge/orientation). The dragged
	 * item's own placeholder is INVISIBLE and skipped, so a fold never targets it.
	 */
	private void resolvePinnedDrag (View launcher, DragEvent event)
	{
		final PinnedAppsBar bar = launcher.findViewById (R.id.llLauncherPinnedApps);
		if (bar == null)
			return;

		final boolean vertical = bar.getOrientation () == LinearLayout.VERTICAL;
		final int[] launcherLoc = new int[2];
		launcher.getLocationOnScreen (launcherLoc);
		final int coord = vertical
			? launcherLoc[1] + (int) event.getY ()
			: launcherLoc[0] + (int) event.getX ();

		View over = null;
		float frac = 0f;
		View firstIcon = null, lastIcon = null;
		int firstStart = Integer.MAX_VALUE, lastEnd = Integer.MIN_VALUE;

		final int[] childLoc = new int[2];
		for (int i = 0; i < bar.getChildCount (); i++)
		{
			final View child = bar.getChildAt (i);
			if (child.getVisibility () != View.VISIBLE)
				continue; // the dragged item's own placeholder //

			child.getLocationOnScreen (childLoc);
			final int start = vertical ? childLoc[1] : childLoc[0];
			final int size = vertical ? child.getHeight () : child.getWidth ();
			if (size <= 0)
				continue;

			if (start < firstStart) { firstStart = start; firstIcon = child; }
			if (start + size > lastEnd) { lastEnd = start + size; lastIcon = child; }
			if (coord >= start && coord < start + size)
			{
				over = child;
				frac = (float) (coord - start) / size;
			}
		}

		if (over == null)
		{
			// Past either end of the bar → insert before the first / after the last.
			if (firstIcon != null && coord < firstStart)
				this.previewInsert (firstIcon, false);
			else
				this.previewInsert (lastIcon, true); // lastIcon may be null → end
			return;
		}

		if (frac >= FOLD_ZONE_LO && frac <= FOLD_ZONE_HI && this.appManager.canFoldOnto (over))
			this.previewFold (over);
		else
			this.previewInsert (over, frac > 0.5f);
	}

	private void previewFold (View target)
	{
		if (target == this.lastFoldTarget && ! this.lastWasInsert)
			return;
		this.lastFoldTarget = target;
		this.lastWasInsert = false;
		this.lastInsertTarget = null;
		this.appManager.previewPinnedFold (target);
	}

	private void previewInsert (View target, boolean after)
	{
		if (this.lastWasInsert && target == this.lastInsertTarget && after == this.lastInsertAfter)
			return;
		this.lastInsertTarget = target;
		this.lastInsertAfter = after;
		this.lastWasInsert = true;
		this.lastFoldTarget = null;
		this.appManager.previewPinnedInsert (target, after);
	}
}

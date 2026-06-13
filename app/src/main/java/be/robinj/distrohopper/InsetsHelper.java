package be.robinj.distrohopper;

import android.app.Activity;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public final class InsetsHelper
{
	private InsetsHelper ()
	{
	}

	/** Pads the view by the system bar and display cutout insets, additive to its original padding. */
	public static void applySystemBarsPadding (final View view)
	{
		final int pl = view.getPaddingLeft ();
		final int pt = view.getPaddingTop ();
		final int pr = view.getPaddingRight ();
		final int pb = view.getPaddingBottom ();

		ViewCompat.setOnApplyWindowInsetsListener (view, (v, windowInsets) ->
		{
			final Insets insets = windowInsets.getInsets (
				WindowInsetsCompat.Type.systemBars () | WindowInsetsCompat.Type.displayCutout ());
			v.setPadding (pl + insets.left, pt + insets.top, pr + insets.right, pb + insets.bottom);

			return WindowInsetsCompat.CONSUMED;
		});
	}

	public static void applySystemBarsPadding (final Activity activity)
	{
		// Lay out edge-to-edge on every API level so the additive padding above is
		// never combined with decor-fitted insets.
		WindowCompat.setDecorFitsSystemWindows (activity.getWindow (), false);
		applySystemBarsPadding (activity.findViewById (android.R.id.content));

		// Window#setStatusBarColor is a no-op under the enforced edge-to-edge of recent
		// Android versions, so paint our own scrim behind the status bar to stop the
		// translucent window background from bleeding through above the action bar.
		if (activity instanceof AppCompatActivity)
			addStatusBarScrim (activity);
	}

	/** Fills the status bar inset region with the action bar's colour, matching the header bar. */
	private static void addStatusBarScrim (final Activity activity)
	{
		final TypedValue colorPrimary = new TypedValue ();
		if (! activity.getTheme ().resolveAttribute (androidx.appcompat.R.attr.colorPrimary, colorPrimary, true))
			return;

		final ViewGroup decor = (ViewGroup) activity.getWindow ().getDecorView ();
		final View scrim = new View (activity);
		scrim.setBackgroundColor (colorPrimary.data);

		// Index 0 so the scrim sees the insets (and draws) beneath the action bar overlay.
		// Its listener must return the insets unchanged, or the content below won't be padded.
		final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams (
			FrameLayout.LayoutParams.MATCH_PARENT, 0, Gravity.TOP);
		decor.addView (scrim, 0, lp);

		ViewCompat.setOnApplyWindowInsetsListener (scrim, (v, windowInsets) ->
		{
			final Insets insets = windowInsets.getInsets (
				WindowInsetsCompat.Type.statusBars () | WindowInsetsCompat.Type.displayCutout ());
			lp.height = insets.top;
			v.setLayoutParams (lp);

			return windowInsets;
		});
	}
}

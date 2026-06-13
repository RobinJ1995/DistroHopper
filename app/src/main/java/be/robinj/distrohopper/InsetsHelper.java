package be.robinj.distrohopper;

import android.app.Activity;
import android.util.TypedValue;
import android.view.View;

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

		// Match the status bar colour to the action bar so the translucent window
		// background doesn't bleed through above the header bar.
		if (activity instanceof AppCompatActivity)
		{
			final TypedValue colorPrimary = new TypedValue ();
			if (activity.getTheme ().resolveAttribute (androidx.appcompat.R.attr.colorPrimary, colorPrimary, true))
				activity.getWindow ().setStatusBarColor (colorPrimary.data);
		}

		applySystemBarsPadding (activity.findViewById (android.R.id.content));
	}
}

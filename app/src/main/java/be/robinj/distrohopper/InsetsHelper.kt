package be.robinj.distrohopper

import android.app.Activity
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

object InsetsHelper {
	/** Pads the view by the system bar and display cutout insets, additive to its original padding. */
	@JvmStatic
	fun applySystemBarsPadding(view: View) {
		val pl = view.paddingLeft
		val pt = view.paddingTop
		val pr = view.paddingRight
		val pb = view.paddingBottom

		ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
			val insets = windowInsets.getInsets(
				WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
			v.setPadding(pl + insets.left, pt + insets.top, pr + insets.right, pb + insets.bottom)

			WindowInsetsCompat.CONSUMED
		}
	}

	@JvmStatic
	fun applySystemBarsPadding(activity: Activity) {
		// Lay out edge-to-edge on every API level so the additive padding above is
		// never combined with decor-fitted insets.
		WindowCompat.setDecorFitsSystemWindows(activity.window, false)
		applySystemBarsPadding(activity.findViewById<View>(android.R.id.content))

		// Window#setStatusBarColor is a no-op under the enforced edge-to-edge of recent
		// Android versions, so paint our own scrim behind the status bar to stop the
		// translucent window background from bleeding through above the action bar.
		if (activity is AppCompatActivity)
			addStatusBarScrim(activity)
	}

	/** Fills the status bar inset region with the action bar's colour, matching the header bar. */
	private fun addStatusBarScrim(activity: Activity) {
		val colorPrimary = TypedValue()
		if (!activity.theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, colorPrimary, true))
			return

		val decor = activity.window.decorView as ViewGroup
		val scrim = View(activity)
		scrim.setBackgroundColor(colorPrimary.data)

		// Index 0 so the scrim sees the insets (and draws) beneath the action bar overlay.
		// Its listener must return the insets unchanged, or the content below won't be padded.
		val lp = FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT, 0, Gravity.TOP)
		decor.addView(scrim, 0, lp)

		ViewCompat.setOnApplyWindowInsetsListener(scrim) { v, windowInsets ->
			val insets = windowInsets.getInsets(
				WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout())
			lp.height = insets.top
			v.layoutParams = lp

			windowInsets
		}
	}
}

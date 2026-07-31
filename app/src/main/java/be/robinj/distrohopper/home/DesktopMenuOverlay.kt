package be.robinj.distrohopper.home

import android.app.Activity
import android.graphics.Color
import android.os.PowerManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R

/**
 * The long-press-on-empty-desktop menu: the home screen zooms out slightly and
 * darkens behind a scrim while a rounded sheet slides up from the bottom of the
 * screen, offering the desktop-level actions (add a widget, open the settings).
 *
 * Like [be.robinj.distrohopper.folder.FolderOverlay] this is an **in-activity
 * overlay** added to `android.R.id.content` rather than a dialog/PopupWindow:
 * the zoom-out is a plain scale on the activity's content (the wallpaper lives
 * in its own system window and stays put behind it), and Back/Home dismissal is
 * wired through HomeActivity the same way as the folder popovers.
 */
class DesktopMenuOverlay(private val activity: Activity) {
	private val content = activity.findViewById<ViewGroup>(android.R.id.content)
	private var scrim: FrameLayout? = null
	private var sheet: View? = null
	// The sheet's own layout padding, before the navigation inset is added //
	private var sheetBasePaddingBottom = 0

	val isShowing: Boolean get() = this.scrim != null

	/**
	 * Shows the menu. [onAddWidget]/[onCustomise]/[onSettings] run once the
	 * corresponding row is tapped (the overlay dismisses itself first).
	 */
	fun show(onAddWidget: () -> Unit, onCustomise: () -> Unit, onSettings: () -> Unit) {
		if (this.isShowing) {
			return
		}
		active?.takeIf { it !== this }?.dismiss()

		val duration = this.duration()

		val scrim = FrameLayout(this.activity).apply {
			setBackgroundColor(SCRIM_COLOUR)
			isClickable = true
			setOnClickListener { this@DesktopMenuOverlay.dismiss() }
			alpha = 0f
		}

		val sheet = LayoutInflater.from(this.activity)
			.inflate(R.layout.desktop_menu_sheet, scrim, false)
		sheet.isClickable = true // taps on the sheet must not fall through to the scrim //
		this.sheetBasePaddingBottom = sheet.paddingBottom
		this.applyNavigationInset(sheet)

		sheet.findViewById<View>(R.id.rowDesktopMenuAddWidget).setOnClickListener {
			this.dismiss()
			onAddWidget()
		}
		sheet.findViewById<View>(R.id.rowDesktopMenuCustomise).setOnClickListener {
			this.dismiss()
			onCustomise()
		}
		sheet.findViewById<View>(R.id.rowDesktopMenuSettings).setOnClickListener {
			this.dismiss()
			onSettings()
		}

		scrim.addView(sheet, FrameLayout.LayoutParams(
			this.sheetWidth(this.content.width), FrameLayout.LayoutParams.WRAP_CONTENT,
			Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL))
		this.content.addView(scrim, FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
		this.scrim = scrim
		this.sheet = sheet

		// HomeActivity is not recreated on rotation (configChanges), so the
		// open menu survives it: re-derive the width cap, the inset padding and
		// the zoom pivot whenever the overlay's size actually changes. //
		scrim.addOnLayoutChangeListener { _, l, t, r, b, oldL, oldT, oldR, oldB ->
			if (r - l != oldR - oldL || b - t != oldB - oldT) {
				this.onHostResized(r - l)
			}
		}

		scrim.animate().alpha(1f).setDuration(duration).start()

		this.backdrop()?.let { backdrop ->
			backdrop.pivotX = backdrop.width / 2f
			backdrop.pivotY = backdrop.height / 2f
			backdrop.animate().scaleX(ZOOM_SCALE).scaleY(ZOOM_SCALE)
				.setDuration(duration).setInterpolator(DecelerateInterpolator()).start()
		}

		// The slide-in needs the sheet's height, which only exists after a
		// layout pass; start it hidden and animate once laid out. //
		sheet.visibility = View.INVISIBLE
		sheet.post {
			if (this.sheet !== sheet) {
				return@post // dismissed before the first layout //
			}

			sheet.translationY = sheet.height.toFloat()
			sheet.visibility = View.VISIBLE
			sheet.animate().translationY(0f)
				.setDuration(duration).setInterpolator(DecelerateInterpolator()).start()
		}

		active = this
		this.notifyStateChanged()
	}

	/** Animates the overlay closed: sheet down, backdrop back to full size. */
	fun dismiss() {
		val scrim = this.scrim ?: return
		val sheet = this.sheet
		this.scrim = null
		this.sheet = null
		if (active === this) {
			active = null
		}
		this.notifyStateChanged()

		val duration = this.duration()

		this.backdrop()?.animate()?.scaleX(1f)?.scaleY(1f)
			?.setDuration(duration)?.setInterpolator(AccelerateInterpolator())?.start()
		sheet?.animate()?.translationY(sheet.height.toFloat())
			?.setDuration(duration)?.setInterpolator(AccelerateInterpolator())?.start()
		scrim.animate().alpha(0f).setDuration(duration).withEndAction {
			this.content.removeView(scrim)
		}.start()
	}

	/** Full-width on phones, capped (and centred) on wide screens. */
	private fun sheetWidth(hostWidth: Int): Int {
		val maxWidth = this.dp(MAX_SHEET_WIDTH_DP)

		return if (hostWidth in 1 until maxWidth) FrameLayout.LayoutParams.MATCH_PARENT
			else maxWidth
	}

	/**
	 * Keeps the actions clear of the navigation bar; the overlay spans the
	 * whole window, unlike the inset-padded launcher/dash container.
	 */
	private fun applyNavigationInset(sheet: View) {
		val navInset = ViewCompat.getRootWindowInsets(this.content)
			?.getInsets(WindowInsetsCompat.Type.tappableElement())?.bottom ?: 0
		sheet.setPadding(sheet.paddingLeft, sheet.paddingTop,
			sheet.paddingRight, this.sheetBasePaddingBottom + navInset)
	}

	/** The overlay changed size (rotation): re-fit the sheet and the zoom pivot. */
	private fun onHostResized(hostWidth: Int) {
		val sheet = this.sheet ?: return

		this.applyNavigationInset(sheet)

		val width = this.sheetWidth(hostWidth)
		(sheet.layoutParams as? FrameLayout.LayoutParams)
			?.takeIf { it.width != width }
			?.let {
				it.width = width
				sheet.layoutParams = it
			}

		this.backdrop()?.let { backdrop ->
			backdrop.pivotX = backdrop.width / 2f
			backdrop.pivotY = backdrop.height / 2f
		}
	}

	/** The view to zoom out — the activity's main content (child 0). */
	private fun backdrop(): View? =
		this.content.getChildAt(0)?.takeIf { it !== this.scrim }

	/** Transitions settle immediately in battery saver, like the dash's. */
	private fun duration(): Long =
		if (this.activity.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true) 0L
		else DURATION

	private fun dp(value: Int): Int =
		(value * this.activity.resources.displayMetrics.density).toInt()

	/** Keeps HomeActivity's Back callback in sync with the menu-open state. */
	private fun notifyStateChanged() {
		(this.activity as? HomeActivity)?.updateBackCallback()
	}

	companion object {
		private const val DURATION = 220L
		private const val ZOOM_SCALE = 0.92f
		private const val MAX_SHEET_WIDTH_DP = 480
		private val SCRIM_COLOUR = Color.argb(150, 0, 0, 0)

		// The currently open menu, if any: the scrim covers the whole activity,
		// so a single slot suffices — mirrors FolderOverlay's active tracking so
		// HomeActivity can close it on Back or Home. //
		private var active: DesktopMenuOverlay? = null

		/** Whether a desktop menu is currently open in [activity]. */
		@JvmStatic
		fun isShowingIn(activity: Activity): Boolean =
			active?.let { it.activity === activity && it.isShowing } ?: false

		/** Dismisses the desktop menu open in [activity], if any. */
		@JvmStatic
		fun dismissActive(activity: Activity) {
			if (isShowingIn(activity)) {
				active?.dismiss()
			}
		}

		/** Drops the [active] reference so a destroyed [activity] is not retained. */
		@JvmStatic
		fun clearFor(activity: Activity) {
			if (active?.activity === activity) {
				active = null
			}
		}
	}
}

package be.robinj.distrohopper.home

import android.app.Activity
import android.app.Dialog
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.desktop.FrostedGlass
import be.robinj.distrohopper.preferences.AnimationMode

/**
 * The long-press-on-empty-desktop menu: the desktop zooms out and darkens while a
 * rounded sheet slides up from the bottom of the screen, offering the desktop-level
 * actions (add a widget, customise, open the settings).
 *
 * The sheet is a **window of its own** (`DesktopMenuSheetTheme`, a
 * `ModernDialogTheme` pinned to the bottom edge) rather than a view in the home
 * screen, and that is the whole reason it is a Dialog: cross-window blur is clipped
 * to a window's bounds and its background's alpha, so the sheet blurs exactly what it
 * covers — wallpaper included. No in-activity view can do that. A view's
 * [android.graphics.RenderEffect] blurs the view's *own* content, all of it, and can
 * never reach the wallpaper, which lives in its own system window behind the
 * (translucent) activity. `FrostedGlass.applyBottomSheetFallback` covers the devices
 * where cross-window blur is switched off, exactly as the pop-up dialogs do.
 *
 * The zoom-out stays in the activity (see [zoomTargets]), so the two halves are
 * animated separately but in step. Back and outside taps are the Dialog's own; Home
 * dismisses through HomeActivity via the static active-slot pattern
 * ([isShowingIn]/[dismissActive]/[clearFor]), mirroring
 * [be.robinj.distrohopper.folder.FolderOverlay].
 */
class DesktopMenuOverlay(private val activity: Activity) {
	private val content = activity.findViewById<ViewGroup>(android.R.id.content)
	private var dialog: Dialog? = null
	private var sheet: View? = null
	// The sheet's own layout padding, before the navigation inset is added //
	private var sheetBasePaddingBottom = 0

	val isShowing: Boolean get() = this.dialog?.isShowing == true

	/**
	 * Shows the menu. [onAddWidget]/[onCustomise]/[onSettings] run once the
	 * corresponding action is tapped (the overlay dismisses itself first).
	 */
	fun show(onAddWidget: () -> Unit, onCustomise: () -> Unit, onSettings: () -> Unit) {
		if (this.isShowing) {
			return
		}
		active?.takeIf { it !== this }?.dismiss()

		val dialog = Dialog(this.activity, R.style.DesktopMenuSheetTheme)
		dialog.setContentView(R.layout.desktop_menu_sheet)
		dialog.setCanceledOnTouchOutside(true)

		val sheet = dialog.findViewById<View>(R.id.desktopMenuSheet)
		this.sheetBasePaddingBottom = sheet.paddingBottom
		this.dialog = dialog
		this.sheet = sheet

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

		val window = dialog.window
		window?.setGravity(Gravity.BOTTOM)
		// Let the window run under the system bars, so the sheet's square bottom
		// corners actually meet the screen edge (and its blur covers the strip
		// behind the navigation bar) instead of stopping above them. It has to be
		// fitInsetsTypes: BOTTOM gravity resolves against the window's *parent
		// frame*, which is inset by the system bars until this says otherwise —
		// FLAG_LAYOUT_NO_LIMITS only frees the display bounds and leaves that
		// parent frame (and so the sheet) stopping short. [refit] pads the actions
		// clear of the bar itself. //
		// ...and hang the bottom of the window off the bottom of the screen by one
		// corner radius. The surface has to be rounded uniformly for the blur to
		// follow it (see desktop_menu_sheet_background), so the bottom corners are
		// simply moved out of sight rather than squared off; [refit] adds the same
		// amount back as padding so nothing rides off with them. //
		window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
		window?.let {
			it.attributes = it.attributes.apply {
				fitInsetsTypes = 0
				y = -this@DesktopMenuOverlay.dp(CORNER_RADIUS_DP)
			}
		}
		if (this.duration() == 0L) {
			window?.setWindowAnimations(0) // battery saver: settle immediately //
		}
		// The blur comes from the theme; this is the no-blur fallback. The sheet's
		// surface is the same rounded card as the pop-up dialogs', so the plain
		// dialog fallback matches it exactly. //
		window?.let { FrostedGlass.applyDialogFallback(it) }

		this.refit()

		// HomeActivity is not recreated on rotation (configChanges), so the open menu
		// survives it: re-derive the width cap, the inset padding and the zoom pivots
		// whenever the sheet's window actually changes size. //
		window?.decorView?.addOnLayoutChangeListener { _, l, t, r, b, oldL, oldT, oldR, oldB ->
			if (r - l != oldR - oldL || b - t != oldB - oldT) {
				this.refit()
				this.applyZoomPivots()
			}
		}

		// Covers every close path — our own dismiss(), Back, and a tap outside //
		dialog.setOnDismissListener { this.onDismissed() }

		dialog.show()

		this.applyZoomPivots()
		for (target in this.zoomTargets()) {
			target.animate().scaleX(ZOOM_SCALE).scaleY(ZOOM_SCALE)
				.setDuration(this.duration()).setInterpolator(DecelerateInterpolator()).start()
		}

		active = this
		this.notifyStateChanged()
	}

	/** Closes the menu: the sheet slides back down and the desktop zooms back in. */
	fun dismiss() {
		this.dialog?.dismiss() // [onDismissed] does the rest //
	}

	/** The sheet's window is gone: drop it and zoom the desktop back in. */
	private fun onDismissed() {
		this.dialog = null
		this.sheet = null
		if (active === this) {
			active = null
		}

		for (target in this.zoomTargets()) {
			target.animate().scaleX(1f).scaleY(1f)
				.setDuration(this.duration()).setInterpolator(AccelerateInterpolator()).start()
		}

		this.notifyStateChanged()
	}

	/** Re-fits the sheet's window to the screen: width cap and navigation inset. */
	private fun refit() {
		val window = this.dialog?.window ?: return
		val sheet = this.sheet ?: return

		// The overlay spans the whole window, unlike the inset-padded
		// launcher/dash container, so it keeps the actions clear of the
		// navigation bar itself. //
		val navInset = ViewCompat.getRootWindowInsets(this.content)
			?.getInsets(WindowInsetsCompat.Type.tappableElement())?.bottom ?: 0
		// The window hangs one corner radius below the screen (see [show]), so that
		// much padding is added back to keep the actions on-screen. //
		sheet.setPadding(sheet.paddingLeft, sheet.paddingTop, sheet.paddingRight,
			this.sheetBasePaddingBottom + navInset + this.dp(CORNER_RADIUS_DP))

		val width = this.sheetWidth(this.content.width)
		if (window.attributes?.width != width) {
			window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
		}
	}

	/** Full-width on phones, capped (and centred) on wide screens. */
	private fun sheetWidth(hostWidth: Int): Int {
		val maxWidth = this.dp(MAX_SHEET_WIDTH_DP)

		return if (hostWidth in 1 until maxWidth) WindowManager.LayoutParams.MATCH_PARENT
			else maxWidth
	}

	/**
	 * The views that zoom out: the desktop and the launcher/dash, i.e. what the
	 * menu is *about*. The panel and the status bar are deliberately left alone —
	 * shrinking them away from the screen edge reads as a glitch rather than a
	 * zoom — and so is the wallpaper (`wpWallpaper` / the overlay tint above it),
	 * which stands in for the system wallpaper behind the window and should sit
	 * still exactly like the real one does.
	 */
	private fun zoomTargets(): List<View> =
		listOfNotNull(
			this.activity.findViewById(R.id.vgWidgets),
			this.activity.findViewById(R.id.llLauncherAndDashContainer),
		)

	/**
	 * Points every target at the *same* screen pixel — the centre of their shared
	 * parent — so scaling them individually is geometrically identical to scaling
	 * that parent, and they zoom as one piece instead of each toward its own
	 * middle. Re-derived whenever the sheet resizes (rotation).
	 */
	private fun applyZoomPivots() {
		for (target in this.zoomTargets()) {
			val parent = target.parent as? View ?: continue
			target.pivotX = parent.width / 2f - target.left
			target.pivotY = parent.height / 2f - target.top
		}
	}

	/** Transitions settle immediately when animations are off, like the dash's. */
	private fun duration(): Long =
		if (AnimationMode.animationsEnabled(this.activity)) DURATION else 0L

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
		// Must match desktop_menu_sheet_background / R.dimen.dialog_corner_radius //
		private const val CORNER_RADIUS_DP = 28

		// The currently open menu, if any: the sheet is modal, so a single slot
		// suffices — mirrors FolderOverlay's active tracking so HomeActivity can
		// close it on Home. //
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

		/**
		 * Tears the menu down with [activity]: unlike an in-activity overlay, a
		 * leftover Dialog window would outlive it (and be reported leaked), so the
		 * window is closed here — silently, since the views it would animate are
		 * going away too.
		 */
		@JvmStatic
		fun clearFor(activity: Activity) {
			val overlay = active ?: return
			if (overlay.activity !== activity) {
				return
			}

			active = null
			overlay.dialog?.setOnDismissListener(null)
			overlay.dialog?.dismiss()
			overlay.dialog = null
			overlay.sheet = null
		}
	}
}

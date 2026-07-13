package be.robinj.distrohopper.folder

import android.app.Activity
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import be.robinj.distrohopper.HomeActivity

/**
 * Shared chrome for the three folder popovers (dash, launcher, desktop): a dim,
 * blurred full-screen overlay holding the folder's content [panel], opened
 * centred over the tapped folder icon ([anchor]) with a scale-and-fade animation
 * that grows from the icon.
 *
 * It is an **in-activity overlay** (added to `android.R.id.content`) so an extract
 * drag started from the panel stays in the activity window and reaches the
 * dash/dock/desktop/trash drop targets (see `FolderPopup` /
 * `widgets.DesktopFolderOverlay`). The backdrop blur is a GPU [RenderEffect] on
 * the content behind the overlay (minSdk 31), so it needs no wallpaper bitmap.
 */
class FolderOverlay(private val activity: Activity) {
	private val content = activity.findViewById<ViewGroup>(android.R.id.content)
	private var scrim: FrameLayout? = null
	private var panel: View? = null

	val isShowing: Boolean get() = this.scrim != null

	/**
	 * Shows [panel] (laid out at [width]x[height] px) centred over [anchor] but
	 * clamped on-screen, dimming and blurring everything behind it. A tap outside
	 * the panel calls [onOutsideTap] (typically dismiss).
	 */
	fun show(panel: View, width: Int, height: Int, anchor: View, onOutsideTap: () -> Unit) {
		active?.takeIf { it !== this }?.dismiss()

		val loc = anchorCentreInContent(anchor)
		val margin = dp(8)
		val left = (loc.first - width / 2)
			.coerceIn(margin, (content.width - width - margin).coerceAtLeast(margin))
		val top = (loc.second - height / 2)
			.coerceIn(margin, (content.height - height - margin).coerceAtLeast(margin))

		val scrim = FrameLayout(activity).apply {
			setBackgroundColor(SCRIM_COLOR)
			isClickable = true
			setOnClickListener { onOutsideTap() }
			alpha = 0f
		}
		scrim.addView(panel, FrameLayout.LayoutParams(width, height).apply {
			leftMargin = left
			topMargin = top
		})
		content.addView(scrim, FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
		this.scrim = scrim
		this.panel = panel

		this.backdrop()?.setRenderEffect(
			RenderEffect.createBlurEffect(BLUR_RADIUS, BLUR_RADIUS, Shader.TileMode.CLAMP))

		// Grow from the icon: pivot at the anchor centre within the panel.
		panel.pivotX = (loc.first - left).toFloat().coerceIn(0f, width.toFloat())
		panel.pivotY = (loc.second - top).toFloat().coerceIn(0f, height.toFloat())
		panel.scaleX = START_SCALE
		panel.scaleY = START_SCALE
		panel.alpha = 0f
		panel.animate().scaleX(1f).scaleY(1f).alpha(1f)
			.setDuration(DURATION).setInterpolator(DecelerateInterpolator()).start()
		scrim.animate().alpha(1f).setDuration(DURATION).start()

		active = this
		this.notifyStateChanged()
	}

	/**
	 * Animates the overlay closed. [beforeRemove] runs once faded out but still
	 * attached (e.g. desktop widgets detach back to retention), [onRemoved] after
	 * it is gone from the tree. A no-op if nothing is showing.
	 */
	fun dismiss(beforeRemove: () -> Unit = {}, onRemoved: () -> Unit = {}) {
		val scrim = this.scrim ?: return
		val panel = this.panel
		this.scrim = null
		this.panel = null
		if (active === this)
			active = null
		this.notifyStateChanged()

		panel?.animate()?.scaleX(START_SCALE)?.scaleY(START_SCALE)?.alpha(0f)
			?.setDuration(DURATION)?.setInterpolator(AccelerateInterpolator())?.start()
		scrim.animate().alpha(0f).setDuration(DURATION).withEndAction {
			beforeRemove()
			this.content.getChildAt(0)?.takeIf { it !== scrim }?.setRenderEffect(null)
			this.content.removeView(scrim)
			onRemoved()
		}.start()
	}

	/** The view behind the overlay to blur — the activity's main content (child 0). */
	private fun backdrop(): View? =
		this.content.getChildAt(0)?.takeIf { it !== this.scrim }

	/** [anchor]'s centre in the content view's coordinate space. */
	private fun anchorCentreInContent(anchor: View): Pair<Int, Int> {
		val a = IntArray(2)
		val c = IntArray(2)
		anchor.getLocationOnScreen(a)
		this.content.getLocationOnScreen(c)
		return (a[0] + anchor.width / 2 - c[0]) to (a[1] + anchor.height / 2 - c[1])
	}

	private fun dp(value: Int): Int =
		(value * this.activity.resources.displayMetrics.density).toInt()

	/** Keeps HomeActivity's Back callback in sync with folder-open state. */
	private fun notifyStateChanged() {
		(this.activity as? HomeActivity)?.updateBackCallback()
	}

	companion object {
		private const val BLUR_RADIUS = 5f
		private const val START_SCALE = 0.7f
		private const val DURATION = 160L
		private val SCRIM_COLOR = Color.argb(140, 0, 0, 0)

		// The currently open folder overlay, if any. Only one folder can be open
		// at a time (the scrim covers the whole activity), and every popup type
		// opens and dismisses through this class, so a single slot suffices. Lets
		// HomeActivity close the folder on Back or Home without every click site
		// having to retain the popup it created.
		private var active: FolderOverlay? = null

		/** Whether a folder overlay is currently open in [activity]. */
		@JvmStatic
		fun isShowingIn(activity: Activity): Boolean =
			active?.let { it.activity === activity && it.isShowing } ?: false

		/**
		 * Dismisses the folder overlay open in [activity], if any. Returns whether
		 * there was one — i.e. whether this press was consumed by a folder.
		 */
		@JvmStatic
		fun dismissActive(activity: Activity): Boolean {
			val open = isShowingIn(activity)
			if (open)
				active?.dismiss()
			return open
		}

		/** Drops the [active] reference so a destroyed [activity] is not retained. */
		@JvmStatic
		fun clearFor(activity: Activity) {
			if (active?.activity === activity)
				active = null
		}
	}
}

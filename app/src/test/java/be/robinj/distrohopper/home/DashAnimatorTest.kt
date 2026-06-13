package be.robinj.distrohopper.home

import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.os.PowerManager
import android.view.View
import android.widget.EditText
import android.widget.GridView
import android.widget.LinearLayout
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.preferences.Preference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowPowerManager

/**
 * DashController end-states for the animated dash_animation presets. Runs
 * under the PAUSED looper because animators never advance under LEGACY; the
 * visual close work only happens once the animators have run.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class DashAnimatorTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@After fun tearDown() { this.scenario.close() }

	private fun launch(theme: String) {
		this.scenario = ActivityTestSupport.launchHome(
			configurePrefs = { it.putString(Preference.THEME.getName(), theme) })
	}

	private fun controller(activity: HomeActivity): DashController {
		val container = DependencyContainer.of(activity)
		return DashController(activity, activity.viewFinder,
			container.themeManager.current, container.prefs)
	}

	/*
	 * Fresh-open start states are applied in a pre-draw listener; dispatching
	 * pre-draw by hand makes the animation start deterministic under Robolectric.
	 */
	private fun startPendingAnimation(activity: HomeActivity) {
		// The genie reads the current pager page's grid; lay the pager out so it
		// exists, then fire the pre-draw the fresh-open start is gated on (on
		// llDash, which shares the window's ViewTreeObserver with the page).
		ActivityTestSupport.layoutDashApps(activity)
		activity.findViewById<LinearLayout>(R.id.llDash).viewTreeObserver.dispatchOnPreDraw()
	}

	private fun assertSettledOpen(activity: HomeActivity) {
		val llDash = activity.findViewById<LinearLayout>(R.id.llDash)
		val overlay = activity.findViewById<View>(R.id.flWallpaperOverlayWhenDashOpened)

		assertEquals(View.VISIBLE, llDash.visibility)
		assertEquals(1F, llDash.alpha, 0.001F)
		this.assertIdentityTransforms(llDash)
		assertEquals(View.INVISIBLE,
			activity.findViewById<View>(R.id.flWallpaperOverlay).visibility)
		assertEquals(View.VISIBLE, overlay.visibility)
		assertEquals(1F, overlay.alpha, 0.001F)
		this.assertIconsAtRest(activity)
	}

	private fun assertSettledClosed(activity: HomeActivity) {
		val llDash = activity.findViewById<LinearLayout>(R.id.llDash)
		val overlay = activity.findViewById<View>(R.id.flWallpaperOverlayWhenDashOpened)

		assertEquals(View.GONE, llDash.visibility)
		assertEquals(1F, llDash.alpha, 0.001F) // Reset so the NONE path stays correct //
		this.assertIdentityTransforms(llDash)
		assertEquals(View.VISIBLE,
			activity.findViewById<View>(R.id.flWallpaperOverlay).visibility)
		assertEquals(View.INVISIBLE, overlay.visibility)
		assertEquals(1F, overlay.alpha, 0.001F)
		this.assertIconsAtRest(activity)
	}

	private fun assertIdentityTransforms(view: View) {
		assertEquals(0F, view.translationX, 0.001F)
		assertEquals(0F, view.translationY, 0.001F)
		assertEquals(1F, view.scaleX, 0.001F)
		assertEquals(1F, view.scaleY, 0.001F)
	}

	private fun assertIconsAtRest(activity: HomeActivity) {
		val grid = activity.findViewById<GridView>(R.id.gvDashHomeApps) ?: return
		for (i in 0 until grid.childCount) {
			this.assertIdentityTransforms(grid.getChildAt(i))
		}
	}

	private fun assertOpensAndClosesSettled(theme: String) {
		this.launch(theme)
		this.scenario.onActivity { activity ->
			val dash = this.controller(activity)

			dash.open()
			this.startPendingAnimation(activity)
			ActivityTestSupport.drainTasks()
			assertTrue(dash.isOpen)
			this.assertSettledOpen(activity)

			dash.close()
			ActivityTestSupport.drainTasks()
			assertFalse(dash.isOpen)
			this.assertSettledClosed(activity)
		}
	}

	@Test fun gnomeOpensAndClosesSettled() = this.assertOpensAndClosesSettled("gnome")

	@Test fun cinnamonOpensAndClosesSettled() = this.assertOpensAndClosesSettled("cinnamon")

	@Test fun elementaryOpensAndClosesSettled() = this.assertOpensAndClosesSettled("elementary")

	@Test fun unityOpensAndClosesSettled() = this.assertOpensAndClosesSettled("default")

	@Test fun powerSaverDisablesOpenAndCloseAnimations() {
		this.launch("gnome")
		this.scenario.onActivity { activity ->
			val powerManager = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
			Shadow.extract<ShadowPowerManager>(powerManager).setIsPowerSaveMode(true)
			val dash = this.controller(activity)
			val panelBackground = activity.resources.getDrawable(
				DependencyContainer.of(activity).themeManager.current.panel_background)

			dash.open()

			assertTrue(dash.isOpen)
			this.assertSettledOpen(activity)
			assertEquals(0, (activity.findViewById<LinearLayout>(R.id.llPanel).background
				as LayerDrawable).getDrawable(1).alpha)

			dash.close()

			assertFalse(dash.isOpen)
			this.assertSettledClosed(activity)
			assertEquals(panelBackground.alpha,
				(activity.findViewById<LinearLayout>(R.id.llPanel).background
					as LayerDrawable).getDrawable(1).alpha)
		}
	}

	@Test fun closeClearsTheSearchFieldImmediately() {
		this.launch("gnome")
		this.scenario.onActivity { activity ->
			val dash = this.controller(activity)
			val etDashSearch = activity.findViewById<EditText>(R.id.etDashSearch)

			dash.open()
			this.startPendingAnimation(activity)
			ActivityTestSupport.drainTasks()
			etDashSearch.setText("query")

			dash.close()

			assertFalse(dash.isOpen)
			assertEquals("", etDashSearch.text.toString()) // Immediate, not animated //

			ActivityTestSupport.drainTasks()
			this.assertSettledClosed(activity)
		}
	}

	@Test fun closingDuringTheOpenAnimationStillHidesTheDash() {
		this.launch("gnome")
		this.scenario.onActivity { activity ->
			val dash = this.controller(activity)

			dash.open()
			this.startPendingAnimation(activity)
			dash.close()
			ActivityTestSupport.drainTasks()

			assertFalse(dash.isOpen)
			this.assertSettledClosed(activity)
		}
	}

	@Test fun reopeningDuringTheCloseAnimationLeavesTheDashOpen() {
		this.launch("gnome")
		this.scenario.onActivity { activity ->
			val dash = this.controller(activity)

			dash.open()
			this.startPendingAnimation(activity)
			ActivityTestSupport.drainTasks()
			dash.close()
			dash.open()
			ActivityTestSupport.drainTasks()

			assertTrue(dash.isOpen)
			this.assertSettledOpen(activity)
		}
	}
}

package be.robinj.distrohopper.home

import android.app.Application
import android.view.View
import android.widget.EditText
import android.widget.GridView
import android.widget.LinearLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

/**
 * DashController end-states with the gnome theme, whose dash_animation is
 * GENIE: unlike the NONE themes covered by DashControllerTest, the visual
 * close work only happens once the animators have run.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class DashAnimatorTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() {
		val application = ApplicationProvider.getApplicationContext<Application>()
		listOf(Preferences.PREFERENCES, Preferences.PINNED_APPS, Preferences.LENSES).forEach {
			application.getSharedPreferences(it, 0).edit().clear().commit()
		}
		application.getSharedPreferences(Preferences.PREFERENCES, 0).edit()
			.putString(Preference.THEME.getName(), "gnome").commit()
		DependencyContainer.of(application).customiseMode.value = false
		ActivityTestSupport.installTestDispatchers()
		ActivityTestSupport.seedPackageManager()
		this.scenario = ActivityScenario.launch(HomeActivity::class.java)
			.also { this.drain() }
	}

	@After fun tearDown() { this.scenario.close() }

	/*
	 * ActivityTestSupport.drainTasks() requires the LEGACY looper, but animators
	 * only advance under the PAUSED looper; idling the main looper is enough here
	 * because installTestDispatchers() already runs background work inline.
	 */
	private fun drain() = ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

	private fun controller(activity: HomeActivity): DashController {
		val container = DependencyContainer.of(activity)
		return DashController(activity, activity.viewFinder,
			container.themeManager.current, container.prefs)
	}

	/*
	 * The genie start transforms are applied in a pre-draw listener; dispatching
	 * pre-draw by hand makes the animation start deterministic under Robolectric.
	 */
	private fun startPendingAnimation(activity: HomeActivity) {
		activity.findViewById<GridView>(R.id.gvDashHomeApps).viewTreeObserver.dispatchOnPreDraw()
	}

	private fun assertSettledOpen(activity: HomeActivity) {
		val llDash = activity.findViewById<LinearLayout>(R.id.llDash)
		val overlay = activity.findViewById<View>(R.id.flWallpaperOverlayWhenDashOpened)

		assertEquals(View.VISIBLE, llDash.visibility)
		assertEquals(1F, llDash.alpha, 0.001F)
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
		assertEquals(1F, llDash.alpha, 0.001F) // Reset so the instant path stays correct //
		assertEquals(View.VISIBLE,
			activity.findViewById<View>(R.id.flWallpaperOverlay).visibility)
		assertEquals(View.INVISIBLE, overlay.visibility)
		assertEquals(1F, overlay.alpha, 0.001F)
		this.assertIconsAtRest(activity)
	}

	private fun assertIconsAtRest(activity: HomeActivity) {
		val grid = activity.findViewById<GridView>(R.id.gvDashHomeApps)
		for (i in 0 until grid.childCount) {
			val child = grid.getChildAt(i)
			assertEquals(0F, child.translationX, 0.001F)
			assertEquals(0F, child.translationY, 0.001F)
			assertEquals(1F, child.scaleX, 0.001F)
			assertEquals(1F, child.scaleY, 0.001F)
		}
	}

	@Test fun openAnimatesToTheSettledOpenState() {
		this.scenario.onActivity { activity ->
			val dash = this.controller(activity)

			dash.open()
			this.startPendingAnimation(activity)
			this.drain()

			assertTrue(dash.isOpen)
			this.assertSettledOpen(activity)
		}
	}

	@Test fun closeAnimatesToTheSettledClosedStateAndClearsTheSearchFieldImmediately() {
		this.scenario.onActivity { activity ->
			val dash = this.controller(activity)
			val etDashSearch = activity.findViewById<EditText>(R.id.etDashSearch)

			dash.open()
			this.startPendingAnimation(activity)
			this.drain()
			etDashSearch.setText("query")

			dash.close()

			assertFalse(dash.isOpen)
			assertEquals("", etDashSearch.text.toString()) // Immediate, not animated //

			this.drain()
			this.assertSettledClosed(activity)
		}
	}

	@Test fun closingDuringTheOpenAnimationStillHidesTheDash() {
		this.scenario.onActivity { activity ->
			val dash = this.controller(activity)

			dash.open()
			this.startPendingAnimation(activity)
			dash.close()
			this.drain()

			assertFalse(dash.isOpen)
			this.assertSettledClosed(activity)
		}
	}

	@Test fun reopeningDuringTheCloseAnimationLeavesTheDashOpen() {
		this.scenario.onActivity { activity ->
			val dash = this.controller(activity)

			dash.open()
			this.startPendingAnimation(activity)
			this.drain()
			dash.close()
			dash.open()
			this.drain()

			assertTrue(dash.isOpen)
			this.assertSettledOpen(activity)
		}
	}
}

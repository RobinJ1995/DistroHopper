package be.robinj.distrohopper.home

import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class DashControllerTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() { this.scenario = ActivityTestSupport.launchHome() }
	@After fun tearDown() { this.scenario.close() }

	private fun controller(activity: HomeActivity): DashController {
		val container = DependencyContainer.of(activity)
		return DashController(activity, activity.viewFinder,
			container.themeManager.current, container.prefs)
	}

	@Test fun openShowsTheDashAndSwapsTheOverlays() {
		this.scenario.onActivity { activity ->
			val dash = this.controller(activity)

			dash.open()

			assertTrue(dash.isOpen)
			assertEquals(View.VISIBLE,
				activity.findViewById<LinearLayout>(R.id.llDash).visibility)
			assertEquals(View.INVISIBLE,
				activity.findViewById<View>(R.id.flWallpaperOverlay).visibility)
			assertEquals(View.VISIBLE,
				activity.findViewById<View>(R.id.flWallpaperOverlayWhenDashOpened).visibility)
		}
	}

	@Test fun closeHidesTheDashAndClearsTheSearchField() {
		this.scenario.onActivity { activity ->
			val dash = this.controller(activity)
			val etDashSearch = activity.findViewById<EditText>(R.id.etDashSearch)

			dash.open()
			etDashSearch.setText("query")
			dash.close()

			assertFalse(dash.isOpen)
			assertEquals(View.GONE,
				activity.findViewById<LinearLayout>(R.id.llDash).visibility)
			assertEquals("", etDashSearch.text.toString())
			assertEquals(View.VISIBLE,
				activity.findViewById<View>(R.id.flWallpaperOverlay).visibility)
		}
	}

	@Test fun openAndCloseAreIdempotent() {
		this.scenario.onActivity { activity ->
			val dash = this.controller(activity)

			dash.close() // already closed; must not throw
			assertFalse(dash.isOpen)

			dash.open()
			dash.open()
			assertTrue(dash.isOpen)
		}
	}
}

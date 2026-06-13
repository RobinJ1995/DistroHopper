package be.robinj.distrohopper.desktop

import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.HomeActivity
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class FrostedGlassTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
	@After fun tearDown() {
		scenario.close()
		// The injected predicate lives on a shared object; restore the real one. //
		FrostedGlass.crossWindowBlurEnabled = { it.windowManager.isCrossWindowBlurEnabled }
	}

	@Test fun dialogGetsTheGrainFallbackWhenCrossWindowBlurIsUnavailable() {
		scenario.onActivity { activity ->
			FrostedGlass.crossWindowBlurEnabled = { false }

			val applied = FrostedGlass.applyDialogFallback(activity.window)

			assertTrue(applied is FrostedFallbackDrawable)
		}
	}

	@Test fun dialogKeepsItsThemedSurfaceWhenCrossWindowBlurIsAvailable() {
		scenario.onActivity { activity ->
			FrostedGlass.crossWindowBlurEnabled = { true }

			assertNull(FrostedGlass.applyDialogFallback(activity.window))
		}
	}
}

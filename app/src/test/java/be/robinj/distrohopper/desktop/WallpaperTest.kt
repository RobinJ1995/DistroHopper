package be.robinj.distrohopper.desktop

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class WallpaperTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
	@After fun tearDown() { scenario.close() }

	private fun backgroundColour(wallpaper: Wallpaper): Int =
		(wallpaper.background as ColorDrawable).color

	@Test fun setClearsTheDrawableAndMakesTheBackgroundTransparent() {
		scenario.onActivity { activity ->
			val wallpaper = activity.findViewById<Wallpaper>(R.id.wpWallpaper)
			wallpaper.setImageDrawable(ColorDrawable(Color.RED))

			wallpaper.set()

			assertNull(wallpaper.drawable)
			assertEquals(activity.resources.getColor(R.color.transparent),
				backgroundColour(wallpaper))
		}
	}

	@Test fun blurUsesFrostedFallbackWhenCrossWindowBlurIsUnavailable() {
		scenario.onActivity { activity ->
			val wallpaper = activity.findViewById<Wallpaper>(R.id.wpWallpaper)
			var appliedRadius = -1
			wallpaper.crossWindowBlurEnabled = { false }
			wallpaper.setBackgroundBlurRadius = { _, radius -> appliedRadius = radius }

			wallpaper.blur(activity.window, 64)

			assertEquals(0, appliedRadius)
			assertFalse(wallpaper.background is ColorDrawable)
		}
	}

	@Test fun blurUsesCrossWindowBlurWithoutFrostedFallbackWhenAvailable() {
		scenario.onActivity { activity ->
			val wallpaper = activity.findViewById<Wallpaper>(R.id.wpWallpaper)
			var appliedRadius = -1
			wallpaper.crossWindowBlurEnabled = { true }
			wallpaper.setBackgroundBlurRadius = { _, radius -> appliedRadius = radius }

			wallpaper.blur(activity.window, 64)

			assertEquals(64, appliedRadius)
			assertTrue(wallpaper.background is ColorDrawable)
			assertEquals(0, Color.alpha(backgroundColour(wallpaper)))
		}
	}

	@Test fun switchingToCrossWindowBlurClearsAnExistingFrostedFallback() {
		scenario.onActivity { activity ->
			val wallpaper = activity.findViewById<Wallpaper>(R.id.wpWallpaper)
			var blurSupported = false
			val appliedRadii = mutableListOf<Int>()
			wallpaper.crossWindowBlurEnabled = { blurSupported }
			wallpaper.setBackgroundBlurRadius = { _, radius -> appliedRadii += radius }

			wallpaper.blur(activity.window, 64)
			assertFalse(wallpaper.background is ColorDrawable)

			blurSupported = true
			wallpaper.blur(activity.window, 64)

			assertEquals(listOf(0, 64), appliedRadii)
			assertTrue(wallpaper.background is ColorDrawable)
			assertEquals(0, Color.alpha(backgroundColour(wallpaper)))
		}
	}

	@Test fun switchingToFallbackClearsCrossWindowBlurBeforeAddingGrain() {
		scenario.onActivity { activity ->
			val wallpaper = activity.findViewById<Wallpaper>(R.id.wpWallpaper)
			var blurSupported = true
			val appliedRadii = mutableListOf<Int>()
			wallpaper.crossWindowBlurEnabled = { blurSupported }
			wallpaper.setBackgroundBlurRadius = { _, radius -> appliedRadii += radius }

			wallpaper.blur(activity.window, 64)
			blurSupported = false
			wallpaper.blur(activity.window, 64)

			assertEquals(listOf(64, 0), appliedRadii)
			assertFalse(wallpaper.background is ColorDrawable)
		}
	}

	@Test fun frostedFallbackTintAdaptsAndDarkensTheWallpaperColour() {
		assertEquals(Color.argb(178, 32, 16, 8),
			Wallpaper.fallbackTintFor(Color.rgb(100, 50, 25)))
	}

	@Test fun unblurRestoresTheTransparentBackground() {
		scenario.onActivity { activity ->
			val wallpaper = activity.findViewById<Wallpaper>(R.id.wpWallpaper)
			wallpaper.blur(activity.window, 64)

			wallpaper.unblur(activity.window)

			assertEquals(activity.resources.getColor(R.color.transparent),
				backgroundColour(wallpaper))
		}
	}

	@Test fun initLeavesNoLiveWallpaper() {
		scenario.onActivity { activity ->
			val wallpaper = activity.findViewById<Wallpaper>(R.id.wpWallpaper)

			wallpaper.init()

			assertFalse(wallpaper.isLiveWallpaper)
		}
	}

	@Test fun averageColourFallsBackToUbuntuOrangeWithoutAnyWallpaper() {
		scenario.onActivity { activity ->
			val wallpaper = activity.findViewById<Wallpaper>(R.id.wpWallpaper)
			wallpaper.init()

			assertEquals(Color.rgb(180, 60, 18), wallpaper.getAverageColour(200))
		}
	}

}

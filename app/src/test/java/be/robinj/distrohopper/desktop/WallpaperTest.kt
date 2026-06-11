package be.robinj.distrohopper.desktop

import android.Manifest
import android.app.Application
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
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

	@Test fun blurFallsBackToDarkeningWhenCrossWindowBlurIsUnavailable() {
		scenario.onActivity { activity ->
			// Robolectric's WindowManager reports cross-window blur as disabled.
			val wallpaper = activity.findViewById<Wallpaper>(R.id.wpWallpaper)

			wallpaper.blur(activity.window, 64)

			assertEquals(activity.resources.getColor(R.color.transparentblack60),
				backgroundColour(wallpaper))
		}
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

	@Test fun initWithoutStoragePermissionLeavesNoLiveWallpaper() {
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

	@Test fun initWithStoragePermissionToleratesTheMissingWallpaper() {
		// Robolectric's WallpaperManager cannot serve a wallpaper drawable, so
		// this covers the permission-granted branch tolerating a null result.
		val application = ApplicationProvider.getApplicationContext<Application>()
		Shadows.shadowOf(application)
			.grantPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)

		scenario.onActivity { activity ->
			val wallpaper = activity.findViewById<Wallpaper>(R.id.wpWallpaper)
			wallpaper.init()

			assertEquals(Color.rgb(180, 60, 18), wallpaper.getAverageColour(200))
		}
	}
}

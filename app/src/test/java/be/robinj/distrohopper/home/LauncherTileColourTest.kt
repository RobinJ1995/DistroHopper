package be.robinj.distrohopper.home

import android.app.Application
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.desktop.Wallpaper
import be.robinj.distrohopper.theme.ThemeRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LauncherTileColourTest {
	/** Wallpaper.getAverageColour's last-resort fallback (no wallpaper colours in tests). */
	private val ubuntuOrange = Color.rgb(180, 60, 18)

	private lateinit var application: Application

	@Before fun setUp() {
		application = ApplicationProvider.getApplicationContext()
	}

	@Test fun staticThemeUsesItsFixedTileColour() {
		val theme = ThemeRegistry.create("gnome")
		val expected = application.resources.getColor(theme.launcher_applauncher_backgroundcolour)

		assertEquals(expected, LauncherTileColour.resolve(application, theme))
	}

	@Test fun dynamicThemeDerivesFromWallpaper() {
		val theme = ThemeRegistry.create("default")

		// Default is the only chameleonic theme; with no wallpaper colours
		// available the wallpaper average falls back to the Ubuntu orange.
		assertEquals(ubuntuOrange, LauncherTileColour.resolve(application, theme))
	}

	@Test fun liveWallpaperUsesFixedDarkColour() {
		val wallpaper = Wallpaper(application).also { it.isLiveWallpaper = true }

		assertEquals(Color.argb(40, 40, 40, 40), LauncherTileColour.dynamic(wallpaper, 204))
	}
}

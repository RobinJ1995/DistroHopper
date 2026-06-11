package be.robinj.distrohopper.home

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.LinearLayout
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.desktop.Wallpaper
import be.robinj.distrohopper.desktop.launcher.AppLauncher
import be.robinj.distrohopper.preferences.Preference
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class WallpaperColourApplierTest {
	/** Wallpaper.getAverageColour's last-resort fallback colour. */
	private val ubuntuOrange = Color.rgb(180, 60, 18)

	private fun launch(theme: String): ActivityScenario<HomeActivity> =
		ActivityTestSupport.launchHome(configurePrefs = {
			it.putString(Preference.THEME.getName(), theme)
		})

	private fun applier(activity: HomeActivity): WallpaperColourApplier {
		val container = DependencyContainer.of(activity)
		return WallpaperColourApplier(activity, activity.viewFinder,
			container.themeManager.current,
			LauncherEdgeController(activity, activity.viewFinder,
				container.themeManager.current, container.prefs))
	}

	@Test fun fallsBackToUbuntuOrangeWithoutWallpaperAccess() {
		launch("default").use { scenario ->
			scenario.onActivity { activity ->
				// No storage permission and no wallpaper colours in the test
				// environment, so the average colour falls back to orange.
				val wallpaper = activity.findViewById<Wallpaper>(R.id.wpWallpaper)
				wallpaper.init()

				assertEquals(ubuntuOrange, applier(activity).apply(wallpaper))
			}
		}
	}

	@Test fun dynamicThemeColoursLauncherDashAndAppLaunchers() {
		launch("default").use { scenario ->
			scenario.onActivity { activity ->
				val wallpaper = activity.findViewById<Wallpaper>(R.id.wpWallpaper)
				wallpaper.init()

				val bgColour = applier(activity).apply(wallpaper)

				val llLauncher = activity.findViewById<LinearLayout>(R.id.llLauncher)
				val llDash = activity.findViewById<LinearLayout>(R.id.llDash)
				assertEquals(bgColour, (llLauncher.background as ColorDrawable).color)
				assertEquals(bgColour, (llDash.background as ColorDrawable).color)
				assertEquals(ubuntuOrange,
					activity.findViewById<AppLauncher>(R.id.lalBfb).colour)
				assertEquals(ubuntuOrange,
					activity.findViewById<AppLauncher>(R.id.lalTrash).colour)
			}
		}
	}

	@Test fun nonDynamicThemeLeavesAppLauncherColoursAlone() {
		launch("gnome").use { scenario ->
			scenario.onActivity { activity ->
				val wallpaper = activity.findViewById<Wallpaper>(R.id.wpWallpaper)
				wallpaper.init()
				val lalBfb = activity.findViewById<AppLauncher>(R.id.lalBfb)
				lalBfb.colour = Color.MAGENTA

				applier(activity).apply(wallpaper)

				assertEquals(Color.MAGENTA, lalBfb.colour)
			}
		}
	}

	@Test fun liveWallpaperUsesFixedDarkColours() {
		launch("default").use { scenario ->
			scenario.onActivity { activity ->
				val theme = DependencyContainer.of(activity).themeManager.current
				val wallpaper = activity.findViewById<Wallpaper>(R.id.wpWallpaper)
				wallpaper.isLiveWallpaper = true

				val bgColour = applier(activity).apply(wallpaper)

				val bgOpacity = activity.resources.getInteger(theme.dynamic_background_opacity)
				assertEquals(Color.argb(bgOpacity, 40, 40, 40), bgColour)
				assertEquals(Color.argb(40, 40, 40, 40),
					activity.findViewById<AppLauncher>(R.id.lalBfb).colour)
			}
		}
	}
}

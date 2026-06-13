package be.robinj.distrohopper.home

import android.view.View
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.preferences.Preference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class ThemeApplierTest {
	private fun launch(theme: String): ActivityScenario<HomeActivity> =
		ActivityTestSupport.launchHome(configurePrefs = {
			it.putString(Preference.THEME.getName(), theme)
		})

	private fun requestedColumnWidth(gv: GridView): Int =
		GridView::class.java.getDeclaredField("mRequestedColumnWidth")
			.apply { isAccessible = true }.getInt(gv)

	@Test fun defaultThemeShowsRibbonAndHidesPanelBfb() {
		launch("default").use { scenario ->
			scenario.onActivity { activity ->
				assertEquals(View.GONE,
					activity.findViewById<TextView>(R.id.tvPanelBfb).visibility)
				assertEquals(View.VISIBLE,
					activity.findViewById<LinearLayout>(R.id.llDashRibbon).visibility)
				assertEquals(View.VISIBLE,
					activity.findViewById<View>(R.id.lalPreferences).visibility)

				val llLauncher = activity.findViewById<LinearLayout>(R.id.llLauncher)
				val wrapper = activity.findViewById<View>(R.id.llBfbSpinnerWrapper)
				assertEquals(View.VISIBLE, wrapper.visibility)
				assertEquals(0, llLauncher.indexOfChild(wrapper))

				val theme = DependencyContainer.of(activity).themeManager.current
				assertEquals(
					activity.resources.getDimension(theme.panel_height).toInt(),
					activity.findViewById<LinearLayout>(R.id.llPanel).layoutParams.height)
			}
		}
	}

	@Test fun gnomeThemeHidesPanelBfbAndMovesLauncherBfbToTheBottom() {
		launch("gnome").use { scenario ->
			scenario.onActivity { activity ->
				// The refined GNOME theme (c049c82) removed the panel BFB.
				assertEquals(View.GONE,
					activity.findViewById<TextView>(R.id.tvPanelBfb).visibility)

				assertEquals(View.GONE,
					activity.findViewById<LinearLayout>(R.id.llDashRibbon).visibility)
				assertEquals(View.GONE,
					activity.findViewById<View>(R.id.lalPreferences).visibility)

				// launcher_bfb_location BOTTOM: the bfb wrapper is moved from the
				// top of the launcher to just before the preferences launcher.
				val llLauncher = activity.findViewById<LinearLayout>(R.id.llLauncher)
				val wrapperIndex = llLauncher.indexOfChild(
					activity.findViewById(R.id.llBfbSpinnerWrapper))
				val preferencesIndex = llLauncher.indexOfChild(
					activity.findViewById(R.id.lalPreferences))
				assertTrue(wrapperIndex > 0)
				assertEquals(preferencesIndex - 1, wrapperIndex)
			}
		}
	}

	@Test fun cinnamonThemeHidesPanelAndRibbonButShowsPreferences() {
		launch("cinnamon").use { scenario ->
			scenario.onActivity { activity ->
				assertEquals(View.GONE,
					activity.findViewById<TextView>(R.id.tvPanelBfb).visibility)
				assertEquals(View.GONE,
					activity.findViewById<LinearLayout>(R.id.llDashRibbon).visibility)
				assertEquals(View.VISIBLE,
					activity.findViewById<View>(R.id.lalPreferences).visibility)
				assertEquals(View.VISIBLE,
					activity.findViewById<View>(R.id.llBfbSpinnerWrapper).visibility)
				// 0dp panel
				assertEquals(0,
					activity.findViewById<LinearLayout>(R.id.llPanel).layoutParams.height)
			}
		}
	}

	@Test fun elementaryThemeShowsPanelBfbAndHidesPreferences() {
		launch("elementary").use { scenario ->
			scenario.onActivity { activity ->
				assertEquals(View.VISIBLE,
					activity.findViewById<TextView>(R.id.tvPanelBfb).visibility)
				assertEquals(View.GONE,
					activity.findViewById<LinearLayout>(R.id.llDashRibbon).visibility)
				assertEquals(View.GONE,
					activity.findViewById<View>(R.id.lalPreferences).visibility)
			}
		}
	}

	@Test fun applyDashIconWidthScalesTheGridColumnWidthByDensity() {
		launch("default").use { scenario ->
			scenario.onActivity { activity ->
				val container = DependencyContainer.of(activity)
				val applier = ThemeApplier(activity, activity.viewFinder,
					container.themeManager.current,
					LauncherEdgeController(activity, activity.viewFinder,
						container.themeManager.current, container.prefs))

				ActivityTestSupport.layoutDashApps(activity)
				applier.applyDashIconWidth(40)

				val density = activity.resources.displayMetrics.density
				assertEquals(Math.round((80 + 40) * density),
					requestedColumnWidth(activity.findViewById(R.id.gvDashHomeApps)))
			}
		}
	}

	@Test fun dashIconWidthPreferenceIsAppliedOnLaunch() {
		ActivityTestSupport.launchHome(configurePrefs = {
			it.putInt(Preference.DASHICON_WIDTH.getName(), 64)
		}).use { scenario ->
			scenario.onActivity { activity ->
				ActivityTestSupport.layoutDashApps(activity)
				val density = activity.resources.displayMetrics.density
				assertEquals(Math.round((80 + 64) * density),
					requestedColumnWidth(activity.findViewById(R.id.gvDashHomeApps)))
			}
		}
	}
}

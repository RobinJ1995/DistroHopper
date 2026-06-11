package be.robinj.distrohopper.home

import android.view.View
import android.widget.LinearLayout
import androidx.core.graphics.Insets
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.theme.Location
import be.robinj.distrohopper.widgets.WidgetsContainer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class LauncherEdgeControllerTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() { this.scenario = ActivityTestSupport.launchHome() }
	@After fun tearDown() { this.scenario.close() }

	private fun controller(activity: HomeActivity): LauncherEdgeController {
		val container = DependencyContainer.of(activity)
		return LauncherEdgeController(activity, activity.viewFinder,
			container.themeManager.current, container.prefs)
	}

	@Test fun launcherEdgeBottomReorientsTheLauncherHorizontally() {
		this.scenario.onActivity { activity ->
			val controller = this.controller(activity)

			controller.applyLauncherEdge(Location.BOTTOM, true)

			assertEquals(Location.BOTTOM, controller.launcherEdge)
			assertEquals(LinearLayout.HORIZONTAL,
				activity.findViewById<LinearLayout>(R.id.llLauncher).orientation)
			assertEquals(LinearLayout.VERTICAL,
				activity.findViewById<LinearLayout>(R.id.llLauncherAndDashContainer).orientation)
		}
	}

	@Test fun panelEdgeNoneHidesThePanel() {
		this.scenario.onActivity { activity ->
			this.controller(activity).applyPanelEdge(Location.NONE)

			assertEquals(View.GONE, activity.findViewById<LinearLayout>(R.id.llPanel).visibility)
		}
	}

	@Test fun panelEdgeTopAppliesTheOpacityPreference() {
		this.scenario.onActivity { activity ->
			val container = DependencyContainer.of(activity)
			container.prefs.edit { putInt(Preference.PANEL_OPACITY.getName(), 50) }

			this.controller(activity).applyPanelEdge(Location.TOP)

			assertEquals(0.5F, activity.findViewById<LinearLayout>(R.id.llPanel).alpha, 0.001F)
		}
	}

	@Test fun widgetAreaInsetsFollowTheLauncherEdgeAndNavigationInsets() {
		this.scenario.onActivity { activity ->
			val controller = this.controller(activity)
			val vgWidgets = activity.findViewById<WidgetsContainer>(R.id.vgWidgets)
			val llLauncher = activity.findViewById<View>(R.id.llLauncher)

			controller.navigationInsets = Insets.of(10, 0, 20, 30)

			controller.applyLauncherEdge(Location.LEFT, true)
			controller.updateWidgetAreaInsets(vgWidgets, llLauncher)
			assertEquals(llLauncher.width + 10, vgWidgets.paddingLeft)
			assertEquals(20, vgWidgets.paddingRight)
			assertEquals(30, vgWidgets.paddingBottom)

			controller.applyLauncherEdge(Location.BOTTOM, true)
			controller.updateWidgetAreaInsets(vgWidgets, llLauncher)
			assertEquals(10, vgWidgets.paddingLeft)
			assertEquals(llLauncher.height + 30, vgWidgets.paddingBottom)
		}
	}
}

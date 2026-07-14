package be.robinj.distrohopper.home

import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.desktop.dash.DashGrid
import be.robinj.distrohopper.desktop.launcher.LauncherIconGrid
import be.robinj.distrohopper.preferences.Preference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

/**
 * Preference changes that apply live through HomeViewModel's flows, without
 * recreating the activity (see HomeStateBinder).
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class ReactivePrefsTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() { this.scenario = ActivityTestSupport.launchHome() }
	@After fun tearDown() { this.scenario.close() }

	@Test fun panelOpacityAppliesWithoutRecreation() {
		this.scenario.onActivity { activity ->
			DependencyContainer.of(activity).prefs.edit {
				putInt(Preference.PANEL_OPACITY.getName(), 40)
			}
		}
		ActivityTestSupport.drainTasks()

		this.scenario.onActivity { activity ->
			assertEquals(0.4F, activity.findViewById<LinearLayout>(R.id.llPanel).alpha, 0.001F)
		}
	}

	@Test fun launcherIconPresetResizesThePanelCloseButton() {
		var widthBefore = 0
		this.scenario.onActivity { activity ->
			widthBefore = activity.findViewById<ImageButton>(R.id.ibPanelDashClose).layoutParams.width
			// "Huge" (0) resolves to a different slot count than the default on the
			// test screen (see the precondition below), so the width must change.
			DependencyContainer.of(activity).prefs.edit {
				putInt(Preference.LAUNCHER_ICON_PRESET.getName(), 0)
			}
		}
		ActivityTestSupport.drainTasks()

		this.scenario.onActivity { activity ->
			val sw = activity.resources.configuration.smallestScreenWidthDp
			assertNotEquals("test screen must distinguish Huge from Default",
				LauncherIconGrid.countForPreset(sw, 0),
				LauncherIconGrid.countForPreset(sw, LauncherIconGrid.DEFAULT_PRESET))

			val width = activity.findViewById<ImageButton>(R.id.ibPanelDashClose).layoutParams.width
			assertEquals(LauncherIconGrid.iconSizePx(activity), width)
			// Guards against the startup sizing masking a dead reactive path.
			assertNotEquals(widthBefore, width)
		}
	}

	@Test fun dashGridColumnsAppliesToTheDashGrid() {
		this.scenario.onActivity { activity ->
			DependencyContainer.of(activity).prefs.edit {
				putInt(Preference.DASH_GRID_COLUMNS.getName(), 5)
			}
		}
		ActivityTestSupport.drainTasks()

		this.scenario.onActivity { activity ->
			// The dash grid lives on a lazily-laid-out pager page; the pref change
			// re-applied the column count, and the page picks it up on layout.
			ActivityTestSupport.layoutDashApps(activity)

			assertEquals(DashGrid.dashColumns(activity),
				activity.findViewById<android.widget.GridView>(R.id.gvDashHomeApps).numColumns)
		}
	}
}

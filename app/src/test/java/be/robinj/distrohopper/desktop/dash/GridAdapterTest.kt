package be.robinj.distrohopper.desktop.dash

import android.widget.GridView
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class GridAdapterTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
	@After fun tearDown() { scenario.close() }

	@Test fun getViewBindsTheAppLabelIconAndTag() {
		scenario.onActivity { activity ->
			val gvDashHomeApps = activity.findViewById<GridView>(R.id.gvDashHomeApps)
			val adapter = gvDashHomeApps.adapter as GridAdapter
			val app = activity.appManager[0]

			val view = adapter.getView(0, null, gvDashHomeApps)

			assertEquals(app.label, view.findViewById<TextView>(R.id.tvLabel).text.toString())
			assertTrue(view.tag is AppLauncher)
			assertEquals(app.label, (view.tag as AppLauncher).label)
		}
	}

	@Test fun getViewSizesTheCellSquareByDensityAndIconWidthPreference() {
		scenario.onActivity { activity ->
			val gvDashHomeApps = activity.findViewById<GridView>(R.id.gvDashHomeApps)
			val adapter = gvDashHomeApps.adapter as GridAdapter

			val view = adapter.getView(0, null, gvDashHomeApps)

			// 80 base + the DASHICON_WIDTH default of 24, density-scaled
			val expected = Math.round(
				(80 + 24) * activity.resources.displayMetrics.density)
			assertEquals(expected, view.layoutParams.width)
			assertEquals(view.layoutParams.width, view.layoutParams.height)
		}
	}

	@Test fun getViewRecyclesTheConvertView() {
		scenario.onActivity { activity ->
			val gvDashHomeApps = activity.findViewById<GridView>(R.id.gvDashHomeApps)
			val adapter = gvDashHomeApps.adapter as GridAdapter

			val first = adapter.getView(0, null, gvDashHomeApps)
			val second = adapter.getView(1, first, gvDashHomeApps)

			assertSame(first, second)
			assertEquals(activity.appManager[1].label,
				second.findViewById<TextView>(R.id.tvLabel).text.toString())
		}
	}

	@Test fun adapterListsAllInstalledApps() {
		scenario.onActivity { activity ->
			val adapter = activity.findViewById<GridView>(R.id.gvDashHomeApps)
				.adapter as GridAdapter

			assertEquals(activity.appManager.size(), adapter.count)
			assertTrue(adapter.count > 0)
		}
	}
}

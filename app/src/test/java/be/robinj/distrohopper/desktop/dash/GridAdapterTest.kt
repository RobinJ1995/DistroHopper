package be.robinj.distrohopper.desktop.dash

import android.database.DataSetObserver
import android.view.ViewGroup
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.App
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class GridAdapterTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
	@After fun tearDown() { scenario.close() }

	// GridAdapter is exercised directly with a throwaway parent rather than
	// through the dash grid: that grid now lives on a lazily-laid-out pager
	// page, and the adapter's behaviour is independent of where it is bound.
	// The dash grid now holds DashItems (apps + folders); wrap the installed
	// apps as plain AppItems in install order so the index-based assertions hold.
	private fun gridAdapter(activity: HomeActivity): GridAdapter =
		GridAdapter(activity, activity,
			activity.appManager.installedApps.map { DashItem.AppItem(it) as DashItem }.toMutableList())

	@Test fun getViewBindsTheAppLabelIconAndTag() {
		scenario.onActivity { activity ->
			val parent = GridView(activity)
			val adapter = this.gridAdapter(activity)
			val app = activity.appManager[0]

			val view = adapter.getView(0, null, parent)

			assertEquals(app.label, view.findViewById<TextView>(R.id.tvLabel).text.toString())
			assertTrue(view.tag is DashItem.AppItem)
			assertEquals(app.label, (view.tag as DashItem.AppItem).app.label)
		}
	}

	@Test fun getViewLoadsAnUncachedIconOffTheBindAndRebindsWithIt() {
		scenario.onActivity { activity ->
			val parent = GridView(activity)
			// Built without the caches, so its icon is not in memory //
			val app = App(activity, activity.appManager,
				ActivityTestSupport.resolveInfo("com.example.uncached", "UncachedActivity", "Uncached"))
			val adapter = GridAdapter(activity, activity, mutableListOf<DashItem>(DashItem.AppItem(app)))
			var rebinds = 0
			adapter.registerDataSetObserver(object : DataSetObserver() {
				override fun onChanged() { rebinds++ }
			})

			ShadowLooper.pauseMainLooper()
			val view = adapter.getView(0, null, parent)

			assertNull(view.findViewById<ImageView>(R.id.imgIcon).drawable)
			assertNull(app.peekIcon())

			ShadowLooper.unPauseMainLooper()
			ActivityTestSupport.drainTasks()

			assertNotNull(app.peekIcon())
			assertEquals(1, rebinds)
			assertSame(app.peekIcon().drawable,
				adapter.getView(0, view, parent).findViewById<ImageView>(R.id.imgIcon).drawable)
		}
	}

	@Test fun getViewSizesTheCellFromTheDashGrid() {
		scenario.onActivity { activity ->
			// A bare parent has no laid-out column width, so the cell height
			// falls back to DashGrid's screen-derived cell size; the width
			// fills the column (MATCH_PARENT) for square, stretched cells.
			val parent = GridView(activity)
			val adapter = this.gridAdapter(activity)

			val view = adapter.getView(0, null, parent)

			assertEquals(DashGrid.cellSizePx(activity), view.layoutParams.height)
			assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, view.layoutParams.width)
		}
	}

	@Test fun getViewRecyclesTheConvertView() {
		scenario.onActivity { activity ->
			val parent = GridView(activity)
			val adapter = this.gridAdapter(activity)

			val first = adapter.getView(0, null, parent)
			val second = adapter.getView(1, first, parent)

			assertSame(first, second)
			assertEquals(activity.appManager[1].label,
				second.findViewById<TextView>(R.id.tvLabel).text.toString())
		}
	}

	@Test fun adapterListsAllInstalledApps() {
		scenario.onActivity { activity ->
			val adapter = this.gridAdapter(activity)

			assertEquals(activity.appManager.size(), adapter.count)
			assertTrue(adapter.count > 0)
		}
	}
}

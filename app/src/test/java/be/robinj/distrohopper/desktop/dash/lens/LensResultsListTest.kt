package be.robinj.distrohopper.desktop.dash.lens

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.GridView
import android.widget.ScrollView
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.desktop.dash.DashGrid
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class LensResultsListTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
	@After fun tearDown() { scenario.close() }

	private class StubLens(context: Context) : Lens(context) {
		override val key = "StubLens"
		override val type = LensType.NETWORK
		override suspend fun search(query: String, maxResults: Int, emitter: LensResultEmitter) {}
		override fun getName() = "Stub lens"
		override fun getDescription() = "Test lens"
	}

	private fun result(context: Context, name: String) = LensSearchResult(context, name, "", ColorDrawable(Color.RED))

	@Test fun sectionsAppearInOrderAndKeepTheirViewsAsResultsStreamIn() {
		scenario.onActivity { activity ->
			val list = activity.findViewById<LensResultsList>(R.id.llDashHomeLensResults)
			val sections = mutableListOf<LensSearchResultCollection>()
			val adapter = CollectionGridAdapter(activity, sections)
			list.setAdapter(adapter)

			val first = mutableListOf(result(activity, "A1"))
			sections.add(LensSearchResultCollection(StubLens(activity), first))
			adapter.notifyDataSetChanged()
			val firstView = list.getChildAt(0)
			val firstGrid = firstView.findViewById<GridView>(R.id.gvResults).adapter

			first.add(result(activity, "A2"))
			sections.add(LensSearchResultCollection(StubLens(activity), "Second", listOf(result(activity, "B1"))))
			adapter.notifyDataSetChanged()

			assertEquals(2, list.childCount)
			assertSame(firstView, list.getChildAt(0))
			assertSame(firstGrid, firstView.findViewById<GridView>(R.id.gvResults).adapter)
			assertEquals(2, firstGrid.count)
			assertEquals("B1", (list.getChildAt(1).findViewById<GridView>(R.id.gvResults)
				.adapter.getItem(0) as LensSearchResult).name)
		}
	}

	@Test fun aNewSearchReplacesTheOldSectionsAndScrollsToTheTop() {
		scenario.onActivity { activity ->
			val list = activity.findViewById<LensResultsList>(R.id.llDashHomeLensResults)
			val old = CollectionGridAdapter(activity, listOf(
				LensSearchResultCollection(StubLens(activity), listOf(result(activity, "Old")))))
			list.setAdapter(old)
			val oldView = list.getChildAt(0)
			(list.parent as ScrollView).scrollTo(0, 100)

			val fresh = mutableListOf<LensSearchResultCollection>()
			list.setAdapter(CollectionGridAdapter(activity, fresh))
			assertEquals(0, list.childCount)
			assertEquals(0, (list.parent as ScrollView).scrollY)

			// The old adapter no longer reaches the list //
			old.notifyDataSetChanged()
			assertEquals(0, list.childCount)
			assertEquals(null, oldView.parent)
		}
	}

	@Test fun draggingInsideASectionNeverScrollsItsGrid() {
		scenario.onActivity { activity ->
			val list = activity.findViewById<LensResultsList>(R.id.llDashHomeLensResults)
			list.setAdapter(CollectionGridAdapter(activity, listOf(LensSearchResultCollection(
				StubLens(activity), (1..10).map { result(activity, "R$it") }))))
			list.measure(
				View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
				View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
			list.layout(0, 0, list.measuredWidth, list.measuredHeight)
			val grid = list.getChildAt(0).findViewById<GridView>(R.id.gvResults)
			val top = grid.getChildAt(0).top

			// A slow upward drag starting on the grid's first row //
			val x = 540F
			val y = grid.bottom - 10F
			val t = SystemClock.uptimeMillis()
			fun send(action: Int, dy: Float, dt: Long) = MotionEvent.obtain(t, t + dt, action, x, y + dy, 0).also {
				list.dispatchTouchEvent(it)
				it.recycle()
			}
			send(MotionEvent.ACTION_DOWN, 0F, 0)
			for (i in 1..20) send(MotionEvent.ACTION_MOVE, -15F * i, 16L * i)
			// Held still before release, so an unfixed grid scrolls without flinging //
			send(MotionEvent.ACTION_MOVE, -300F, 1400)
			send(MotionEvent.ACTION_UP, -300F, 1500)
			grid.layout(grid.left, grid.top, grid.right, grid.bottom)

			assertEquals(0, grid.firstVisiblePosition)
			assertEquals(top, grid.getChildAt(0).top)
		}
	}

	@Test fun rotatingReappliesTheColumnCountToEverySection() {
		scenario.onActivity { activity ->
			val list = activity.findViewById<LensResultsList>(R.id.llDashHomeLensResults)
			list.setAdapter(CollectionGridAdapter(activity, listOf(
				LensSearchResultCollection(StubLens(activity), listOf(result(activity, "A"))))))
			val grid = list.getChildAt(0).findViewById<GridView>(R.id.gvResults)
			val portrait = DashGrid.dashColumns(activity)

			RuntimeEnvironment.setQualifiers("+land")
			val landscape = DashGrid.dashColumns(activity)
			assertNotEquals(portrait, landscape)
			list.dispatchConfigurationChanged(activity.resources.configuration)

			// GridView.getNumColumns() only reflects the requested count once laid out //
			grid.measure(
				View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.EXACTLY),
				View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
			grid.layout(0, 0, grid.measuredWidth, grid.measuredHeight)
			assertEquals(landscape, grid.numColumns)
		}
	}
}

package be.robinj.distrohopper.desktop.dash.lens

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.view.View
import android.widget.GridView
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.desktop.dash.DashGrid
import java.net.UnknownHostException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class CollectionGridAdapterTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() { scenario = ActivityTestSupport.launchHome() }
	@After fun tearDown() { scenario.close() }

	private class StubLens(context: Context) : Lens(context) {
		override val type = LensType.NETWORK
		override suspend fun search(query: String, maxResults: Int, emitter: LensResultEmitter) {}
		override fun getName() = "Stub lens"
		override fun getDescription() = "Test lens"
	}

	private fun getView(activity: HomeActivity, collection: LensSearchResultCollection): View {
		val adapter = CollectionGridAdapter(activity, listOf(collection))
		return adapter.getView(0,
			null, activity.findViewById(R.id.lvDashHomeLensResults))
	}

	@Test fun resultsArePopulatedIntoTheInnerGridUnderTheLensName() {
		scenario.onActivity { activity ->
			val lens = StubLens(activity)
			val result = LensSearchResult(activity, "A result",
				"https://example.com", ColorDrawable(Color.RED))

			val view = getView(activity, LensSearchResultCollection(lens, listOf(result)))

			assertEquals(View.VISIBLE, view.visibility)
			assertEquals("Stub lens",
				view.findViewById<TextView>(R.id.tvLabel).text.toString())
			val gvResults = view.findViewById<GridView>(R.id.gvResults)
			assertEquals(1, gvResults.adapter.count)
			assertNotNull(gvResults.onItemClickListener)
		}
	}

	@Test fun lensResultsUseTheSameColumnCountAsTheDashAppsGrid() {
		scenario.onActivity { activity ->
			val lens = StubLens(activity)
			val result = LensSearchResult(activity, "A result",
				"https://example.com", ColorDrawable(Color.RED))

			val view = getView(activity, LensSearchResultCollection(lens, listOf(result)))
			val gvResults = view.findViewById<GridView>(R.id.gvResults)

			// GridView.getNumColumns() only reflects the requested count once laid
			// out, so measure/layout the collection view first //
			view.measure(
				View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
				View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.AT_MOST))
			view.layout(0, 0, view.measuredWidth, view.measuredHeight)

			// Both the apps grid and the lens grids are sized via DashGridSizer //
			assertEquals(DashGrid.dashColumns(activity), gvResults.numColumns)
		}
	}

	@Test fun aFailedSearchShowsASingleSyntheticErrorResult() {
		scenario.onActivity { activity ->
			val collection = LensSearchResultCollection(
				StubLens(activity), RuntimeException("backend down"))

			val view = getView(activity, collection)

			assertEquals(View.VISIBLE, view.visibility)
			val gvResults = view.findViewById<GridView>(R.id.gvResults)
			assertEquals(1, gvResults.adapter.count)
			val error = gvResults.adapter.getItem(0) as LensSearchResult
			// The synthetic error tile is titled by the exception class //
			assertEquals("RuntimeException", error.name)
			// Tapping it shows the failure dialog rather than navigating //
			assertNotNull(gvResults.onItemClickListener)
		}
	}

	@Test fun networkErrorsAreHiddenEntirelyWhileOffline() {
		scenario.onActivity { activity ->
			val connectivityManager = activity
				.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
			Shadows.shadowOf(connectivityManager).setActiveNetworkInfo(null)
			val collection = LensSearchResultCollection(
				StubLens(activity), UnknownHostException("no dns"))

			val view = getView(activity, collection)

			assertEquals(View.GONE, view.visibility)
		}
	}
}

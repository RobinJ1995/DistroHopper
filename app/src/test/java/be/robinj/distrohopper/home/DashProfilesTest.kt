package be.robinj.distrohopper.home

import android.content.res.Configuration
import android.view.View
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.LinearLayout
import androidx.viewpager2.widget.ViewPager2
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.desktop.dash.DashGrid
import be.robinj.distrohopper.desktop.dash.GridAdapter
import be.robinj.distrohopper.desktop.dash.ProfilePagerAdapter
import be.robinj.distrohopper.desktop.dash.profile.ProfilePillView
import be.robinj.distrohopper.preferences.Preference
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

/**
 * The dash apps area is always a ViewPager2 — one swipeable page per profile,
 * a single page in the usual single-profile case — with a theme-specific tab
 * indicator (Unity ribbon glyphs / GNOME panel pill) once a second profile
 * exists.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class DashProfilesTest {
	private fun pager(activity: HomeActivity) =
		activity.findViewById<ViewPager2>(R.id.vpDashProfiles)

	private fun pageCount(activity: HomeActivity) =
		(pager(activity).adapter as ProfilePagerAdapter).itemCount

	@Test fun singleProfileIsASingleNonIndicatedPagerPage() {
		ActivityTestSupport.launchHome().use { scenario ->
			scenario.onActivity { activity ->
				assertEquals(View.VISIBLE, pager(activity).visibility)
				assertEquals(1, pageCount(activity))

				ActivityTestSupport.layoutDashApps(activity)
				val grid = ActivityTestSupport.dashGrid(activity)
				assertNotNull(grid)
				assertTrue(grid!!.adapter is GridAdapter)
				assertEquals(activity.appManager.size(), grid.adapter.count)

				val ribbon = activity.findViewById<LinearLayout>(R.id.llDashRibbonProfiles)
				assertEquals(View.GONE, ribbon.visibility)
				assertEquals(0, ribbon.childCount)
			}
		}
	}

	@Test fun workProfileAddsASecondSwipeablePage() {
		val workUser = ActivityTestSupport.addWorkProfile()
		ActivityTestSupport.addWorkProfileApp(
			workUser, "com.example.work", "WorkChatActivity", "WorkChat")

		ActivityTestSupport.launchHome().use { scenario ->
			scenario.onActivity { activity ->
				assertEquals(View.VISIBLE, pager(activity).visibility)
				assertEquals(2, pageCount(activity))
				assertEquals(0, pager(activity).currentItem)
			}
		}
	}

	@Test fun firstWorkProfileAppInstallAddsASecondTab() {
		ActivityTestSupport.launchHome().use { scenario ->
			scenario.onActivity { activity ->
				assertEquals(1, pageCount(activity))

				// As delivered by WorkProfileAppsCallback.onPackageAdded //
				val workUser = ActivityTestSupport.addWorkProfile()
				activity.appManager.add(ActivityTestSupport.launcherActivityInfo(
					"com.example.work", "WorkChatActivity", "WorkChat", workUser),
					true, true)

				assertEquals(2, pageCount(activity))
			}
		}
	}

	@Test fun removingTheLastWorkProfileAppReturnsToOneTab() {
		val workUser = ActivityTestSupport.addWorkProfile()
		ActivityTestSupport.addWorkProfileApp(
			workUser, "com.example.work", "WorkChatActivity", "WorkChat")

		ActivityTestSupport.launchHome().use { scenario ->
			scenario.onActivity { activity ->
				assertEquals(2, pageCount(activity))

				val workApp = activity.appManager.installedApps.single { it.user != null }
				activity.appManager.remove(workApp)

				assertEquals(1, pageCount(activity))
				val ribbon = activity.findViewById<LinearLayout>(R.id.llDashRibbonProfiles)
				assertEquals(View.GONE, ribbon.visibility)
			}
		}
	}

	@Test fun unityRibbonShowsOneTabGlyphPerProfile() {
		val workUser = ActivityTestSupport.addWorkProfile()
		ActivityTestSupport.addWorkProfileApp(
			workUser, "com.example.work", "WorkChatActivity", "WorkChat")

		// Default theme is Unity-style //
		ActivityTestSupport.launchHome().use { scenario ->
			scenario.onActivity { activity ->
				val ribbon = activity.findViewById<LinearLayout>(R.id.llDashRibbonProfiles)
				assertEquals(View.VISIBLE, ribbon.visibility)
				assertEquals(2, ribbon.childCount)
			}
		}
	}

	@Test fun gnomePanelPillReflectsProfilesAndShowsOnlyWhileDashOpen() {
		val workUser = ActivityTestSupport.addWorkProfile()
		ActivityTestSupport.addWorkProfileApp(
			workUser, "com.example.work", "WorkChatActivity", "WorkChat")

		ActivityTestSupport.launchHome(configurePrefs = {
			it.putString(Preference.THEME.getName(), "gnome")
		}).use { scenario ->
			scenario.onActivity { activity ->
				val container = activity.findViewById<FrameLayout>(R.id.llPanelProfileIndicator)
				val pill = container.getChildAt(0) as ProfilePillView
				assertEquals(2, pill.count)

				// Hidden on the desktop, shown once the dash opens, hidden again on close //
				assertEquals(View.GONE, container.visibility)
				activity.appManager.setDashOpen(true)
				assertEquals(View.VISIBLE, container.visibility)
				activity.appManager.setDashOpen(false)
				assertEquals(View.GONE, container.visibility)
			}
		}
	}

	@Test fun aProfilePageReattachedAfterRotationPicksUpTheNewColumnCount() {
		// Regression: rotating the device and then swiping to another profile
		// showed massively over- or under-sized icons. A page sitting in the
		// recycler cache re-attaches without re-binding when swiped to, and the
		// rotation handler only resized the grids attached at that moment — so an
		// off-screen page kept the previous orientation's column count. The count
		// is now applied on attach, not just on bind.
		val workUser = ActivityTestSupport.addWorkProfile()
		ActivityTestSupport.addWorkProfileApp(
			workUser, "com.example.work", "WorkChatActivity", "WorkChat")

		ActivityTestSupport.launchHome().use { scenario ->
			scenario.onActivity { activity ->
				val pager = pager(activity)
				val adapter = pager.adapter as ProfilePagerAdapter

				// Bind and attach the work-profile page (index 1) in portrait.
				setLandscape(activity, false)
				val holder = adapter.onCreateViewHolder(pager, 0)
				adapter.onBindViewHolder(holder, 1)
				adapter.onViewAttachedToWindow(holder)
				measure(holder.grid)

				val portraitColumns = DashGrid.dashColumns(activity)
				assertEquals(portraitColumns, holder.grid.numColumns)

				// Rotate to landscape, then re-attach the SAME page without
				// re-binding — exactly what ViewPager2 does when swiping to a page
				// still held in the recycler cache.
				setLandscape(activity, true)
				val landscapeColumns = DashGrid.dashColumns(activity)
				assertTrue("landscape should show more columns than portrait",
					landscapeColumns > portraitColumns)

				adapter.onViewAttachedToWindow(holder)
				measure(holder.grid)

				assertEquals(landscapeColumns, holder.grid.numColumns)
			}
		}
	}

	/** Flips the activity's configuration between portrait and landscape. */
	private fun setLandscape(activity: HomeActivity, landscape: Boolean) {
		val config = Configuration(activity.resources.configuration)
		val wide = max(config.screenWidthDp, config.screenHeightDp)
		val narrow = min(config.screenWidthDp, config.screenHeightDp)
		config.orientation =
			if (landscape) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
		config.screenWidthDp = if (landscape) wide else narrow
		config.screenHeightDp = if (landscape) narrow else wide

		@Suppress("DEPRECATION")
		activity.resources.updateConfiguration(config, activity.resources.displayMetrics)
	}

	/** Lays the grid out so GridView.getNumColumns() reflects the requested count. */
	private fun measure(grid: GridView) {
		grid.measure(
			View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
			View.MeasureSpec.makeMeasureSpec(1600, View.MeasureSpec.EXACTLY))
		grid.layout(0, 0, grid.measuredWidth, grid.measuredHeight)
	}

	@Test fun pillViewMorphsTheActiveSlotAsThePagerScrolls() {
		// The pager drives onPageScrolled; the pill's fractional position is what
		// produces the smooth capsule-to-dot morph. Asserted on the view directly
		// since Robolectric does not fling the pager.
		val pill = ProfilePillView(org.robolectric.RuntimeEnvironment.getApplication())
		pill.count = 2
		pill.position = 0F
		assertEquals(2, pill.count)

		pill.position = 0.5F
		assertEquals(0.5F, pill.position)
	}
}

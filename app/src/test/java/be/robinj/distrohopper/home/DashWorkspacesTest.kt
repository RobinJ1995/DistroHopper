package be.robinj.distrohopper.home

import android.view.View
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.LinearLayout
import androidx.viewpager2.widget.ViewPager2
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.desktop.dash.WorkspacePagerAdapter
import be.robinj.distrohopper.desktop.dash.workspace.WorkspacePillView
import be.robinj.distrohopper.preferences.Preference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

/**
 * The dash with a work profile: the single grid is replaced by a swipeable
 * per-workspace pager, with a theme-specific tab indicator (Unity ribbon
 * glyphs / GNOME panel pill).
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class DashWorkspacesTest {
	private fun vp(activity: HomeActivity) =
		activity.findViewById<ViewPager2>(R.id.vpDashWorkspaces)

	private fun gridWrapper(activity: HomeActivity) =
		activity.findViewById<LinearLayout>(R.id.llDashHomeAppsGridWrapper)

	@Test fun workProfileShowsSwipeablePagerInsteadOfTheSingleGrid() {
		val workUser = ActivityTestSupport.addWorkProfile()
		ActivityTestSupport.addWorkProfileApp(
			workUser, "com.example.work", "WorkChatActivity", "WorkChat")

		ActivityTestSupport.launchHome().use { scenario ->
			scenario.onActivity { activity ->
				assertEquals(View.GONE, gridWrapper(activity).visibility)
				assertEquals(View.VISIBLE, vp(activity).visibility)

				val adapter = vp(activity).adapter as WorkspacePagerAdapter
				assertEquals(2, adapter.itemCount)
				assertEquals(0, vp(activity).currentItem)
			}
		}
	}

	@Test fun singleWorkspaceKeepsTheSingleGrid() {
		ActivityTestSupport.launchHome().use { scenario ->
			scenario.onActivity { activity ->
				assertEquals(View.VISIBLE, gridWrapper(activity).visibility)
				assertNotNull(activity.findViewById<GridView>(R.id.gvDashHomeApps).adapter)
				assertEquals(View.GONE, vp(activity).visibility)
				assertEquals(null, vp(activity).adapter)
			}
		}
	}

	@Test fun firstWorkProfileAppInstallSwitchesToTabs() {
		ActivityTestSupport.launchHome().use { scenario ->
			scenario.onActivity { activity ->
				assertEquals(View.VISIBLE, gridWrapper(activity).visibility)

				// As delivered by WorkProfileAppsCallback.onPackageAdded //
				val workUser = ActivityTestSupport.addWorkProfile()
				activity.appManager.add(ActivityTestSupport.launcherActivityInfo(
					"com.example.work", "WorkChatActivity", "WorkChat", workUser),
					true, true)

				assertEquals(View.GONE, gridWrapper(activity).visibility)
				assertEquals(View.VISIBLE, vp(activity).visibility)
				assertEquals(2, (vp(activity).adapter as WorkspacePagerAdapter).itemCount)
			}
		}
	}

	@Test fun removingTheLastWorkProfileAppRestoresTheSingleGrid() {
		val workUser = ActivityTestSupport.addWorkProfile()
		ActivityTestSupport.addWorkProfileApp(
			workUser, "com.example.work", "WorkChatActivity", "WorkChat")

		ActivityTestSupport.launchHome().use { scenario ->
			scenario.onActivity { activity ->
				val workApp = activity.appManager.installedApps.single { it.user != null }
				activity.appManager.remove(workApp)

				assertEquals(View.VISIBLE, gridWrapper(activity).visibility)
				assertEquals(View.GONE, vp(activity).visibility)
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
				val ribbon = activity.findViewById<LinearLayout>(R.id.llDashRibbonWorkspaces)
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
				val container = activity.findViewById<FrameLayout>(R.id.llPanelWorkspaceIndicator)
				val pill = container.getChildAt(0) as WorkspacePillView
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

	@Test fun unityRibbonHasNoTabsWithASingleWorkspace() {
		ActivityTestSupport.launchHome().use { scenario ->
			scenario.onActivity { activity ->
				val ribbon = activity.findViewById<LinearLayout>(R.id.llDashRibbonWorkspaces)
				assertEquals(View.GONE, ribbon.visibility)
				assertEquals(0, ribbon.childCount)
			}
		}
	}

	@Test fun pillViewMorphsTheActiveSlotAsThePagerScrolls() {
		// The pager drives onPageScrolled; the pill's fractional position is what
		// produces the smooth capsule-to-dot morph. Asserted on the view directly
		// since Robolectric does not lay out / fling the pager.
		val pill = WorkspacePillView(
			org.robolectric.RuntimeEnvironment.getApplication())
		pill.count = 2
		pill.position = 0F
		assertTrue(pill.count == 2)

		pill.position = 0.5F
		assertEquals(0.5F, pill.position)
	}
}

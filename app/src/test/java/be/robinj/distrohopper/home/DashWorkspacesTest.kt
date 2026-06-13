package be.robinj.distrohopper.home

import android.view.View
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.App
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

/**
 * The dash with a work profile: one labelled app-list section per workspace,
 * stacked in the scrollable workspace container instead of the single grid.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class DashWorkspacesTest {
	private fun sectionLabels(activity: HomeActivity): List<String> {
		val sections = activity.findViewById<LinearLayout>(R.id.llDashHomeWorkspaces)
		return (0 until sections.childCount).map {
			sections.getChildAt(it)
				.findViewById<TextView>(R.id.tvWorkspaceLabel).text.toString()
		}
	}

	private fun sectionAppLabels(activity: HomeActivity, index: Int): List<String> {
		val sections = activity.findViewById<LinearLayout>(R.id.llDashHomeWorkspaces)
		val adapter = sections.getChildAt(index)
			.findViewById<GridView>(R.id.gvWorkspaceApps).adapter
		return (0 until adapter.count).map { (adapter.getItem(it) as App).label }
	}

	@Test fun workProfileAppsGetTheirOwnLabelledDashSection() {
		val workUser = ActivityTestSupport.addWorkProfile()
		ActivityTestSupport.addWorkProfileApp(
			workUser, "com.example.work", "WorkChatActivity", "WorkChat")

		ActivityTestSupport.launchHome().use { scenario ->
			scenario.onActivity { activity ->
				assertEquals(View.GONE,
					activity.findViewById<GridView>(R.id.gvDashHomeApps).visibility)
				assertEquals(View.VISIBLE,
					activity.findViewById<ScrollView>(R.id.svDashHomeWorkspaces).visibility)

				assertEquals(listOf("Personal", "Work"), sectionLabels(activity))

				val personalApps = sectionAppLabels(activity, 0)
				assertTrue(personalApps.contains("Alpha"))
				assertFalse(personalApps.contains("WorkChat"))

				assertEquals(listOf("WorkChat"), sectionAppLabels(activity, 1))

				val workApp = activity.appManager.installedApps.single { it.user != null }
				assertEquals(workUser, workApp.user)
			}
		}
	}

	@Test fun singleWorkspaceKeepsTheSingleDashGrid() {
		ActivityTestSupport.launchHome().use { scenario ->
			scenario.onActivity { activity ->
				val gvDashHomeApps = activity.findViewById<GridView>(R.id.gvDashHomeApps)
				assertEquals(View.VISIBLE, gvDashHomeApps.visibility)
				assertNotNull(gvDashHomeApps.adapter)
				assertEquals(View.GONE,
					activity.findViewById<ScrollView>(R.id.svDashHomeWorkspaces).visibility)
				assertEquals(0,
					activity.findViewById<LinearLayout>(R.id.llDashHomeWorkspaces).childCount)
			}
		}
	}

	@Test fun firstWorkProfileAppInstallRebuildsTheDashIntoSections() {
		ActivityTestSupport.launchHome().use { scenario ->
			scenario.onActivity { activity ->
				assertEquals(View.VISIBLE,
					activity.findViewById<GridView>(R.id.gvDashHomeApps).visibility)

				// As delivered by WorkProfileAppsCallback.onPackageAdded //
				val workUser = ActivityTestSupport.addWorkProfile()
				activity.appManager.add(ActivityTestSupport.launcherActivityInfo(
					"com.example.work", "WorkChatActivity", "WorkChat", workUser),
					true, true)

				assertEquals(View.GONE,
					activity.findViewById<GridView>(R.id.gvDashHomeApps).visibility)
				assertEquals(View.VISIBLE,
					activity.findViewById<ScrollView>(R.id.svDashHomeWorkspaces).visibility)
				assertEquals(listOf("Personal", "Work"), sectionLabels(activity))
				assertEquals(listOf("WorkChat"), sectionAppLabels(activity, 1))
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

				assertEquals(View.VISIBLE,
					activity.findViewById<GridView>(R.id.gvDashHomeApps).visibility)
				assertEquals(View.GONE,
					activity.findViewById<ScrollView>(R.id.svDashHomeWorkspaces).visibility)
			}
		}
	}
}

package be.robinj.distrohopper.desktop.dash.lens

import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.App
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

/**
 * The InstalledApps lens with a work profile: still a single lens, but its
 * results split into one collection (dash section) per workspace.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class InstalledAppsWorkspacesTest {
	@Test fun resultsAreSplitIntoOneCollectionPerWorkspace() {
		val workUser = ActivityTestSupport.addWorkProfile()
		ActivityTestSupport.addWorkProfileApp(
			workUser, "com.example.work", "AlphaWorkActivity", "Alpha Work")

		ActivityTestSupport.launchHome().use { scenario ->
			scenario.onActivity { activity ->
				val lens = InstalledApps(activity, activity.appManager)

				val collections = lens.searchCollections("alpha", 10)

				assertEquals(2, collections.size)
				assertEquals("Installed apps (Personal)", collections[0].name)
				assertEquals("Installed apps (Work)", collections[1].name)
				assertEquals(listOf("Alpha"), collections[0].results.map { it.name })
				assertEquals(listOf("Alpha Work"), collections[1].results.map { it.name })

				// Tapping a result launches the right profile's app //
				val workResult = collections[1].results.single()
				assertEquals(workUser, (workResult.obj as App).user)
			}
		}
	}

	@Test fun workspacesWithoutMatchesGetNoCollection() {
		val workUser = ActivityTestSupport.addWorkProfile()
		ActivityTestSupport.addWorkProfileApp(
			workUser, "com.example.work", "WorkChatActivity", "WorkChat")

		ActivityTestSupport.launchHome().use { scenario ->
			scenario.onActivity { activity ->
				val lens = InstalledApps(activity, activity.appManager)

				val collections = lens.searchCollections("alpha", 10)

				assertEquals(1, collections.size)
				assertEquals("Installed apps (Personal)", collections[0].name)
			}
		}
	}

	@Test fun aSingleWorkspaceKeepsTheSingleUnsuffixedCollection() {
		ActivityTestSupport.launchHome().use { scenario ->
			scenario.onActivity { activity ->
				val lens = InstalledApps(activity, activity.appManager)

				val collections = lens.searchCollections("alpha", 10)

				assertEquals(1, collections.size)
				assertEquals("Installed apps", collections[0].name)
				assertTrue(collections[0].results.isNotEmpty())
			}
		}
	}
}

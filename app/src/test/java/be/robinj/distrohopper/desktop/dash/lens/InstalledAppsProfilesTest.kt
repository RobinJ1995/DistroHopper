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
 * results split into one collection (dash section) per profile.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class InstalledAppsProfilesTest {
	@Test fun resultsAreSplitIntoOneSectionPerProfile() {
		val workUser = ActivityTestSupport.addWorkProfile()
		ActivityTestSupport.addWorkProfileApp(
			workUser, "com.example.work", "AlphaWorkActivity", "Alpha Work")

		ActivityTestSupport.launchHome().use { scenario ->
			scenario.onActivity { activity ->
				val lens = InstalledApps(activity, activity.appManager)

				val sections = lens.collect("alpha", 10).sections

				assertEquals(listOf("Installed apps (Personal)", "Installed apps (Work)"),
					sections.keys.toList())
				assertEquals(listOf("Alpha"), sections["Installed apps (Personal)"]!!.map { it.name })
				assertEquals(listOf("Alpha Work"), sections["Installed apps (Work)"]!!.map { it.name })

				// Tapping a result launches the right profile's app //
				val workResult = sections["Installed apps (Work)"]!!.single()
				assertEquals(workUser, (workResult.obj as App).user)
			}
		}
	}

	@Test fun profilesWithoutMatchesGetNoSection() {
		val workUser = ActivityTestSupport.addWorkProfile()
		ActivityTestSupport.addWorkProfileApp(
			workUser, "com.example.work", "WorkChatActivity", "WorkChat")

		ActivityTestSupport.launchHome().use { scenario ->
			scenario.onActivity { activity ->
				val lens = InstalledApps(activity, activity.appManager)

				val sections = lens.collect("alpha", 10).sections

				assertEquals(listOf("Installed apps (Personal)"), sections.keys.toList())
			}
		}
	}

	@Test fun aSingleProfileKeepsTheSingleUnsuffixedSection() {
		ActivityTestSupport.launchHome().use { scenario ->
			scenario.onActivity { activity ->
				val lens = InstalledApps(activity, activity.appManager)

				val sections = lens.collect("alpha", 10).sections

				assertEquals(listOf("Installed apps"), sections.keys.toList())
				assertTrue(sections["Installed apps"]!!.isNotEmpty())
			}
		}
	}
}

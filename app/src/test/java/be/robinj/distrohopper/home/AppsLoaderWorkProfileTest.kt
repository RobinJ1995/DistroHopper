package be.robinj.distrohopper.home

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.AppManager
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.cache.ICache
import be.robinj.distrohopper.cache.TestDrawableCache
import be.robinj.distrohopper.preferences.Preferences
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class AppsLoaderWorkProfileTest {
	private lateinit var application: Application
	private lateinit var scenario: ActivityScenario<HomeActivity>

	private class HashLabelCache : ICache<String>, MutableMap<String, String> by HashMap() {
		override fun getName() = "labels"
	}

	@Before fun setUp() {
		this.application = ApplicationProvider.getApplicationContext()
		this.scenario = ActivityTestSupport.launchHome()
	}

	@After fun tearDown() {
		this.scenario.close()
	}

	private fun loadApps(activity: HomeActivity, cacheName: String): AppManager =
		runBlocking {
			AppsLoader.loadApps(activity, activity.applicationContext, HashLabelCache(),
				TestDrawableCache(activity, cacheName)) { _, _ -> }
		}

	@Test fun workProfileAppsAreLoadedIntoTheirOwnWorkspace() {
		val workUser = ActivityTestSupport.addWorkProfile()
		ActivityTestSupport.addWorkProfileApp(
			workUser, "com.example.work", "WorkChatActivity", "WorkChat")

		this.scenario.onActivity { activity ->
			val appManager = this.loadApps(activity, "apps_loader_work_icons")

			val workApp = appManager.installedApps.single { it.user != null }
			assertEquals("WorkChat", workApp.label)
			assertEquals(workUser, workApp.user)
			assertEquals(listOf(null, workUser), appManager.workspaces)

			// The personal list keeps the work app out, and vice versa //
			assertTrue(appManager.repository.appsForWorkspace(null).none { it.user != null })
			assertEquals(listOf(workApp), appManager.repository.appsForWorkspace(workUser))
		}
	}

	@Test fun theSamePackageInBothProfilesStaysTwoDistinctApps() {
		val workUser = ActivityTestSupport.addWorkProfile()
		ActivityTestSupport.addWorkProfileApp(
			workUser, "com.example.alpha", "AlphaActivity", "Alpha")

		this.scenario.onActivity { activity ->
			val appManager = this.loadApps(activity, "apps_loader_both_profiles_icons")

			val twins = appManager.findAppsByPackageName("com.example.alpha")
			assertEquals(2, twins.size)
			assertEquals(setOf(null, workUser), twins.map { it.user }.toSet())
		}
	}

	@Test fun pinnedWorkProfileAppsPersistWithTheProfileSerialAndAreRestored() {
		val workUser = ActivityTestSupport.addWorkProfile()
		ActivityTestSupport.addWorkProfileApp(
			workUser, "com.example.work", "WorkChatActivity", "WorkChat")

		this.scenario.onActivity { activity ->
			val appManager = this.loadApps(activity, "apps_loader_pin_work_icons")
			val workApp = appManager.installedApps.single { it.user != null }

			appManager.repository.pin(workApp)
			appManager.repository.savePinnedApps()

			val stored = Preferences
				.getSharedPreferences(this.application, Preferences.PINNED_APPS)
				.getString("0", null)
			assertEquals("com.example.work\nWorkChatActivity\n10", stored)

			val reloaded = this.loadApps(activity, "apps_loader_pin_work_icons_reloaded")
			assertEquals(listOf(workApp), reloaded.pinned)
			assertEquals(workUser, reloaded.pinned.single().user)
		}
	}

	@Test fun aPersonalPinNeverMatchesTheWorkProfileTwin() {
		// Pre-work-profile pin format ("package\nactivity") must keep matching
		// the personal app even when the same package exists in both profiles //
		val workUser = ActivityTestSupport.addWorkProfile()
		ActivityTestSupport.addWorkProfileApp(
			workUser, "com.example.alpha", "AlphaActivity", "Alpha")
		Preferences.getSharedPreferences(this.application, Preferences.PINNED_APPS).edit()
			.putString("0", "com.example.alpha\nAlphaActivity")
			.commit()

		this.scenario.onActivity { activity ->
			val appManager = this.loadApps(activity, "apps_loader_personal_pin_icons")

			assertEquals(1, appManager.pinned.size)
			assertNull(appManager.pinned.single().user)
		}
	}
}

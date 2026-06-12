package be.robinj.distrohopper.home

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.cache.ICache
import be.robinj.distrohopper.cache.TestDrawableCache
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class AppsLoaderTest {
	private lateinit var application: Application
	private lateinit var scenario: ActivityScenario<HomeActivity>

	private class HashLabelCache : ICache<String>, MutableMap<String, String> by HashMap() {
		override fun getName() = "labels"
	}

	@Before fun setUp() {
		this.application = ApplicationProvider.getApplicationContext()
		this.scenario = ActivityTestSupport.launchHome()
		ShadowAlertDialog.reset()
	}

	@After fun tearDown() {
		this.scenario.close()
	}

	private fun addLauncherApp(packageName: String, activityName: String, label: String) {
		val resolveInfo = ActivityTestSupport.resolveInfo(packageName, activityName, label)
		Shadows.shadowOf(this.application.packageManager).addResolveInfoForIntent(
			Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
			resolveInfo,
		)
	}

	@Test fun aBrokenPackageIsSkippedInsteadOfAbortingTheLoad() {
		this.scenario.onActivity { activity ->
			val labelCache = object : ICache<String>, MutableMap<String, String> by HashMap() {
				override fun getName() = "labels"
				override fun get(key: String): String? =
					if (key.contains("com.example.alpha")) throw RuntimeException("broken app")
					else null
			}

			val appManager = runBlocking {
				AppsLoader.loadApps(activity, activity.applicationContext, labelCache,
					TestDrawableCache(activity, "apps_loader_broken_icons")) { _, _ -> }
			}

			assertTrue(appManager.findAppsByPackageName("com.example.beta").isNotEmpty())
			assertTrue(appManager.findAppsByPackageName("com.example.alpha").isEmpty())
			assertNull(ShadowAlertDialog.getLatestAlertDialog())
		}
	}

	@Test fun defaultsAppendToExistingPinsAndPersistInCategoryOrder() {
		addLauncherApp("org.mozilla.firefox", "FirefoxActivity", "Firefox")
		addLauncherApp("com.google.android.gm", "GmailActivity", "Gmail")
		addLauncherApp("com.android.camera2", "CameraActivity", "Camera")
		Preferences.getSharedPreferences(this.application, Preferences.PINNED_APPS).edit()
			.putString("0", "com.example.beta\nBetaActivity")
			.commit()
		Preferences.getSharedPreferences(this.application).edit()
			.putBoolean(Preference.DEFAULT_PINS_PENDING.getName(), true)
			.commit()

		this.scenario.onActivity { activity ->
			val appManager = runBlocking {
				AppsLoader.loadApps(
					activity,
					activity.applicationContext,
					HashLabelCache(),
					TestDrawableCache(activity, "apps_loader_default_pins_icons"),
				) { _, _ -> }
			}

			assertEquals(
				listOf("com.example.beta", "org.mozilla.firefox",
					"com.google.android.gm", "com.android.camera2"),
				appManager.pinned.map { it.packageName },
			)
		}

		val pinned = Preferences.getSharedPreferences(this.application, Preferences.PINNED_APPS)
		assertEquals("com.example.beta\nBetaActivity", pinned.getString("0", null))
		assertEquals("org.mozilla.firefox\nFirefoxActivity", pinned.getString("1", null))
		assertEquals("com.google.android.gm\nGmailActivity", pinned.getString("2", null))
		assertEquals("com.android.camera2\nCameraActivity", pinned.getString("3", null))
		assertFalse(Preferences.getSharedPreferences(this.application)
			.getBoolean(Preference.DEFAULT_PINS_PENDING.getName(), false))
	}

	@Test fun defaultAttemptIsConsumedWhenNothingMatches() {
		Preferences.getSharedPreferences(this.application).edit()
			.putBoolean(Preference.DEFAULT_PINS_PENDING.getName(), true)
			.commit()

		this.scenario.onActivity { activity ->
			runBlocking {
				AppsLoader.loadApps(
					activity,
					activity.applicationContext,
					HashLabelCache(),
					TestDrawableCache(activity, "apps_loader_no_default_pins_icons"),
				) { _, _ -> }
			}
		}

		assertFalse(Preferences.getSharedPreferences(this.application)
			.getBoolean(Preference.DEFAULT_PINS_PENDING.getName(), false))
	}
}

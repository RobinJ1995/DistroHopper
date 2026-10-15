package be.robinj.distrohopper.home

import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.App
import be.robinj.distrohopper.AppManager
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.cache.ICache
import be.robinj.distrohopper.cache.TestDrawableCache
import be.robinj.distrohopper.desktop.AppIcon
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class IconMemoryTrimmerTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() {
		this.scenario = ActivityTestSupport.launchHome()
	}

	@After fun tearDown() {
		this.scenario.close()
	}

	private class HashLabelCache : ICache<String>, MutableMap<String, String> by HashMap() {
		override fun getName() = "labels"
	}

	/** Counts reads, so a test can tell a cache re-hydration from a re-render. */
	private class CountingIconCache(private val inner: ICache<Drawable>) :
		ICache<Drawable>, MutableMap<String, Drawable> by inner {
		var reads = 0

		override fun getName() = this.inner.getName()

		override fun get(key: String): Drawable? {
			this.reads++

			return this.inner[key]
		}
	}

	/** A BitmapDrawable, so it survives a round trip through DrawableCache. */
	private fun icon(context: Context, colour: Int = Color.RED): AppIcon = AppIcon(
		BitmapDrawable(
			context.resources,
			Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).apply { eraseColor(colour) },
		),
	)

	/**
	 * A loaded launcher whose apps all hold an icon, with that icon also written
	 * through to the cache — the state the launcher is in once startup finishes.
	 */
	private fun loadedApps(
		activity: HomeActivity,
		name: String,
	): Triple<AppManager, CountingIconCache, IconMemoryTrimmer> {
		val cache = CountingIconCache(TestDrawableCache(activity, name))
		cache.clear()

		val appManager = runBlocking {
			AppsLoader.loadApps(
				activity, activity.applicationContext, HashLabelCache(), cache,
			) { _, _ -> }
		}

		for (app in appManager.installedApps) {
			app.setIcon(this.icon(activity), cache)
		}

		return Triple(appManager, cache, IconMemoryTrimmer(appManager))
	}

	private fun loadedIconCount(apps: AppManager): Int =
		apps.installedApps.count { it.isIconLoaded }

	@Test fun uiHiddenReleasesTheDashTailButSparesPinnedApps() {
		this.scenario.onActivity { activity ->
			val (apps, _, trimmer) = this.loadedApps(activity, "trim_ui_hidden")
			val pinned = apps.installedApps.first()
			apps.pin(pinned, false, false)

			val total = apps.installedApps.size
			assertEquals(total, this.loadedIconCount(apps))

			val released = trimmer.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)

			assertEquals(total - 1, released)
			assertTrue("the pinned app keeps its icon", pinned.isIconLoaded)
			assertEquals(1, this.loadedIconCount(apps))
		}
	}

	@Test fun backgroundReleasesEverythingIncludingPinnedApps() {
		this.scenario.onActivity { activity ->
			val (apps, _, trimmer) = this.loadedApps(activity, "trim_background")
			val pinned = apps.installedApps.first()
			apps.pin(pinned, false, false)

			val total = apps.installedApps.size
			val released = trimmer.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)

			assertEquals(total, released)
			assertFalse("even a pinned app gives its icon up", pinned.isIconLoaded)
			assertEquals(0, this.loadedIconCount(apps))
		}
	}

	@Test fun levelsDeeperThanBackgroundAlsoReleaseEverything() {
		this.scenario.onActivity { activity ->
			val (apps, _, trimmer) = this.loadedApps(activity, "trim_complete")
			apps.pin(apps.installedApps.first(), false, false)

			val total = apps.installedApps.size

			assertEquals(total, trimmer.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE))
		}
	}

	/**
	 * The legacy foreground levels. Android 14 stopped delivering them; where they
	 * still arrive the launcher is on screen, so there is nothing to gain.
	 */
	@Test fun foregroundRunningLevelsReleaseNothing() {
		this.scenario.onActivity { activity ->
			val (apps, _, trimmer) = this.loadedApps(activity, "trim_running")
			val total = apps.installedApps.size

			for (level in intArrayOf(
				ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
				ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
				ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
			)) {
				assertEquals("level $level must not release", 0, trimmer.onTrimMemory(level))
			}

			assertEquals(total, this.loadedIconCount(apps))
		}
	}

	@Test fun trimmingTwiceReleasesNothingTheSecondTime() {
		this.scenario.onActivity { activity ->
			val (_, _, trimmer) = this.loadedApps(activity, "trim_twice")

			assertTrue(trimmer.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) > 0)
			assertEquals(0, trimmer.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND))
		}
	}

	@Test fun aReleasedIconComesBackFromTheCacheRatherThanBeingRerendered() {
		this.scenario.onActivity { activity ->
			val (apps, cache, trimmer) = this.loadedApps(activity, "trim_reload")
			val app: App = apps.installedApps.first()

			trimmer.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
			assertFalse(app.isIconLoaded)

			val before = cache.reads
			val restored = app.icon

			assertNotNull(restored)
			assertTrue("the icon was read back out of the cache", cache.reads > before)
			assertTrue(app.isIconLoaded)
		}
	}

	/** A cache miss must still produce an icon, by falling through to a render. */
	@Test fun aReleasedIconIsRerenderedWhenTheCacheHasLostIt() {
		this.scenario.onActivity { activity ->
			val (apps, cache, trimmer) = this.loadedApps(activity, "trim_cache_miss")
			val app: App = apps.installedApps.first()

			trimmer.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
			cache.clear()

			assertNotNull(app.icon)
			assertTrue(app.isIconLoaded)
		}
	}

	@Test fun releasingAnAppThatHoldsNoIconReportsNothingReleased() {
		this.scenario.onActivity { activity ->
			val (apps, _, _) = this.loadedApps(activity, "trim_no_icon")
			val app = apps.installedApps.first()

			assertTrue(app.releaseIcon())
			assertFalse("already released", app.releaseIcon())
		}
	}
}

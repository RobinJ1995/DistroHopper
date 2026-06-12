package be.robinj.distrohopper.home

import android.widget.GridView
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.DependencyContainer
import be.robinj.distrohopper.DispatcherProvider
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.cache.ICache
import be.robinj.distrohopper.cache.TestDrawableCache
import be.robinj.distrohopper.desktop.Wallpaper
import be.robinj.distrohopper.desktop.dash.GridAdapter
import be.robinj.distrohopper.desktop.launcher.AppLauncher
import be.robinj.distrohopper.desktop.launcher.SpinnerAppLauncher
import android.view.View
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class StartupLoaderTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() {
		ShadowAlertDialog.reset()
		scenario = ActivityTestSupport.launchHome()
	}
	@After fun tearDown() { scenario.close() }

	/** A label cache whose reads blow up, to force the loader's error path. */
	private class ExplodingCache : ICache<String>, MutableMap<String, String> by HashMap() {
		override fun getName() = "exploding"
		override fun containsKey(key: String) = throw RuntimeException("cache exploded")
		override fun get(key: String): String? = throw RuntimeException("cache exploded")
		override fun put(key: String, value: String): String? =
			throw RuntimeException("cache exploded")
	}

	/**
	 * An icon cache that explodes in the icon-caching phase. Per-app failures
	 * during list loading are skipped, so the error path is exercised at the
	 * phase level.
	 */
	private class ExplodingIconCache
		: ICache<android.graphics.drawable.Drawable>,
		MutableMap<String, android.graphics.drawable.Drawable> by HashMap() {
		override fun getName() = "exploding"
		override fun containsKey(key: String) = throw RuntimeException("cache exploded")
	}

	private class HashLabelCache : ICache<String>, MutableMap<String, String> by HashMap() {
		override fun getName() = "labels"
	}

	private fun start(
		activity: HomeActivity,
		loader: StartupLoader,
		labelCache: ICache<String>,
		iconCache: ICache<android.graphics.drawable.Drawable> =
			TestDrawableCache(activity, "startup_loader_test_icons"),
	) {
		loader.start(
			activity.findViewById<Wallpaper>(R.id.wpWallpaper),
			activity.findViewById<SpinnerAppLauncher>(R.id.lalSpinner),
			activity.findViewById<AppLauncher>(R.id.lalBfb),
			activity.findViewById<GridView>(R.id.gvDashHomeApps),
			labelCache,
			iconCache,
			activity.resources.displayMetrics.density,
			24)
	}

	@Test fun startupLeavesTheHomeScreenFullyLoaded() {
		scenario.onActivity { activity ->
			assertEquals(View.GONE,
				activity.findViewById<SpinnerAppLauncher>(R.id.lalSpinner).visibility)
			assertEquals(View.VISIBLE,
				activity.findViewById<AppLauncher>(R.id.lalBfb).visibility)
			assertTrue(
				activity.findViewById<GridView>(R.id.gvDashHomeApps).adapter is GridAdapter)
			assertNotNull(activity.appManager)
		}
	}

	@Test fun cancelBeforeTheWorkRunsLoadsNothingAndShowsNoError() {
		// IO work is parked in a queue we control, so the cancel lands before
		// any loading has happened; the queue is run afterwards to let the
		// cancellation propagate through the loader's catch blocks.
		val parked = mutableListOf<Runnable>()
		val parkedDispatcher = object : kotlinx.coroutines.CoroutineDispatcher() {
			override fun dispatch(
				context: kotlin.coroutines.CoroutineContext, block: Runnable) {
				parked.add(block)
			}
		}

		scenario.onActivity { activity ->
			val gvDashHomeApps = activity.findViewById<GridView>(R.id.gvDashHomeApps)
			gvDashHomeApps.adapter = null

			val loader = StartupLoader(activity, object : DispatcherProvider {
				override val main = Dispatchers.Main
				override val io = parkedDispatcher
				override val default = parkedDispatcher
			})
			start(activity, loader, ExplodingCache())
			loader.cancel()
			parked.toList().forEach { it.run() }
		}

		ActivityTestSupport.drainTasks()

		scenario.onActivity { activity ->
			assertNull(activity.findViewById<GridView>(R.id.gvDashHomeApps).adapter)
		}
		assertNull("a cancelled startup must not show an error dialog",
			ShadowAlertDialog.getLatestAlertDialog())
	}

	@Test fun aBrokenPackageIsSkippedInsteadOfAbortingTheLoad() {
		scenario.onActivity { activity ->
			// The App constructor reads the label cache; blowing up for one
			// package simulates a single broken app (corrupt icon, vanished
			// package, ...) without aborting the rest of the load //
			val labelCache = object : ICache<String>, MutableMap<String, String> by HashMap() {
				override fun getName() = "labels"
				override fun get(key: String): String? =
					if (key.contains("com.example.alpha")) throw RuntimeException("broken app")
					else null
			}

			val appManager = kotlinx.coroutines.runBlocking {
				AppsLoader.loadApps(activity, activity.applicationContext, labelCache,
					TestDrawableCache(activity, "startup_loader_broken_icons")) { _, _ -> }
			}

			assertTrue("the healthy apps must still load",
				appManager.findAppsByPackageName("com.example.beta").isNotEmpty())
			assertTrue("the broken app must be skipped",
				appManager.findAppsByPackageName("com.example.alpha").isEmpty())
			assertNull("a single broken package must not show an error dialog",
				ShadowAlertDialog.getLatestAlertDialog())
		}
	}

	@Test fun loadingFailureShowsTheExceptionHandlerDialogAndTheActivitySurvives() {
		scenario.onActivity { activity ->
			val loader = StartupLoader(activity,
				DependencyContainer.of(activity).dispatchers)
			start(activity, loader, HashLabelCache(), ExplodingIconCache())
		}

		ActivityTestSupport.drainTasks()

		assertNotNull(ShadowAlertDialog.getLatestAlertDialog())
		scenario.onActivity { activity -> assertNotNull(activity.appManager) }
	}
}

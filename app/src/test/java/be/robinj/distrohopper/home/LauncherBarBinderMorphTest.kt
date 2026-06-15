package be.robinj.distrohopper.home

import android.widget.LinearLayout
import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.App
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.preferences.Preference
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

/**
 * Locks in the fix for the "flash of launcher icons" when swiping between
 * desktops. The per-desktop launcher morph calls requestLayout every frame,
 * changing the dock's measured size; left enabled, the container's CHANGING
 * LayoutTransition would fire-then-cancel on every frame, a cancel-restart
 * cycle that showed as a flash. The morph therefore suppresses that transition
 * for its lifetime and restores it for the single settle-time resize.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class LauncherBarBinderMorphTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() {
		scenario = ActivityTestSupport.launchHome {
			it.putString(Preference.LAUNCHER_APP_PIN_MODE.getName(), "desktop")
		}
	}

	@After fun tearDown() { scenario.close() }

	private fun container(activity: HomeActivity): LinearLayout =
		activity.findViewById(R.id.llLauncherAndDashContainer)

	private fun app(activity: HomeActivity, packageName: String): App =
		activity.appManager.findAppsByPackageName(packageName).first()

	/** Pins distinct sets on desktops 0 and 1 so the bar resizes during a morph. */
	private fun seedDistinctDesktops(activity: HomeActivity) {
		val manager = activity.appManager
		manager.setCurrentDesktop(0)
		manager.pin(app(activity, "com.example.alpha"), false, false)
		manager.setCurrentDesktop(1)
		manager.pin(app(activity, "com.example.beta"), false, false)
		manager.pin(app(activity, "com.example.gamma"), false, false)
		manager.setCurrentDesktop(0)
	}

	@Test fun containerTransitionIsSuppressedWhileMorphing() {
		scenario.onActivity { activity ->
			val binder = LauncherBarBinder(activity.appManager)
			seedDistinctDesktops(activity)
			val container = container(activity)
			assertNotNull("precondition: the container has a CHANGING transition at rest",
				container.layoutTransition)

			binder.onPageScroll(0, 1, 0.5F)

			assertNull("the CHANGING transition must be suppressed during the morph so "
				+ "its per-frame requestLayout cannot cancel-restart it (the flash)",
				container.layoutTransition)
		}
	}

	@Test fun containerTransitionIsRestoredWhenTheDesktopSettles() {
		scenario.onActivity { activity ->
			val binder = LauncherBarBinder(activity.appManager)
			seedDistinctDesktops(activity)
			val container = container(activity)
			val original = container.layoutTransition

			binder.onPageScroll(0, 1, 0.5F)
			assertNull(container.layoutTransition)

			binder.showDesktop(1)

			assertSame("the saved transition must be restored so the settle resize still animates",
				original, container.layoutTransition)
		}
	}

	@Test fun refreshPinnedViewLeavesTheContainerTransitionAloneWhenNotMorphing() {
		scenario.onActivity { activity ->
			val binder = LauncherBarBinder(activity.appManager)
			val container = container(activity)
			val original = container.layoutTransition
			assertNotNull(original)

			binder.refreshPinnedView()

			assertSame(original, container.layoutTransition)
		}
	}

	@Test fun aMorphInterruptedByAnotherMorphStillRestoresOnlyTheOriginalTransition() {
		scenario.onActivity { activity ->
			val binder = LauncherBarBinder(activity.appManager)
			seedDistinctDesktops(activity)
			val container = container(activity)
			val original = container.layoutTransition

			// Swipe 0->1, then reverse to 0->... before settling: the saved
			// transition must not be lost to null across the re-built morph //
			binder.onPageScroll(0, 1, 0.4F)
			assertNull(container.layoutTransition)
			binder.onPageScroll(1, 0, 0.6F)
			assertNull(container.layoutTransition)

			binder.showDesktop(0)

			assertSame(original, container.layoutTransition)
		}
	}
}

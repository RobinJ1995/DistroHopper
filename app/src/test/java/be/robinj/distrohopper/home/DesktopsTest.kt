package be.robinj.distrohopper.home

import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.HomeActivity
import be.robinj.distrohopper.R
import be.robinj.distrohopper.widgets.WidgetContainer
import be.robinj.distrohopper.widgets.WidgetTestSupport
import be.robinj.distrohopper.widgets.WidgetsPager
import be.robinj.distrohopper.widgets.DesktopAppView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

/**
 * Deleting a desktop must remove its widgets *and* its per-desktop pins and
 * reindex the higher desktops, keeping the two in lockstep.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class DesktopsTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() { this.scenario = ActivityTestSupport.launchHome() } // per-desktop default //

	@After fun tearDown() { this.scenario.close() }

	private fun widgetIdsOn(pager: WidgetsPager, page: Int): List<Int> {
		val container = pager.pageAt(page)
		return (0 until container.childCount)
			.mapNotNull { container.getChildAt(it) as? WidgetContainer }
			.map { it.appWidgetId }
	}

	private fun desktopAppKeysOn(pager: WidgetsPager, page: Int): List<String> {
		val container = pager.pageAt(page)
		return (0 until container.childCount)
			.mapNotNull { container.getChildAt(it) as? DesktopAppView }
			.map { it.key }
	}

	@Test fun highestOccupiedDesktopCombinesWidgetsAndPins() {
		this.scenario.onActivity { activity ->
			val pager = activity.findViewById<WidgetsPager>(R.id.vgWidgets)
			val host = WidgetTestSupport.host(activity)
			val appManager = requireNotNull(activity.appManager)
			val desktops = Desktops(host, appManager, requireNotNull(activity.desktopAppHost), requireNotNull(activity.desktopFolderHost))

			// A widget on desktop 1, a pin on desktop 3 //
			WidgetTestSupport.addWidget(activity, host, pager.pageAt(1), 11, 0, 0, 2, 2)
			appManager.repository.pin(
				appManager.findAppsByPackageName("com.example.alpha").first(), 3)

			assertEquals(3, desktops.highestOccupiedDesktop())
		}
	}

	@Test fun highestOccupiedDesktopCountsDesktopApps() {
		this.scenario.onActivity { activity ->
			val pager = activity.findViewById<WidgetsPager>(R.id.vgWidgets)
			val host = WidgetTestSupport.host(activity)
			val appManager = requireNotNull(activity.appManager)
			val desktopAppHost = requireNotNull(activity.desktopAppHost)
			val desktops = Desktops(host, appManager, desktopAppHost, requireNotNull(activity.desktopFolderHost))

			// A widget on desktop 0 (so no leading desktop is auto-removed) and a
			// desktop app on desktop 1: the desktop app must lift the highest to 1 //
			WidgetTestSupport.addWidget(activity, host, pager.pageAt(0), 10, 0, 0, 2, 2)
			desktopAppHost.pinAt(WidgetTestSupport.app(activity, "com.example.alpha"), 0, 0, 1)

			assertEquals(1, desktops.highestOccupiedDesktop())
		}
	}

	@Test fun deleteDesktopRemovesWidgetsAndPinsThenReindexes() {
		this.scenario.onActivity { activity ->
			val pager = activity.findViewById<WidgetsPager>(R.id.vgWidgets)
			val host = WidgetTestSupport.host(activity)
			val appManager = requireNotNull(activity.appManager)
			val repo = appManager.repository
			val appA = appManager.findAppsByPackageName("com.example.alpha").first()
			val appB = appManager.findAppsByPackageName("com.example.beta").first()

			// Desktop 0: appA + widget 11; desktop 1: appB + widget 22 (no leading
			// empty desktop, so the auto-removal leaves this setup untouched) //
			repo.pin(appA, 0)
			repo.pin(appB, 1)
			WidgetTestSupport.addWidget(activity, host, pager.pageAt(0), 11, 0, 0, 2, 2)
			WidgetTestSupport.addWidget(activity, host, pager.pageAt(1), 22, 0, 0, 2, 2)
			host.persist()
			pager.pagesChanged()

			Desktops(host, appManager, requireNotNull(activity.desktopAppHost), requireNotNull(activity.desktopFolderHost)).deleteDesktop(0)
			ActivityTestSupport.drainTasks()

			// Desktop 0's own contents are gone //
			assertFalse(repo.isPinnedOn(appA, 0))
			// Desktop 1 shifted down to desktop 0, both pins and widgets //
			assertTrue(repo.isPinnedOn(appB, 0))
			assertEquals(0, repo.highestPinnedDesktop())
			assertEquals(listOf(22), this.widgetIdsOn(pager, 0))
		}
	}

	@Test fun deleteDesktopRemovesDesktopAppsAndShiftsHigherOnesDown() {
		this.scenario.onActivity { activity ->
			val pager = activity.findViewById<WidgetsPager>(R.id.vgWidgets)
			val host = WidgetTestSupport.host(activity)
			val appManager = requireNotNull(activity.appManager)
			val desktopAppHost = requireNotNull(activity.desktopAppHost)
			val appA = WidgetTestSupport.app(activity, "com.example.alpha")
			val appB = WidgetTestSupport.app(activity, "com.example.beta")

			// Desktop 0: appA; desktop 1: appB (packed from desktop 0 so the
			// auto-removal leaves them in place) //
			desktopAppHost.pinAt(appA, 0, 0, 0)
			desktopAppHost.pinAt(appB, 0, 0, 1)

			Desktops(host, appManager, desktopAppHost, requireNotNull(activity.desktopFolderHost)).deleteDesktop(0)
			ActivityTestSupport.drainTasks()

			// appA (desktop 0) gone; appB shifted down from desktop 1 to desktop 0 //
			assertFalse(desktopAppHost.isPinnedOnDesktop(appA))
			assertTrue(desktopAppHost.isPinnedOnDesktop(appB))
			assertEquals(listOf(appB.profileScopedKey), this.desktopAppKeysOn(pager, 0))
		}
	}

	@Test fun deletingTheLastItemRemovesTheEmptyDesktopAndShiftsHigherDown() {
		this.scenario.onActivity { activity ->
			val pager = activity.findViewById<WidgetsPager>(R.id.vgWidgets)
			val desktopAppHost = requireNotNull(activity.desktopAppHost)
			val appA = WidgetTestSupport.app(activity, "com.example.alpha")
			val appB = WidgetTestSupport.app(activity, "com.example.beta")

			// Desktop 0: appA; desktop 1: appB (the only item on each) //
			desktopAppHost.pinAt(appA, 0, 0, 0)
			desktopAppHost.pinAt(appB, 0, 0, 1)

			// Deleting desktop 0's only item empties it: it is auto-removed and
			// desktop 1 shifts down into its place //
			desktopAppHost.remove(requireNotNull(desktopAppHost.viewForKey(appA.profileScopedKey)))
			ActivityTestSupport.drainTasks()

			assertFalse(desktopAppHost.isPinnedOnDesktop(appA))
			assertEquals(listOf(appB.profileScopedKey), this.desktopAppKeysOn(pager, 0))
			assertTrue(this.desktopAppKeysOn(pager, 1).isEmpty())
		}
	}

	@Test fun aTrailingEmptyDesktopAlwaysRemains() {
		this.scenario.onActivity { activity ->
			val pager = activity.findViewById<WidgetsPager>(R.id.vgWidgets)
			val desktopAppHost = requireNotNull(activity.desktopAppHost)
			val appA = WidgetTestSupport.app(activity, "com.example.alpha")

			desktopAppHost.pinAt(appA, 0, 0, 0)

			// One occupied desktop plus the trailing empty one for adding to //
			assertEquals(2, pager.pageCount)

			// Emptying the only occupied desktop collapses to a single empty one //
			desktopAppHost.remove(requireNotNull(desktopAppHost.viewForKey(appA.profileScopedKey)))
			ActivityTestSupport.drainTasks()

			assertEquals(1, pager.pageCount)
		}
	}
}

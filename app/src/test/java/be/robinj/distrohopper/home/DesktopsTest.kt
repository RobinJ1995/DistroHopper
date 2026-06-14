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
			val host = WidgetTestSupport.host(activity)
			val appManager = requireNotNull(activity.appManager)
			val desktopAppHost = requireNotNull(activity.desktopAppHost)
			val desktops = Desktops(host, appManager, desktopAppHost, requireNotNull(activity.desktopFolderHost))

			// Only a desktop app, on desktop 4 //
			desktopAppHost.pinAt(WidgetTestSupport.app(activity, "com.example.alpha"), 0, 0, 4)

			assertEquals(4, desktops.highestOccupiedDesktop())
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

			// Desktop 1: appA + widget 11; desktop 2: appB + widget 22 //
			repo.pin(appA, 1)
			repo.pin(appB, 2)
			WidgetTestSupport.addWidget(activity, host, pager.pageAt(1), 11, 0, 0, 2, 2)
			WidgetTestSupport.addWidget(activity, host, pager.pageAt(2), 22, 0, 0, 2, 2)
			host.persist()
			pager.pagesChanged()

			Desktops(host, appManager, requireNotNull(activity.desktopAppHost), requireNotNull(activity.desktopFolderHost)).deleteDesktop(1)
			ActivityTestSupport.drainTasks()

			// Desktop 1's own contents are gone //
			assertFalse(repo.isPinnedOn(appA, 1))
			// Desktop 2 shifted down to desktop 1, both pins and widgets //
			assertTrue(repo.isPinnedOn(appB, 1))
			assertEquals(1, repo.highestPinnedDesktop())
			assertEquals(listOf(22), this.widgetIdsOn(pager, 1))
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

			// Desktop 1: appA; desktop 2: appB //
			desktopAppHost.pinAt(appA, 0, 0, 1)
			desktopAppHost.pinAt(appB, 0, 0, 2)

			Desktops(host, appManager, desktopAppHost, requireNotNull(activity.desktopFolderHost)).deleteDesktop(1)
			ActivityTestSupport.drainTasks()

			// appA (desktop 1) gone; appB shifted down from desktop 2 to desktop 1 //
			assertFalse(desktopAppHost.isPinnedOnDesktop(appA))
			assertTrue(desktopAppHost.isPinnedOnDesktop(appB))
			assertEquals(listOf(appB.profileScopedKey), this.desktopAppKeysOn(pager, 1))
		}
	}
}

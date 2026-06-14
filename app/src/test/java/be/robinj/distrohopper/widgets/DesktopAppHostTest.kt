package be.robinj.distrohopper.widgets

import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.HomeActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

/**
 * The desktop-app host: restore (prune/de-dup/collision-repair), the
 * cross-desktop single-copy invariant, moving, removal and page shifting.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class DesktopAppHostTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() { this.scenario = ActivityTestSupport.launchHome() }
	@After fun tearDown() { this.scenario.close() }

	private fun writeStored(activity: HomeActivity, vararg layouts: DesktopAppLayout) {
		DesktopLayoutTestStore.saveApps(activity.applicationContext, layouts.toList())
	}

	private fun keysOf(grid: WidgetsContainer): List<String> =
		WidgetTestSupport.desktopAppsOn(grid).map { it.key }

	@Test fun restorePrunesAppsThatAreNoLongerInstalled() {
		this.scenario.onActivity { activity ->
			val grid = WidgetTestSupport.standaloneGrid(activity)
			val host = WidgetTestSupport.desktopHost(activity, grid)
			val alpha = WidgetTestSupport.app(activity, "com.example.alpha")
			this.writeStored(activity,
				DesktopAppLayout(alpha.profileScopedKey, 0, 0, 0),
				DesktopAppLayout("ie.craggy.noel\nNoelActivity", 1, 0, 0))

			host.restore()

			assertEquals(listOf(alpha.profileScopedKey), this.keysOf(grid))
			// The pruned entry is dropped from persistence too //
			assertEquals(1, DesktopLayoutTestStore.apps(activity.applicationContext).size)
		}
	}

	@Test fun restoreDeDupsTheSameAppAcrossPagesKeepingTheFirst() {
		this.scenario.onActivity { activity ->
			val grid = WidgetTestSupport.standaloneGrid(activity)
			val pager = WidgetTestSupport.pagerOf(grid)
			val host = WidgetTestSupport.desktopHost(activity, grid)
			val alpha = WidgetTestSupport.app(activity, "com.example.alpha")
			this.writeStored(activity,
				DesktopAppLayout(alpha.profileScopedKey, 0, 0, 0),
				DesktopAppLayout(alpha.profileScopedKey, 1, 1, 1))

			host.restore()

			// Only the first placement survives, on page 0 //
			assertEquals(listOf(alpha.profileScopedKey), this.keysOf(pager.pageAt(0)))
			assertEquals(emptyList<String>(), this.keysOf(pager.pageAt(1)))
			assertEquals(1, DesktopLayoutTestStore.apps(activity.applicationContext).size)
		}
	}

	@Test fun restoreRepacksAnAppWhoseCellCollidesWithAWidget() {
		this.scenario.onActivity { activity ->
			val grid = WidgetTestSupport.standaloneGrid(activity)
			val widgetHost = WidgetTestSupport.host(activity, grid)
			// A 2x2 widget occupying cols 0-1, rows 0-1 //
			WidgetTestSupport.addWidget(activity, widgetHost, grid, 42, 0, 0, 2, 2)

			val host = WidgetTestSupport.desktopHost(activity, grid)
			val alpha = WidgetTestSupport.app(activity, "com.example.alpha")
			this.writeStored(activity, DesktopAppLayout(alpha.profileScopedKey, 0, 0, 0))

			host.restore()

			val view = WidgetTestSupport.desktopAppsOn(grid).single()
			val lp = view.layoutParams as WidgetsContainer.LayoutParams
			// First free cell row-major past the 2x2 widget is (col 2, row 0) //
			assertEquals(2, lp.col)
			assertEquals(0, lp.row)
		}
	}

	@Test fun pinAtEnforcesCrossDesktopSingleCopy() {
		this.scenario.onActivity { activity ->
			val grid = WidgetTestSupport.standaloneGrid(activity)
			val pager = WidgetTestSupport.pagerOf(grid)
			val host = WidgetTestSupport.desktopHost(activity, grid)
			val alpha = WidgetTestSupport.app(activity, "com.example.alpha")

			host.pinAt(alpha, 0, 0, 0)
			host.pinAt(alpha, 3, 4, 1) // Same app, different desktop //

			// Exactly one pin, relocated to desktop 1 //
			assertEquals(emptyList<String>(), this.keysOf(pager.pageAt(0)))
			assertEquals(listOf(alpha.profileScopedKey), this.keysOf(pager.pageAt(1)))
			assertEquals(1, DesktopLayoutTestStore.apps(activity.applicationContext).size)
		}
	}

	@Test fun pinAtKeepsSamePackageInDifferentProfilesDistinct() {
		ActivityTestSupport.addWorkProfile()
		val workUser = ActivityTestSupport.workProfileHandle()
		ActivityTestSupport.addWorkProfileApp(
			workUser, "com.example.alpha", "AlphaActivity", "Alpha")

		ActivityTestSupport.launchHome().use { scenario ->
			scenario.onActivity { activity ->
				val grid = WidgetTestSupport.standaloneGrid(activity)
				val host = WidgetTestSupport.desktopHost(activity, grid)
				val matches = activity.appManager.findAppsByPackageName("com.example.alpha")
				val personal = matches.first { it.user == null }
				val work = matches.first { it.user != null }

				host.pinAt(personal, 0, 0, 0)
				host.pinAt(work, 1, 0, 0)

				// Distinct profile-scoped keys → both pins coexist //
				assertTrue(host.isPinnedOnDesktop(personal))
				assertTrue(host.isPinnedOnDesktop(work))
				assertEquals(2, WidgetTestSupport.desktopAppsOn(grid).size)
			}
		}
	}

	@Test fun pinAtRelocatesWhenTheRequestedCellIsTakenByAnotherApp() {
		this.scenario.onActivity { activity ->
			val grid = WidgetTestSupport.standaloneGrid(activity)
			val host = WidgetTestSupport.desktopHost(activity, grid)
			val alpha = WidgetTestSupport.app(activity, "com.example.alpha")
			val beta = WidgetTestSupport.app(activity, "com.example.beta")

			host.pinAt(alpha, 0, 0, 0)
			host.pinAt(beta, 0, 0, 0) // Same cell — must fall back to a free one //

			val betaView = WidgetTestSupport.desktopAppsOn(grid).first { it.key == beta.profileScopedKey }
			val lp = betaView.layoutParams as WidgetsContainer.LayoutParams
			assertFalse(lp.col == 0 && lp.row == 0)
			assertEquals(2, WidgetTestSupport.desktopAppsOn(grid).size)
		}
	}

	@Test fun aPinnedDesktopAppOccupiesA2x2Block() {
		this.scenario.onActivity { activity ->
			val grid = WidgetTestSupport.standaloneGrid(activity)
			val host = WidgetTestSupport.desktopHost(activity, grid)

			host.pinAt(WidgetTestSupport.app(activity, "com.example.alpha"), 1, 1, 0)

			val lp = WidgetTestSupport.desktopAppsOn(grid).single()
				.layoutParams as WidgetsContainer.LayoutParams
			assertEquals(DesktopAppLayout.SPAN, lp.colSpan)
			assertEquals(DesktopAppLayout.SPAN, lp.rowSpan)
			assertEquals(2, lp.colSpan) // The chosen footprint, guarding against a 1x1 regression //
		}
	}

	@Test fun twoDesktopAppsCannotOverlapTheir2x2Blocks() {
		this.scenario.onActivity { activity ->
			val grid = WidgetTestSupport.standaloneGrid(activity)
			val host = WidgetTestSupport.desktopHost(activity, grid)
			val alpha = WidgetTestSupport.app(activity, "com.example.alpha")
			val beta = WidgetTestSupport.app(activity, "com.example.beta")

			host.pinAt(alpha, 0, 0, 0) // Occupies cols 0-1, rows 0-1 //
			host.pinAt(beta, 1, 1, 0)  // Would overlap a 2x2 alpha (a 1x1 would not) //

			val betaLp = WidgetTestSupport.desktopAppsOn(grid)
				.first { it.key == beta.profileScopedKey }
				.layoutParams as WidgetsContainer.LayoutParams
			// Relocated clear of alpha's block //
			assertFalse(betaLp.col == 1 && betaLp.row == 1)
			assertEquals(2, WidgetTestSupport.desktopAppsOn(grid).size)
		}
	}

	@Test fun moveToRepositionsWithinTheDesktop() {
		this.scenario.onActivity { activity ->
			val grid = WidgetTestSupport.standaloneGrid(activity)
			val host = WidgetTestSupport.desktopHost(activity, grid)
			val alpha = WidgetTestSupport.app(activity, "com.example.alpha")
			host.pinAt(alpha, 0, 0, 0)
			val view = WidgetTestSupport.desktopAppsOn(grid).single()

			host.moveTo(view, 5, 6)

			val lp = view.layoutParams as WidgetsContainer.LayoutParams
			assertEquals(5, lp.col)
			assertEquals(6, lp.row)
			val persisted = DesktopLayoutTestStore.apps(activity.applicationContext).single()
			assertEquals(5, persisted.col)
			assertEquals(6, persisted.row)
		}
	}

	@Test fun moveToOntoAWidgetCellIsRejected() {
		this.scenario.onActivity { activity ->
			val grid = WidgetTestSupport.standaloneGrid(activity)
			val widgetHost = WidgetTestSupport.host(activity, grid)
			WidgetTestSupport.addWidget(activity, widgetHost, grid, 42, 4, 4, 2, 2)
			val host = WidgetTestSupport.desktopHost(activity, grid)
			val alpha = WidgetTestSupport.app(activity, "com.example.alpha")
			host.pinAt(alpha, 0, 0, 0)
			val view = WidgetTestSupport.desktopAppsOn(grid).single()

			host.moveTo(view, 4, 4) // Onto the widget //

			val lp = view.layoutParams as WidgetsContainer.LayoutParams
			assertEquals(0, lp.col) // Unchanged //
			assertEquals(0, lp.row)
		}
	}

	@Test fun removeTakesItOffTheDesktopAndOutOfPersistence() {
		this.scenario.onActivity { activity ->
			val grid = WidgetTestSupport.standaloneGrid(activity)
			val host = WidgetTestSupport.desktopHost(activity, grid)
			val alpha = WidgetTestSupport.app(activity, "com.example.alpha")
			host.pinAt(alpha, 0, 0, 0)
			val view = WidgetTestSupport.desktopAppsOn(grid).single()

			host.remove(view)

			assertEquals(0, WidgetTestSupport.desktopAppsOn(grid).size)
			assertFalse(host.isPinnedOnDesktop(alpha))
			assertTrue(DesktopLayoutTestStore.apps(activity.applicationContext).isEmpty())
		}
	}

	@Test fun unpinFromAllDesktopsRemovesTheAppOnUninstall() {
		this.scenario.onActivity { activity ->
			val grid = WidgetTestSupport.standaloneGrid(activity)
			val host = WidgetTestSupport.desktopHost(activity, grid)
			val alpha = WidgetTestSupport.app(activity, "com.example.alpha")
			host.pinAt(alpha, 2, 2, 1)

			host.unpinFromAllDesktops(alpha)

			assertFalse(host.isPinnedOnDesktop(alpha))
			assertNull(host.viewForKey(alpha.profileScopedKey))
		}
	}

	@Test fun highestDesktopAndRemoveDesktopPageShiftHigherPagesDown() {
		this.scenario.onActivity { activity ->
			val grid = WidgetTestSupport.standaloneGrid(activity)
			val pager = WidgetTestSupport.pagerOf(grid)
			val host = WidgetTestSupport.desktopHost(activity, grid)
			val alpha = WidgetTestSupport.app(activity, "com.example.alpha")
			val beta = WidgetTestSupport.app(activity, "com.example.beta")
			host.pinAt(alpha, 0, 0, 1)
			host.pinAt(beta, 0, 0, 3)

			assertEquals(3, host.highestDesktop())

			host.removeDesktopPage(1)

			// beta shifts from desktop 3 down to desktop 2 //
			assertEquals(2, host.highestDesktop())
			assertEquals(listOf(beta.profileScopedKey), this.keysOf(pager.pageAt(2)))
			assertFalse(host.isPinnedOnDesktop(alpha))
		}
	}
}

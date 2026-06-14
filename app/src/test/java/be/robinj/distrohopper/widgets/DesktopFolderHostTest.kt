package be.robinj.distrohopper.widgets

import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.HomeActivity
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

/**
 * The desktop-folder host: create from two apps, add apps/widgets (with the 3x3
 * fit limit), delete (members removed), runtime uninstall, and restore reconcile.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class DesktopFolderHostTest {
	private lateinit var scenario: ActivityScenario<HomeActivity>

	@Before fun setUp() { this.scenario = ActivityTestSupport.launchHome() }
	@After fun tearDown() { this.scenario.close() }

	private class Fixture(
		val grid: WidgetsContainer,
		val appHost: DesktopAppHost,
		val widgetHost: WidgetHost,
		val folderHost: DesktopFolderHost,
	)

	private fun fixture(activity: HomeActivity): Fixture {
		val grid = WidgetTestSupport.standaloneGrid(activity)
		val appHost = WidgetTestSupport.desktopHost(activity, grid)
		val widgetHost = WidgetTestSupport.host(activity, grid)
		val folderHost = WidgetTestSupport.desktopFolderHost(activity, grid, appHost, widgetHost)
		return Fixture(grid, appHost, widgetHost, folderHost)
	}

	/** Pins an app to the desktop and returns its view. */
	private fun pin(activity: HomeActivity, f: Fixture, pkg: String, col: Int, row: Int): DesktopAppView {
		val app = WidgetTestSupport.app(activity, pkg)
		f.appHost.pinAt(app, col, row, 0)
		return WidgetTestSupport.desktopAppsOn(f.grid).first { it.key == app.profileScopedKey }
	}

	private fun folder(f: Fixture) = WidgetTestSupport.foldersOn(f.grid).single()

	@Test fun createFolderFromTwoAppsReplacesThemWithAFolder() {
		this.scenario.onActivity { activity ->
			val f = this.fixture(activity)
			val a = this.pin(activity, f, "com.example.alpha", 0, 0)
			val b = this.pin(activity, f, "com.example.beta", 2, 0)

			f.folderHost.createFolder(a, b)

			assertEquals(0, WidgetTestSupport.desktopAppsOn(f.grid).size)
			val folder = this.folder(f)
			assertEquals(2, folder.layout.appCount)
		}
	}

	@Test fun addWidgetThatFitsMovesItIntoTheFolderAndOffTheGrid() {
		this.scenario.onActivity { activity ->
			val f = this.fixture(activity)
			val a = this.pin(activity, f, "com.example.alpha", 0, 0)
			val b = this.pin(activity, f, "com.example.beta", 2, 0)
			f.folderHost.createFolder(a, b)
			val id = this.folder(f).folderId

			val widget = WidgetTestSupport.addWidget(activity, f.widgetHost, f.grid, 42, 5, 5, 1, 1)
			f.folderHost.addWidget(id, widget)

			assertEquals(listOf(42), this.folder(f).layout.widgetIds)
			assertNotNull(f.folderHost.retainedWidget(42))
			// The widget left the grid (it now lives off-grid in the folder) //
			assertNull(f.grid.findViewAtCell(5, 5))
		}
	}

	@Test fun aWidgetTooBigToFitIsRejected() {
		this.scenario.onActivity { activity ->
			val f = this.fixture(activity)
			val a = this.pin(activity, f, "com.example.alpha", 0, 0)
			val b = this.pin(activity, f, "com.example.beta", 2, 0)
			f.folderHost.createFolder(a, b)
			val id = this.folder(f).folderId

			// 2 app cells + a 3x3 widget cannot fit in the 3x3 folder grid.
			val widget = WidgetTestSupport.addWidget(activity, f.widgetHost, f.grid, 42, 4, 4, 3, 3)
			f.folderHost.addWidget(id, widget)

			assertTrue(this.folder(f).layout.widgetIds.isEmpty())
			assertNull(f.folderHost.retainedWidget(42))
		}
	}

	@Test fun deleteFolderRemovesItAndItsMembers() {
		this.scenario.onActivity { activity ->
			val f = this.fixture(activity)
			val a = this.pin(activity, f, "com.example.alpha", 0, 0)
			val b = this.pin(activity, f, "com.example.beta", 2, 0)
			f.folderHost.createFolder(a, b)

			f.folderHost.deleteFolder(this.folder(f).folderId)

			assertEquals(0, WidgetTestSupport.foldersOn(f.grid).size)
			// Members do not return to the desktop (deleted with the folder) //
			assertEquals(0, WidgetTestSupport.desktopAppsOn(f.grid).size)
		}
	}

	@Test fun restoreDissolvesAFolderWhoseMemberIsUninstalled() {
		this.scenario.onActivity { activity ->
			val f = this.fixture(activity)
			val alpha = WidgetTestSupport.app(activity, "com.example.alpha")
			DesktopFolderPersistence(activity.applicationContext).save(listOf(
				DesktopFolderLayout("folder-1", 0, 0, 0)
					.withApp(alpha.profileScopedKey)!!
					.withApp("com.example.gone\nGone")!!))

			f.folderHost.restore()

			// One app missing → folder dissolves, the surviving app returns loose //
			assertEquals(0, WidgetTestSupport.foldersOn(f.grid).size)
			assertEquals(listOf(alpha.profileScopedKey),
				WidgetTestSupport.desktopAppsOn(f.grid).map { it.key })
		}
	}

	@Test fun unpinFromAllDesktopsDropsTheAppAndDissolvesAtTwo() {
		this.scenario.onActivity { activity ->
			val f = this.fixture(activity)
			val a = this.pin(activity, f, "com.example.alpha", 0, 0)
			val b = this.pin(activity, f, "com.example.beta", 2, 0)
			f.folderHost.createFolder(a, b)

			f.folderHost.unpinFromAllDesktops(WidgetTestSupport.app(activity, "com.example.alpha"))

			// Down to one member → folder dissolves; beta returns loose //
			assertEquals(0, WidgetTestSupport.foldersOn(f.grid).size)
			val beta = WidgetTestSupport.app(activity, "com.example.beta")
			assertEquals(listOf(beta.profileScopedKey),
				WidgetTestSupport.desktopAppsOn(f.grid).map { it.key })
		}
	}
}

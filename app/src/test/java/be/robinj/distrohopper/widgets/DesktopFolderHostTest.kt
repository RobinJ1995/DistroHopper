package be.robinj.distrohopper.widgets

import androidx.test.core.app.ActivityScenario
import be.robinj.distrohopper.ActivityTestSupport
import be.robinj.distrohopper.HomeActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

	@Test fun removeMemberExtractsAnAppLooseAndDissolvesATwoMemberFolder() {
		this.scenario.onActivity { activity ->
			val f = this.fixture(activity)
			val a = this.pin(activity, f, "com.example.alpha", 0, 0)
			val b = this.pin(activity, f, "com.example.beta", 2, 0)
			f.folderHost.createFolder(a, b)
			val id = this.folder(f).folderId
			val alpha = WidgetTestSupport.app(activity, "com.example.alpha")

			f.folderHost.removeMember(id, be.robinj.distrohopper.folder.FolderMember.AppMember(
				alpha.profileScopedKey), 4, 4, 0)

			// Folder dropped to one member → dissolves; both apps are loose again.
			assertEquals(0, WidgetTestSupport.foldersOn(f.grid).size)
			assertEquals(2, WidgetTestSupport.desktopAppsOn(f.grid).size)
		}
	}

	@Test fun deleteMemberDropsItWithoutReturningItLoose() {
		this.scenario.onActivity { activity ->
			val f = this.fixture(activity)
			val a = this.pin(activity, f, "com.example.alpha", 0, 0)
			val b = this.pin(activity, f, "com.example.beta", 2, 0)
			val c = this.pin(activity, f, "com.example.gamma", 4, 0)
			f.folderHost.createFolder(a, b)
			val id = this.folder(f).folderId
			f.folderHost.addApp(id, c) // folder now has 3 apps

			val gamma = WidgetTestSupport.app(activity, "com.example.gamma")
			f.folderHost.deleteMember(id, be.robinj.distrohopper.folder.FolderMember.AppMember(
				gamma.profileScopedKey))

			// Folder survives with 2 apps; the deleted member is not on the desktop.
			assertEquals(1, WidgetTestSupport.foldersOn(f.grid).size)
			assertEquals(2, this.folder(f).layout.appCount)
			assertEquals(0, WidgetTestSupport.desktopAppsOn(f.grid).size)
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

	@Test fun createFolderAnchorsAtTheTargetAppsCellNotTheDraggedOne() {
		this.scenario.onActivity { activity ->
			val f = this.fixture(activity)
			val dragged = this.pin(activity, f, "com.example.alpha", 0, 0)
			val target = this.pin(activity, f, "com.example.beta", 4, 2)

			f.folderHost.createFolder(dragged, target)

			val lp = this.folder(f).layoutParams as WidgetsContainer.LayoutParams
			assertEquals("the folder takes the dropped-onto app's cell", 4, lp.col)
			assertEquals(2, lp.row)
		}
	}

	@Test fun removeMemberPlacesTheExtractedAppAtTheDropCell() {
		this.scenario.onActivity { activity ->
			val f = this.fixture(activity)
			// Three apps so the folder survives the extraction (no dissolve), making
			// it unambiguous which app must land at the drop cell.
			val a = this.pin(activity, f, "com.example.alpha", 0, 0)
			val b = this.pin(activity, f, "com.example.beta", 2, 0)
			val c = this.pin(activity, f, "com.example.gamma", 4, 0)
			f.folderHost.createFolder(a, b)
			val id = this.folder(f).folderId
			f.folderHost.addApp(id, c)
			val alpha = WidgetTestSupport.app(activity, "com.example.alpha")

			f.folderHost.removeMember(id, be.robinj.distrohopper.folder.FolderMember.AppMember(
				alpha.profileScopedKey), 5, 5, 0)

			// Exactly the extracted app is loose, at the drop cell (not a remaining one).
			val loose = WidgetTestSupport.desktopAppsOn(f.grid)
			assertEquals(1, loose.size)
			assertEquals(alpha.profileScopedKey, loose.first().key)
			val lp = loose.first().layoutParams as WidgetsContainer.LayoutParams
			assertEquals(5, lp.col)
			assertEquals(5, lp.row)
		}
	}

	@Test fun restoreDropsAFolderMemberThatIsAlsoLooseOnTheDesktop() {
		this.scenario.onActivity { activity ->
			val f = this.fixture(activity)
			val alpha = WidgetTestSupport.app(activity, "com.example.alpha")
			val beta = WidgetTestSupport.app(activity, "com.example.beta")
			// alpha is BOTH loose on the desktop and persisted inside a folder.
			f.appHost.pinAt(alpha, 0, 0, 0)
			DesktopFolderPersistence(activity.applicationContext).save(listOf(
				DesktopFolderLayout("folder-1", 3, 3, 0)
					.withApp(alpha.profileScopedKey)!!
					.withApp(beta.profileScopedKey)!!))

			f.folderHost.restore()

			// loose wins: alpha stays loose and is dropped from the folder, which then
			// has one member and dissolves (beta returns loose). Never both places.
			val looseKeys = WidgetTestSupport.desktopAppsOn(f.grid).map { it.key }
			assertTrue(looseKeys.contains(alpha.profileScopedKey))
			assertEquals(0, WidgetTestSupport.foldersOn(f.grid).size)
		}
	}

	@Test fun dropFromFoldersRemovesTheAppWithoutTouchingTheOthers() {
		this.scenario.onActivity { activity ->
			val f = this.fixture(activity)
			val a = this.pin(activity, f, "com.example.alpha", 0, 0)
			val b = this.pin(activity, f, "com.example.beta", 2, 0)
			val c = this.pin(activity, f, "com.example.gamma", 4, 0)
			f.folderHost.createFolder(a, b)
			val id = this.folder(f).folderId
			f.folderHost.addApp(id, c) // folder now holds 3 apps
			val alpha = WidgetTestSupport.app(activity, "com.example.alpha")

			f.folderHost.dropFromFolders(alpha)

			// alpha leaves the folder (it has just been placed loose elsewhere); the
			// folder survives with its other two members.
			assertEquals(1, WidgetTestSupport.foldersOn(f.grid).size)
			assertFalse(this.folder(f).layout.appKeys.contains(alpha.profileScopedKey))
		}
	}
}

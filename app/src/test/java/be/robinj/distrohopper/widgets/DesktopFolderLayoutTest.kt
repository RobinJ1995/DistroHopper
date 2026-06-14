package be.robinj.distrohopper.widgets

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.folder.FolderMember
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The desktop folder's 3x3 mixed packing (apps as 1x1, widgets at their span)
 * and its JSON persistence round-trip.
 */
@RunWith(RobolectricTestRunner::class)
class DesktopFolderLayoutTest {
	private val context: Context = ApplicationProvider.getApplicationContext()

	private fun folder(vararg appKeys: String): DesktopFolderLayout {
		var layout = DesktopFolderLayout("folder-1", 0, 0, 0)
		for (key in appKeys) {
			layout = layout.withApp(key) ?: error("no room for $key")
		}
		return layout
	}

	@Test fun appsPackOneByOneAndAFolderHoldsAtMostNine() {
		var layout = DesktopFolderLayout("folder-1", 2, 3, 1)
		for (i in 0 until 9) {
			layout = layout.withApp("pkg$i\nA") ?: error("app $i should fit")
		}
		assertEquals(9, layout.appCount)
		assertNull("a tenth app must not fit", layout.withApp("pkg9\nA"))
	}

	@Test fun aWidgetFitsAlongsideApps_ifThereIsRoom() {
		// One app at (0,0) leaves room for up to a 2x3 / 3x2 widget, not 3x3.
		val oneApp = this.folder("a\nA")
		assertNotNull(oneApp.withWidget(10, 2, 3))
		assertNotNull(oneApp.withWidget(10, 3, 2))
		assertNull(oneApp.withWidget(10, 3, 3))
	}

	@Test fun fourAppsLeaveRoomForAtMostAReducedWidget() {
		// Four apps pack to (0,0),(1,0),(2,0),(0,1) — a 2x2 hole remains, a 3x2 does not.
		val fourApps = this.folder("a\nA", "b\nB", "c\nC", "d\nD")
		assertNotNull(fourApps.withWidget(10, 2, 2))
		assertNull(fourApps.withWidget(10, 3, 2))
	}

	@Test fun withoutRemovesAMember() {
		val layout = this.folder("a\nA", "b\nB")
			.withWidget(10, 1, 1)!!
		assertEquals(listOf("a\nA", "b\nB"), layout.appKeys)
		assertEquals(listOf(10), layout.widgetIds)

		val pruned = layout.without(FolderMember.WidgetMember(10))
		assertEquals(emptyList<Int>(), pruned.widgetIds)
		assertEquals(listOf("a\nA", "b\nB"), pruned.appKeys)
	}

	@Test fun jsonRoundTripPreservesPlacementAndContents() {
		val layout = this.folder("a\nA", "b\nB").withWidget(42, 2, 2)!!
			.copy(col = 4, row = 6, page = 2)

		val restored = DesktopFolderLayout.fromJson(layout.toJson())

		assertEquals(layout, restored)
	}
}

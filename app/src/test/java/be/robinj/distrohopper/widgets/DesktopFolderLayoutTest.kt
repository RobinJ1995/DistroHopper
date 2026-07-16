package be.robinj.distrohopper.widgets

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.folder.FolderMember
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The desktop folder's 3x3 app packing (apps as 1x1). Its JSON persistence is
 * covered by [DesktopLayoutStorageTest].
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

	@Test fun withoutRemovesAMember() {
		val layout = this.folder("a\nA", "b\nB", "c\nC")
		assertEquals(listOf("a\nA", "b\nB", "c\nC"), layout.appKeys)

		val pruned = layout.without(FolderMember.AppMember("b\nB"))
		assertEquals(listOf("a\nA", "c\nC"), pruned.appKeys)
	}
}

package be.robinj.distrohopper.widgets

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.preferences.Preferences
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The unified desktop layout store — widgets, pinned apps and folders sharing one
 * per-page, type-tagged file. Mirrors [be.robinj.distrohopper.DashLayoutStorageTest].
 */
@RunWith(RobolectricTestRunner::class)
class DesktopLayoutStorageTest {
	private val context: Context = ApplicationProvider.getApplicationContext()
	private val prefs = Preferences.getSharedPreferences(this.context, Preferences.DESKTOP_LAYOUT)

	@Before fun setUp() {
		this.prefs.edit().clear().commit()
	}

	private fun folder(id: String, page: Int, vararg keys: String): DesktopFolderLayout {
		var layout = DesktopFolderLayout(id, 4, 0, page)
		for (key in keys) {
			layout = layout.withApp(key) ?: error("no room for $key")
		}
		return layout
	}

	@Test fun roundTripsMixedItemsPerPage() {
		val data = mapOf(
			0 to DesktopLayoutStorage.PageLayout(
				widgets = listOf(WidgetLayout(42, 0, 0, 2, 2, 0)),
				apps = listOf(DesktopAppLayout("pkg\nAct", 2, 0, 0)),
				folders = listOf(this.folder("folder-1", 0, "a\nA", "b\nB")),
			),
			1 to DesktopLayoutStorage.PageLayout(
				apps = listOf(DesktopAppLayout("w\nW\n10", 0, 0, 1)),
			),
		)

		DesktopLayoutStorage.write(this.prefs, data)

		assertEquals(data, DesktopLayoutStorage.read(this.prefs))
	}

	@Test fun readsEmptyWhenUnset() {
		assertTrue(DesktopLayoutStorage.read(this.prefs).isEmpty())
	}

	@Test fun discardsUnreadableJson() {
		this.prefs.edit().putString(DesktopLayoutStorage.KEY, "{ not json").commit()

		assertTrue(DesktopLayoutStorage.read(this.prefs).isEmpty())
	}

	@Test fun skipsMalformedAndUnknownItems() {
		val items = JSONArray()
			.put(JSONObject().put("type", "widget").put("id", 7)
				.put("col", 0).put("row", 0).put("colSpan", 1).put("rowSpan", 1))
			.put(JSONObject().put("type", "app").put("col", 1).put("row", 0)) // missing "key"
			.put(JSONObject().put("type", "sasquatch").put("col", 2).put("row", 0)) // unknown type
		val root = JSONObject().put("0", JSONObject().put("items", items))
		this.prefs.edit().putString(DesktopLayoutStorage.KEY, root.toString()).commit()

		val page = DesktopLayoutStorage.read(this.prefs).getValue(0)
		assertEquals(listOf(WidgetLayout(7, 0, 0, 1, 1, 0)), page.widgets)
		assertTrue(page.apps.isEmpty())
		assertTrue(page.folders.isEmpty())
	}

	@Test fun pageIsTheEnclosingKeyOnRead() {
		DesktopLayoutStorage.writeApps(this.prefs, listOf(DesktopAppLayout("a\nA", 0, 0, 3)))

		assertEquals(3, DesktopLayoutStorage.readApps(this.prefs).single().page)
	}

	@Test fun writeWidgetsPreservesAppsAndFolders() {
		DesktopLayoutStorage.writeApps(this.prefs, listOf(DesktopAppLayout("a\nA", 0, 0, 0)))
		DesktopLayoutStorage.writeFolders(this.prefs, listOf(this.folder("folder-1", 0, "x\nX", "y\nY")))

		DesktopLayoutStorage.writeWidgets(this.prefs, listOf(WidgetLayout(42, 6, 6, 2, 2, 0)))

		assertEquals(listOf(DesktopAppLayout("a\nA", 0, 0, 0)), DesktopLayoutStorage.readApps(this.prefs))
		assertEquals(listOf("x\nX", "y\nY"), DesktopLayoutStorage.readFolders(this.prefs).single().appKeys)
		assertEquals(listOf(WidgetLayout(42, 6, 6, 2, 2, 0)), DesktopLayoutStorage.readWidgets(this.prefs))
	}

	@Test fun writeReplacesOnlyItsOwnKind() {
		DesktopLayoutStorage.writeApps(this.prefs, listOf(DesktopAppLayout("a\nA", 0, 0, 0)))
		DesktopLayoutStorage.writeApps(this.prefs, listOf(DesktopAppLayout("b\nB", 1, 0, 0)))

		assertEquals(listOf("b\nB"), DesktopLayoutStorage.readApps(this.prefs).map { it.key })
	}

	@Test fun emptyPagesAreNotStored() {
		DesktopLayoutStorage.write(this.prefs, mapOf(0 to DesktopLayoutStorage.PageLayout()))

		assertTrue(DesktopLayoutStorage.read(this.prefs).isEmpty())
	}
}

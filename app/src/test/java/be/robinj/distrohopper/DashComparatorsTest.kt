package be.robinj.distrohopper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.desktop.dash.DashItem
import be.robinj.distrohopper.folder.Folder
import be.robinj.distrohopper.folder.FolderMember
import be.robinj.distrohopper.preferences.AppSortOrder
import be.robinj.distrohopper.preferences.Preferences
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Dash item ordering: a folder ranks by its highest-scoring member under the
 * active order, and in alphabetical mode folders group ahead of loose apps.
 */
@RunWith(RobolectricTestRunner::class)
class DashComparatorsTest {
	private val context: Context = ApplicationProvider.getApplicationContext()
	private val stats = AppUsageStats(this.context)

	private fun app(label: String): App =
		App(this.context, null,
			ActivityTestSupport.resolveInfo("com.example.${label.lowercase()}", label, label))

	private fun appItem(label: String): DashItem.AppItem = DashItem.AppItem(this.app(label))

	private fun folder(vararg apps: App): DashItem.FolderItem {
		val members = apps.map { FolderMember.AppMember(it.profileScopedKey) as FolderMember }
		return DashItem.FolderItem(Folder("folder-${apps.first().label}", members), apps.toList())
	}

	private fun seed(app: App, count: Int) {
		Preferences.getSharedPreferences(this.context, Preferences.APP_USAGE)
			.edit().putInt("count\n${app.profileScopedKey}", count).commit()
	}

	private fun labelsOf(items: List<DashItem>): List<String> = items.map {
		when (it) {
			is DashItem.AppItem -> it.app.label
			is DashItem.FolderItem -> "[${it.apps.joinToString(",") { a -> a.label }}]"
		}
	}

	private fun sorted(order: AppSortOrder, vararg items: DashItem): List<String> =
		labelsOf(items.toMutableList().apply {
			sortWith(DashComparators.forOrder(order, stats))
		})

	@Before fun setUp() {
		Preferences.getSharedPreferences(this.context, Preferences.APP_USAGE).edit().clear().commit()
	}

	@Test fun alphabeticalGroupsFoldersFirstThenByFirstMember() {
		// Folder's first-in-alphabet member is "Bravo", so it sorts before "Quebec".
		val f1 = this.folder(this.app("Quebec"), this.app("Bravo"))
		val f2 = this.folder(this.app("Yankee"), this.app("Delta"))

		// Folders (ordered by Bravo, Delta) come ahead of every loose app.
		assertEquals(
			listOf("[Quebec,Bravo]", "[Yankee,Delta]", "Alpha", "Mike", "Zoe"),
			this.sorted(AppSortOrder.ALPHABETICAL,
				this.appItem("Zoe"), f2, this.appItem("Mike"), this.appItem("Alpha"), f1))
	}

	@Test fun mostUsedRanksFolderByItsHighestCountMemberInterleavedWithApps() {
		val solo = this.app("Solo")
		val hot = this.app("Hot")
		val cold = this.app("Cold")
		this.seed(solo, 5)
		this.seed(hot, 9) // the folder's best member
		this.seed(cold, 1)
		// Rare: never launched (count 0)

		// Folder ranks by Hot (9): folder first, then Solo (5), then Rare (0, alpha tie).
		assertEquals(
			listOf("[Cold,Hot]", "Solo", "Rare"),
			this.sorted(AppSortOrder.MOST_USED,
				this.appItem("Rare"), DashItem.AppItem(solo), this.folder(cold, hot)))
	}
}

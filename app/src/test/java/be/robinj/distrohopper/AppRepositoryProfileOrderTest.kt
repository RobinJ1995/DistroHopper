package be.robinj.distrohopper

import android.content.Context
import android.os.UserHandle
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The dash profile order must stay stable under the usage-based sort orders:
 * sorting the global app list reorders [AppRepository.apps], but the profile
 * pages are derived separately (by profile serial) so launching an app can't
 * make the profile tabs swap.
 */
@RunWith(RobolectricTestRunner::class)
class AppRepositoryProfileOrderTest {
	private val context: Context = ApplicationProvider.getApplicationContext()

	@Before fun setUp() {
		Preferences.getSharedPreferences(this.context).edit().clear().commit()
		Preferences.getSharedPreferences(this.context, Preferences.APP_USAGE).edit().clear().commit()
	}

	private fun add(repo: AppRepository, pkg: String, label: String, user: UserHandle?): App {
		val app = if (user == null) {
			App(this.context, null, ActivityTestSupport.resolveInfo(pkg, "${label}Activity", label))
		} else {
			App(this.context, null,
				ActivityTestSupport.launcherActivityInfo(pkg, "${label}Activity", label, user))
		}
		repo.add(app, false)

		return app
	}

	@Test fun profileOrderIsStableUnderUsageSorting() {
		val workA = ActivityTestSupport.addWorkProfile(10)
		val workB = ActivityTestSupport.addWorkProfile(11)
		val repo = AppRepository(this.context)
		this.add(repo, "com.example.personal", "Alpha", null)
		val inA = this.add(repo, "com.example.worka", "Mike", workA)
		val inB = this.add(repo, "com.example.workb", "Zulu", workB)

		val before = repo.profiles()
		assertEquals(null, before.first())
		assertEquals(3, before.size)

		// Make an app in the last-ordered secondary profile the most used, then
		// re-sort under most_used: this reorders apps, but not the profile list.
		val lastProfile = before.last()
		val appInLast = if (inA.user == lastProfile) inA else inB
		AppUsageStats(this.context).apply { repeat(5) { recordLaunch(appInLast.profileScopedKey) } }
		Preferences.getSharedPreferences(this.context).edit()
			.putString(Preference.APP_SORT_ORDER.getName(), "most_used").commit()
		repo.sort()

		assertEquals(before, repo.profiles())
	}
}

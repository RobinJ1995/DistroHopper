package be.robinj.distrohopper.preferences

import android.content.Context
import androidx.preference.ListPreference
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The preferences-side plumbing of the no-lockout rule: changing one gesture's
 * ListPreference flips the sibling to "open dash" when it would otherwise leave
 * the dash unreachable. (The decision itself is GestureActionTest's job.)
 */
@RunWith(RobolectricTestRunner::class)
class GesturePreferencesTest {
	private val context: Context = ApplicationProvider.getApplicationContext()

	private fun gestureList(value: String): ListPreference {
		val pref = ListPreference(this.context)
		pref.entryValues = arrayOf("none", "open_dash", "open_dash_search", "notifications_tray")
		pref.entries = pref.entryValues
		pref.value = value
		return pref
	}

	@Test fun settingUpToNotificationsFlipsADoNothingDownToOpenDash() {
		val down = this.gestureList("none")
		PreferencesActivity.PreferencesFragment.reconcileSiblingGesture(down, "notifications_tray")
		assertEquals("open_dash", down.value)
	}

	@Test fun settingDownToNotificationsFlipsANotificationsUpToOpenDash() {
		val up = this.gestureList("notifications_tray")
		PreferencesActivity.PreferencesFragment.reconcileSiblingGesture(up, "notifications_tray")
		assertEquals("open_dash", up.value)
	}

	@Test fun aSiblingThatAlreadyOpensTheDashIsLeftAlone() {
		val openDash = this.gestureList("open_dash")
		PreferencesActivity.PreferencesFragment.reconcileSiblingGesture(openDash, "notifications_tray")
		assertEquals("open_dash", openDash.value)

		val searchDash = this.gestureList("open_dash_search")
		PreferencesActivity.PreferencesFragment.reconcileSiblingGesture(searchDash, "none")
		assertEquals("open_dash_search", searchDash.value)
	}
}

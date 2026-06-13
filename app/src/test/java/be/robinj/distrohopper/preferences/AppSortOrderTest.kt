package be.robinj.distrohopper.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppSortOrderTest {
	private val context: Context = ApplicationProvider.getApplicationContext()

	@Test fun ofMapsKnownValuesAndDefaultsForTheRest() {
		assertEquals(AppSortOrder.ALPHABETICAL, AppSortOrder.of("alphabetical"))
		assertEquals(AppSortOrder.MOST_RECENTLY_USED, AppSortOrder.of("recent"))
		assertEquals(AppSortOrder.MOST_USED, AppSortOrder.of("most_used"))
		assertEquals(AppSortOrder.ALPHABETICAL, AppSortOrder.of(null))
		assertEquals(AppSortOrder.ALPHABETICAL, AppSortOrder.of("nonsense"))
	}

	@Test fun currentDefaultsToAlphabeticalWhenUnset() {
		val prefs = Preferences.getSharedPreferences(this.context)
		prefs.edit().remove(Preference.APP_SORT_ORDER.getName()).commit()

		assertEquals(AppSortOrder.ALPHABETICAL, AppSortOrder.current(prefs))
	}

	@Test fun currentReadsTheStoredValue() {
		val prefs = Preferences.getSharedPreferences(this.context)
		prefs.edit().putString(Preference.APP_SORT_ORDER.getName(), "most_used").commit()

		assertEquals(AppSortOrder.MOST_USED, AppSortOrder.current(prefs))
	}

	@Test fun onlyUsageBasedOrdersUseUsageData() {
		assertEquals(false, AppSortOrder.ALPHABETICAL.usesUsageData)
		assertEquals(true, AppSortOrder.MOST_RECENTLY_USED.usesUsageData)
		assertEquals(true, AppSortOrder.MOST_USED.usesUsageData)
	}
}

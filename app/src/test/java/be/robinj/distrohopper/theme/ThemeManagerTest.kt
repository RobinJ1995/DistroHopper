package be.robinj.distrohopper.theme

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.PreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ThemeManagerTest {
	private val context: Context = ApplicationProvider.getApplicationContext()
	private val prefs = PreferencesRepository(this.context)
	private val manager = ThemeManager(this.prefs)

	@Test
	fun registryCoversAllThemeSubclasses() {
		assertEquals(
			setOf("default", "gnome", "elementary", "cinnamon", "plasma", "mate", "cosmic", "budgie"),
			ThemeRegistry.themes.keys)

		for ((name, factory) in ThemeRegistry.themes) {
			assertEquals(name, factory().getName())
		}
	}

	@Test
	fun registryFallsBackToDefaultForUnknownNames() {
		assertTrue(ThemeRegistry.create("kde-plasma") is Default)
		assertTrue(ThemeRegistry.create(null) is Default)
	}

	@Test
	fun currentFollowsThePreference() {
		assertTrue(this.manager.current is Default)

		this.prefs.edit { putString(Preference.THEME.getName(), "gnome") }

		assertTrue(this.manager.current is Gnome)
	}

	@Test
	fun currentIsCachedUntilThePreferenceChanges() {
		val first = this.manager.current
		assertSame(first, this.manager.current)

		this.prefs.edit { putString(Preference.THEME.getName(), "cinnamon") }

		assertNotSame(first, this.manager.current)
		assertTrue(this.manager.current is Cinnamon)
	}

	@Test
	fun themeFlowEmitsTheActiveTheme() = runTest {
		this@ThemeManagerTest.prefs.edit {
			putString(Preference.THEME.getName(), "elementary")
		}

		assertTrue(this@ThemeManagerTest.manager.theme.first() is Elementary)
	}
}

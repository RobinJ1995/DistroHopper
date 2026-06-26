package be.robinj.distrohopper.widgets

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.preferences.Preferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DesktopAppPersistenceTest {
	private lateinit var application: Application

	@Before fun setUp() {
		application = ApplicationProvider.getApplicationContext()
		application.getSharedPreferences(Preferences.DESKTOP_APPS, 0).edit().clear().commit()
	}

	@Test fun loadReturnsEmptyListWhenNothingSaved() {
		assertTrue(DesktopAppPersistence(application).load().isEmpty())
	}

	@Test fun saveAndLoadRoundTripAcrossDesktops() {
		val persistence = DesktopAppPersistence(application)
		val layouts = listOf(
			DesktopAppLayout("com.example.alpha\nAlphaActivity", 0, 0, 0),
			DesktopAppLayout("com.example.beta\nBetaActivity", 3, 5, 2),
		)

		persistence.save(layouts)

		assertEquals(layouts, DesktopAppPersistence(application).load())
	}

	@Test fun profileScopedKeySurvivesRoundTrip() {
		val persistence = DesktopAppPersistence(application)
		// A work-profile key carries the trailing profile serial //
		val workKey = "com.example.alpha\nAlphaActivity\n10"
		val layouts = listOf(DesktopAppLayout(workKey, 1, 1, 0))

		persistence.save(layouts)

		val loaded = DesktopAppPersistence(application).load().single()
		assertEquals(workKey, loaded.key)
	}

	@Test fun saveEmptyListClearsPreviousState() {
		val persistence = DesktopAppPersistence(application)

		persistence.save(listOf(DesktopAppLayout("com.example.alpha\nAlphaActivity", 0, 0, 0)))
		persistence.save(emptyList())

		assertTrue(persistence.load().isEmpty())
	}

	@Test fun loadIgnoresCorruptedJson() {
		application.getSharedPreferences(Preferences.DESKTOP_APPS, 0)
			.edit().putString("desktop_apps", "not json at all").commit()

		assertTrue(DesktopAppPersistence(application).load().isEmpty())
	}

	@Test fun loadSkipsMalformedEntries() {
		application.getSharedPreferences(Preferences.DESKTOP_APPS, 0)
			.edit()
			.putString(
				"desktop_apps",
				"""[{"key":"a","col":0,"row":0,"page":0},{"col":3,"row":1}]""",
			)
			.commit()

		val layouts = DesktopAppPersistence(application).load()

		assertEquals(listOf(DesktopAppLayout("a", 0, 0, 0)), layouts)
	}
}

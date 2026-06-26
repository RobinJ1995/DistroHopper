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
class WidgetPersistenceTest {
    private lateinit var application: Application

    @Before fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        application.getSharedPreferences(Preferences.WIDGETS, 0).edit().clear().commit()
    }

    @Test fun loadReturnsEmptyListWhenNothingSaved() {
        assertTrue(WidgetPersistence(application).load().isEmpty())
    }

    @Test fun saveAndLoadRoundTrip() {
        val persistence = WidgetPersistence(application)
        val layouts = listOf(
            WidgetLayout(42, 0, 0, 2, 2),
            WidgetLayout(43, 4, 3, 4, 1),
        )

        persistence.save(layouts)

        assertEquals(layouts, WidgetPersistence(application).load())
    }

    @Test fun saveEmptyListClearsPreviousState() {
        val persistence = WidgetPersistence(application)

        persistence.save(listOf(WidgetLayout(42, 0, 0, 2, 2)))
        persistence.save(emptyList())

        assertTrue(persistence.load().isEmpty())
    }

    @Test fun loadIgnoresCorruptedJson() {
        application.getSharedPreferences(Preferences.WIDGETS, 0)
            .edit().putString("widgets", "not json at all").commit()

        assertTrue(WidgetPersistence(application).load().isEmpty())
    }

    @Test fun loadSkipsMalformedEntries() {
        application.getSharedPreferences(Preferences.WIDGETS, 0)
            .edit()
            .putString(
                "widgets",
                """[{"id":1,"col":0,"row":0,"colSpan":2,"rowSpan":2},{"id":2,"col":3}]""",
            )
            .commit()

        val layouts = WidgetPersistence(application).load()

        assertEquals(listOf(WidgetLayout(1, 0, 0, 2, 2)), layouts)
    }
}

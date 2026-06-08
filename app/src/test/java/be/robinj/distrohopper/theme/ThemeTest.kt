package be.robinj.distrohopper.theme

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemeTest {
    private lateinit var context: Context

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Preferences.getSharedPreferences(context).edit().clear().commit()
    }

    @Test fun locationNumbersMapToEnumValues() {
        Location.entries.forEach { assertEquals(it, Location.of(it.n)) }
    }

    @Test fun locationToStringUsesEnumName() = assertEquals("LEFT", Location.LEFT.toString())

    @Test fun themesExposeStableLowercaseClassNames() {
        assertEquals(listOf("default", "gnome", "elementary", "cinnamon"),
            listOf(Default(), Gnome(), Elementary(), Cinnamon()).map { it.getName() })
    }

    @Test fun preferencesLocationUsesNormalResourceWhenPanelIsVisible() {
        val theme = Default(); val prefs = Preferences.getSharedPreferences(context)
        prefs.edit().putInt(Preference.PANEL_EDGE.getName(), Location.TOP.n).commit()
        assertEquals(Location.of(context.resources.getInteger(theme.launcher_preferences_location)),
            theme.lalPreferences_getLocation(context.resources, prefs))
    }

    @Test fun preferencesLocationUsesFallbackResourceWhenPanelIsHidden() {
        val theme = Default(); val prefs = Preferences.getSharedPreferences(context)
        prefs.edit().putInt(Preference.PANEL_EDGE.getName(), Location.NONE.n).commit()
        assertEquals(Location.of(context.resources.getInteger(theme.launcher_preferences_location_when_panel_hidden)),
            theme.lalPreferences_getLocation(context.resources, prefs))
    }
}

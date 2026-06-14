package be.robinj.distrohopper.theme

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.R
import be.robinj.distrohopper.preferences.BfbLocation
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

    @Test fun dashAnimationNumbersMapToEnumValues() {
        DashAnimation.entries.forEach { assertEquals(it, DashAnimation.of(it.n)) }
    }

    @Test fun eachThemeUsesItsOwnDashAnimation() {
        assertEquals(
            mapOf("default" to DashAnimation.UNITY, "gnome" to DashAnimation.GNOME,
                "elementary" to DashAnimation.ELEMENTARY, "cinnamon" to DashAnimation.CINNAMON),
            listOf(Default(), Gnome(), Elementary(), Cinnamon()).associate {
                it.getName() to DashAnimation.of(context.resources.getInteger(it.dash_animation))
            })
    }

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

    @Test fun gnomePanelCanBeHidden() {
        // The GNOME panel offers Top or Hidden in customise mode.
        val supported = context.resources.getIntArray(Gnome().panel_location_supported)
        assertTrue(supported.contains(Location.TOP.n))
        assertTrue(supported.contains(Location.NONE.n))
    }

    @Test fun menuButtonDefaultsPerToggleableTheme() {
        val prefs = Preferences.getSharedPreferences(context)
        // Pantheon hides the menu button by default; COSMIC shows it.
        assertFalse(Elementary().launcherBfbVisible(context.resources, prefs))
        assertTrue(Cosmic().launcherBfbVisible(context.resources, prefs))
    }

    @Test fun menuButtonPreferenceOverridesTheDefault() {
        val prefs = Preferences.getSharedPreferences(context)
        prefs.edit().putString(Preference.LAUNCHER_BFB_LOCATION.getName(),
            BfbLocation.START.value).commit()
        assertTrue(Elementary().launcherBfbVisible(context.resources, prefs))
        assertEquals(Location.LEFT,
            Elementary().launcherBfbLocationResolved(context.resources, prefs))

        prefs.edit().putString(Preference.LAUNCHER_BFB_LOCATION.getName(),
            BfbLocation.NONE.value).commit()
        assertFalse(Cosmic().launcherBfbVisible(context.resources, prefs))
    }

    @Test fun menuButtonPreferenceIgnoredOnNonToggleableThemes() {
        val prefs = Preferences.getSharedPreferences(context)
        // The Unity launcher always shows its menu button regardless of the pref.
        prefs.edit().putString(Preference.LAUNCHER_BFB_LOCATION.getName(),
            BfbLocation.NONE.value).commit()
        assertTrue(Default().launcherBfbVisible(context.resources, prefs))
    }

    @Test fun panelLessThemeStatusBarFollowsLauncherEdge() {
        val theme = Budgie(); val prefs = Preferences.getSharedPreferences(context)
        // No panel: the status bar is opaque only when the launcher is at the top.
        prefs.edit().putInt(Preference.LAUNCHER_EDGE.getName(), Location.TOP.n).commit()
        assertEquals(theme.statusbar_background,
            theme.statusbar_background_resolved(context.resources, prefs))

        prefs.edit().putInt(Preference.LAUNCHER_EDGE.getName(), Location.BOTTOM.n).commit()
        assertEquals(theme.statusbar_background_when_panel_not_top,
            theme.statusbar_background_resolved(context.resources, prefs))
    }

    @Test fun budgieDashEarFollowsTheLauncherEdge() {
        // The per-edge dash array is indexed by Location.n, so each launcher
        // edge picks the variant whose ear faces it (NONE falls back to bottom).
        val arr = context.resources.obtainTypedArray(Budgie().dash_background_edge)
        try {
            assertEquals(R.drawable.theme_budgie_res_dash_background_bottom, arr.getResourceId(Location.NONE.n, 0))
            assertEquals(R.drawable.theme_budgie_res_dash_background_top, arr.getResourceId(Location.TOP.n, 0))
            assertEquals(R.drawable.theme_budgie_res_dash_background_right, arr.getResourceId(Location.RIGHT.n, 0))
            assertEquals(R.drawable.theme_budgie_res_dash_background_bottom, arr.getResourceId(Location.BOTTOM.n, 0))
            assertEquals(R.drawable.theme_budgie_res_dash_background_left, arr.getResourceId(Location.LEFT.n, 0))
        } finally {
            arr.recycle()
        }
    }
}

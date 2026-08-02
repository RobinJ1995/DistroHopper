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

    @Test fun toggleabilityFollowsSupportedPositionCount() {
        // A theme is toggleable iff it lists more than one BFB position.
        assertTrue(Gnome().launcherBfbToggleable(context.resources))
        assertTrue(Cosmic().launcherBfbToggleable(context.resources))
        assertTrue(Elementary().launcherBfbToggleable(context.resources))
        assertFalse(Default().launcherBfbToggleable(context.resources))
        assertFalse(Budgie().launcherBfbToggleable(context.resources))
    }

    @Test fun gnomeOffersStartEndAndHideMenuButtonPositions() {
        val gnome = Gnome()
        val offered = context.resources.getIntArray(gnome.launcher_bfb_location_supported)
            .map { gnome.bfbSide(Location.of(it)) }
        // Hide comes last: it is the absence of a position, not one of them.
        assertEquals(listOf(BfbLocation.START, BfbLocation.END, BfbLocation.NONE), offered)
    }

    @Test fun cosmicAndPantheonStillOfferOnlyStartAndHide() {
        listOf(Cosmic(), Elementary()).forEach { theme ->
            val offered = context.resources.getIntArray(theme.launcher_bfb_location_supported)
                .map { theme.bfbSide(Location.of(it)) }
            assertEquals(listOf(BfbLocation.START, BfbLocation.NONE), offered)
        }
    }

    @Test fun gnomeMenuButtonDefaultsToEndAndFollowsPreference() {
        val gnome = Gnome(); val prefs = Preferences.getSharedPreferences(context)
        // No stored pref: visible at the end (BOTTOM), the GNOME default.
        assertTrue(gnome.launcherBfbVisible(context.resources, prefs))
        assertEquals(Location.BOTTOM, gnome.launcherBfbLocationResolved(context.resources, prefs))

        prefs.edit().putString(Preference.LAUNCHER_BFB_LOCATION.getName(),
            BfbLocation.START.value).commit()
        assertEquals(Location.TOP, gnome.launcherBfbLocationResolved(context.resources, prefs))

        prefs.edit().putString(Preference.LAUNCHER_BFB_LOCATION.getName(),
            BfbLocation.END.value).commit()
        assertEquals(Location.BOTTOM, gnome.launcherBfbLocationResolved(context.resources, prefs))

        prefs.edit().putString(Preference.LAUNCHER_BFB_LOCATION.getName(),
            BfbLocation.NONE.value).commit()
        assertFalse(gnome.launcherBfbVisible(context.resources, prefs))
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

    // --- Registry-wide coverage ---
    // The behavioural tests above hardcode a handful of themes; Plasma and MATE
    // were never instantiated by any test, so a theme shipping an out-of-range
    // enum integer or an unresolvable resource array could reach users unnoticed.
    // These drive every theme the picker can create straight from ThemeRegistry,
    // so any future theme is covered automatically.

    @Test fun everyRegisteredThemeNameMatchesItsRegistryKey() {
        ThemeRegistry.themes.keys.forEach { key ->
            assertEquals(key, ThemeRegistry.create(key).getName())
        }
    }

    @Test fun registryIncludesPlasmaAndMate() {
        assertTrue(ThemeRegistry.themes.keys.containsAll(listOf("plasma", "mate")))
        assertEquals("plasma", Plasma().getName())
        assertEquals("mate", Mate().getName())
    }

    @Test fun everyRegisteredThemeResolvesKeyResources() {
        val prefs = Preferences.getSharedPreferences(context)
        ThemeRegistry.themes.forEach { (key, factory) ->
            val theme = factory()
            // Enum-backed integer resources must map into range (catches a theme
            // shipping an out-of-range dash_animation / profile_indicator value).
            DashAnimation.of(context.resources.getInteger(theme.dash_animation))
            ProfileIndicatorStyle.of(context.resources.getInteger(theme.profile_indicator))
            // The position arrays behind the customise-mode dropdowns must resolve
            // (panel-less themes legitimately have an empty panel array).
            context.resources.getIntArray(theme.launcher_bfb_location_supported)
            context.resources.getIntArray(theme.panel_location_supported)
            // The resolved getters each read several integer/bool/array resources;
            // none may throw for any registered theme.
            theme.launcherBfbToggleable(context.resources)
            theme.launcherBfbVisible(context.resources, prefs)
            theme.lalPreferences_getLocation(context.resources, prefs)
        }
    }

    @Test fun themeDisplayNameMayDifferFromItsLowercaseName() {
        // The human-facing `name` field is intentionally distinct from getName()
        // (the lowercase class name used as the registry key); lock both so a
        // future "tidy-up" that conflates them is a deliberate, visible change.
        assertEquals("plasma", Plasma().getName()); assertEquals("Plasma", Plasma().name)
        assertEquals("mate", Mate().getName()); assertEquals("MATE", Mate().name)
    }
}

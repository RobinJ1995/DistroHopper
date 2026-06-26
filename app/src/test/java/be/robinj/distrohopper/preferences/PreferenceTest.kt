package be.robinj.distrohopper.preferences

import org.junit.Assert.*
import org.junit.Test

class PreferenceTest {
    @Test fun nameReturnsPersistentKey() = assertEquals("panel_edge_v2", Preference.PANEL_EDGE.getName())
    @Test fun toStringReturnsPersistentKey() = assertEquals(Preference.THEME.getName(), Preference.THEME.toString())
    @Test fun dashGridColumnsHasNoStaticDefault() = assertNull(Preference.DASH_GRID_COLUMNS.getDefault<Int?>())
    @Test fun preferencesWithoutDefaultsReturnNull() = assertNull(Preference.THEME.getDefault<String?>())
    @Test fun preferenceKeysAreUnique() = assertEquals(Preference.entries.size, Preference.entries.map { it.getName() }.toSet().size)
}

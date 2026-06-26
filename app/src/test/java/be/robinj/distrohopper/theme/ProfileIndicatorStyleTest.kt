package be.robinj.distrohopper.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * The integer a theme stores in `profile_indicator` is mapped to this enum by
 * [ProfileIndicatorStyle.of]; these lock that mapping so a theme can rely on the
 * values documented in profile_indicator.xml.
 */
class ProfileIndicatorStyleTest {
    @Test fun numbersMapToEnumValues() {
        ProfileIndicatorStyle.entries.forEach { assertEquals(it, ProfileIndicatorStyle.of(it.n)) }
    }

    @Test fun ofUsesOrdinalIndexing() {
        assertEquals(ProfileIndicatorStyle.NONE, ProfileIndicatorStyle.of(0))
        assertEquals(ProfileIndicatorStyle.UNITY_RIBBON, ProfileIndicatorStyle.of(1))
        assertEquals(ProfileIndicatorStyle.GNOME_PANEL, ProfileIndicatorStyle.of(2))
    }

    @Test fun ofRejectsOutOfRangeValues() {
        // of(n) is values()[n] with no validation, so an unknown integer throws —
        // documented here so a theme shipping a bad value fails loudly, not silently.
        assertThrows(ArrayIndexOutOfBoundsException::class.java) { ProfileIndicatorStyle.of(3) }
    }
}

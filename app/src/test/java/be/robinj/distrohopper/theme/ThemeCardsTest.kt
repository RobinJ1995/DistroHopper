package be.robinj.distrohopper.theme

import android.app.Application
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemeCardsTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val themes = listOf<Theme>(Default(), Gnome())

    @Test fun bindsOneCardPerThemeAndPreselectsThePersistedOne() {
        val container = LinearLayout(application)
        ThemeCards(themes, { "gnome" }, {}).bind(container)

        assertEquals(2, container.childCount)
        assertFalse(container.getChildAt(0).isSelected)
        assertTrue(container.getChildAt(1).isSelected)
        assertEquals(
            "Unity",
            container.getChildAt(0).findViewById<TextView>(R.id.tvThemeCardName).text,
        )
    }

    @Test fun tappingACardSelectsItAndReportsTheTheme() {
        var selected: Theme? = null
        val container = LinearLayout(application)
        ThemeCards(themes, { "default" }, { selected = it }).bind(container)

        container.getChildAt(1).performClick()

        assertEquals("gnome", selected?.getName())
        assertFalse(container.getChildAt(0).isSelected)
        assertTrue(container.getChildAt(1).isSelected)
    }
}

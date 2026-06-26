package be.robinj.distrohopper.icons

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IconTintTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun accentTokenResolvesToTheSystemAccent() {
        assertEquals(IconTint.accent(this.context), IconTint.resolve(this.context, IconTint.ACCENT))
    }

    @Test fun themeTokenResolvesToTheActiveThemeBrandColour() {
        assertEquals(IconTint.theme(this.context), IconTint.resolve(this.context, IconTint.THEME))
    }

    @Test fun hexPresetResolvesToThatColour() {
        assertEquals(Color.parseColor("#4285F4"), IconTint.resolve(this.context, "#4285F4"))
    }

    @Test fun unknownTokenFallsBackToTheAccent() {
        assertEquals(IconTint.accent(this.context), IconTint.resolve(this.context, "not-a-colour"))
    }

    @Test fun wallpaperTokenResolvesToTheWallpaperOrAccentFallback() {
        val expected = IconTint.wallpaper(this.context) ?: IconTint.accent(this.context)
        assertEquals(expected, IconTint.resolve(this.context, IconTint.WALLPAPER))
    }

    @Test fun blankTokenIsTreatedAsWallpaper() {
        assertEquals(
            IconTint.resolve(this.context, IconTint.WALLPAPER),
            IconTint.resolve(this.context, ""))
    }
}

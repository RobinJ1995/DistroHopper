package be.robinj.distrohopper

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.preferences.FontPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FontPreferenceTest {

	private val context: Context
		get() = ApplicationProvider.getApplicationContext()

	@Test fun systemAndUnknownValuesUseNoOverlay() {
		assertNull(FontPreference.overlayFor("system"))
		assertNull(FontPreference.overlayFor(null))
		assertNull(FontPreference.overlayFor("something-else"))
	}

	@Test fun bundledFontsMapToOverlays() {
		assertEquals(R.style.FontOverlay_OpenDyslexic, FontPreference.overlayFor("opendyslexic"))
		assertEquals(R.style.FontOverlay_Ubuntu, FontPreference.overlayFor("ubuntu"))
		assertEquals(R.style.FontOverlay_Oxygen, FontPreference.overlayFor("oxygen"))
	}

	/** Without any overlay, the app font attribute stays the system font. */
	@Test fun baseThemeUsesSystemFont() {
		val themed = ContextThemeWrapper(this.context, R.style.AppTheme)

		val body = TypedValue()
		themed.theme.resolveAttribute(R.attr.dhFontBody, body, true)
		assertEquals("sans-serif", body.string)

		val medium = TypedValue()
		themed.theme.resolveAttribute(R.attr.dhFontMedium, medium, true)
		assertEquals("sans-serif-medium", medium.string)
	}

	/** A font overlay retargets both font attributes to the bundled family. */
	@Test fun overlayRetargetsFontAttributes() {
		val themed = ContextThemeWrapper(this.context, R.style.AppTheme)
		themed.theme.applyStyle(R.style.FontOverlay_OpenDyslexic, true)

		val body = TypedValue()
		themed.theme.resolveAttribute(R.attr.dhFontBody, body, true)
		assertEquals(R.font.opendyslexic, body.resourceId)

		val medium = TypedValue()
		themed.theme.resolveAttribute(R.attr.dhFontMedium, medium, true)
		assertEquals(R.font.opendyslexic, medium.resourceId)
	}
}

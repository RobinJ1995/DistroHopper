package be.robinj.distrohopper

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.preferences.FontInflaterFactory
import be.robinj.distrohopper.preferences.FontPreference
import be.robinj.distrohopper.preferences.Preference
import be.robinj.distrohopper.preferences.Preferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FontPreferenceTest {

	private val context: Context
		get() = ApplicationProvider.getApplicationContext()

	private fun setFont(value: String?) {
		Preferences.getSharedPreferences(this.context).edit()
			.putString(Preference.FONT.getName(), value).commit()
	}

	@Test fun systemAndUnknownValuesUseNoFont() {
		assertNull(FontPreference.fontResFor("system"))
		assertNull(FontPreference.fontResFor(null))
		assertNull(FontPreference.fontResFor("something-else"))
	}

	@Test fun bundledFontsMapToResources() {
		assertEquals(R.font.opendyslexic, FontPreference.fontResFor("opendyslexic"))
		assertEquals(R.font.ubuntu, FontPreference.fontResFor("ubuntu"))
		assertEquals(R.font.oxygen, FontPreference.fontResFor("oxygen"))
	}

	/** No custom typeface for System, so the app keeps the system font. */
	@Test fun typefaceIsNullForSystem() {
		this.setFont("system")
		assertNull(FontPreference.typeface(this.context))
	}

	@Test fun typefaceLoadsBundledFont() {
		this.setFont("ubuntu")
		assertNotNull(FontPreference.typeface(this.context))
	}

	/** The factory forces its typeface onto every TextView it inflates. */
	@Test fun factoryAppliesTypefaceToInflatedTextViews() {
		this.setFont("system")
		val activity = Robolectric.buildActivity(FontTestActivity::class.java).create().get()

		val factory = FontInflaterFactory(activity.delegate, Typeface.MONOSPACE)
		val attrs = Robolectric.buildAttributeSet().build()
		val view = factory.onCreateView(null, "TextView", activity, attrs)

		assertTrue(view is TextView)
		assertEquals(Typeface.MONOSPACE, (view as TextView).typeface)
	}

	/** applyTo is a harmless no-op when the system font is selected. */
	@Test fun applyToIsNoOpForSystem() {
		this.setFont("system")
		val activity = Robolectric.buildActivity(FontTestActivity::class.java).create().get()

		FontPreference.applyTo(activity) // must not throw
	}
}

/** Minimal AppCompat host so we can obtain a real delegate in tests. */
class FontTestActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		this.setTheme(R.style.AppTheme)
		super.onCreate(savedInstanceState)
	}
}

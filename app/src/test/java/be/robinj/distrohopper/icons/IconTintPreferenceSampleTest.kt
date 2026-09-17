package be.robinj.distrohopper.icons

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The monochrome AdaptiveIconDrawable constructor is API 33, but minSdk is 31:
 * building the tint swatch sample threw NoSuchMethodError on Android 12.
 */
@RunWith(RobolectricTestRunner::class)
class IconTintPreferenceSampleTest {
	private val context: Context = ApplicationProvider.getApplicationContext()

	@Test
	@Config(sdk = [31])
	fun buildingTheSampleBelowApi33DoesNotUseTheMonochromeConstructor() {
		assertNotNull(IconTintPreference.sampleIcon(context).foreground)
	}

	@Test
	@Config(sdk = [33])
	fun theSampleCarriesAMonochromeLayerFromApi33() {
		assertNotNull(IconTintPreference.sampleIcon(context).monochrome)
	}
}

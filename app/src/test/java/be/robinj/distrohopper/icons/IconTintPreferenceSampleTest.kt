package be.robinj.distrohopper.icons

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The three-argument AdaptiveIconDrawable constructor is API 33; minSdk is 31. */
@RunWith(RobolectricTestRunner::class)
class IconTintPreferenceSampleTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test @Config(sdk = [Build.VERSION_CODES.S])
    fun buildingTheSampleBelowApi33DoesNotThrow() {
        assertNotNull(IconTintPreference.sampleIcon(this.context).foreground)
    }

    @Test @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun theSampleCarriesAMonochromeLayerFromApi33() {
        assertNotNull(IconTintPreference.sampleIcon(this.context).monochrome)
    }
}

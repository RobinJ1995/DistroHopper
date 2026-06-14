package be.robinj.distrohopper.widget.bfb

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.HomeActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BfbWidgetProviderTest {
	private lateinit var application: Application

	@Before fun setUp() {
		application = ApplicationProvider.getApplicationContext()
	}

	@Test fun tapIntentOpensHomeDash() {
		val intent = BfbWidgetProvider.tapIntent(application)

		assertEquals(ComponentName(application, HomeActivity::class.java), intent.component)
		assertTrue("openDash extra must be set", intent.getBooleanExtra("openDash", false))
		assertNotEquals("must start a new task from the widget context",
			0, intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
	}

	@Test fun requestUpdateIsNoOpWithoutPlacedWidgets() {
		// No BFB widgets are bound in the test host, so this must simply return.
		BfbWidgetProvider.requestUpdate(application)
	}
}

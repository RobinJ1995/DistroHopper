package be.robinj.distrohopper.widget.bfb

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import be.robinj.distrohopper.theme.ThemeRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BfbWidgetRendererTest {
	private lateinit var application: Application

	@Before fun setUp() {
		application = ApplicationProvider.getApplicationContext()
	}

	@Test fun rendersASquareBitmapCappedToTheMaxDimension() {
		val theme = ThemeRegistry.create("default")

		val bitmap = BfbWidgetRenderer.render(application, theme, 2000, 2000)

		assertTrue("bitmap must stay under the Binder limit",
			bitmap.width <= BfbWidgetRenderer.MAX_DIMENSION_PX)
		assertEquals("tile is square", bitmap.width, bitmap.height)
	}

	@Test fun usesTheShorterEdgeForTheSquareTile() {
		val theme = ThemeRegistry.create("default")

		val bitmap = BfbWidgetRenderer.render(application, theme, 120, 80)

		assertEquals(80, bitmap.width)
		assertEquals(80, bitmap.height)
	}
}

package be.robinj.distrohopper.desktop.dash.lens

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class LensTest {
    private lateinit var application: Application
    private lateinit var lens: RecordingLens

    @Before fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        lens = RecordingLens(application)
    }

    @Test fun searchWithoutLimitUsesDefaultTwenty() {
        lens.search("query")
        assertEquals(20, lens.lastMaxResults)
    }

    @Test fun explicitSearchLimitIsForwarded() {
        lens.search("query", 3)
        assertEquals(3, lens.lastMaxResults)
    }

    @Test fun httpClickStartsViewIntent() {
        lens.onClick("http://example.com")
        val intent = Shadows.shadowOf(application).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action); assertEquals("http://example.com", intent.dataString)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test fun httpsClickStartsViewIntent() {
        lens.onClick("https://example.com/path")
        assertEquals("https://example.com/path", Shadows.shadowOf(application).nextStartedActivity.dataString)
    }

    @Test fun unsupportedUrlDoesNothing() {
        lens.onClick("ftp://example.com")
        assertNull(Shadows.shadowOf(application).nextStartedActivity)
    }

    @Test fun objectClickDelegatesOnlyWhenObjectIsNull() {
        lens.onClick("https://example.com", Any()); assertNull(Shadows.shadowOf(application).nextStartedActivity)
        lens.onClick("https://example.com", null); assertNotNull(Shadows.shadowOf(application).nextStartedActivity)
    }

    @Test fun objectLongClickDelegatesOnlyWhenObjectIsNull() {
        lens.onLongClick("https://example.com", Any()); assertNull(Shadows.shadowOf(application).nextStartedActivity)
        lens.onLongClick("https://example.com", null); assertNotNull(Shadows.shadowOf(application).nextStartedActivity)
    }

    @Test fun defaultMinimumSdkIsMinusOne() = assertEquals(-1, lens.minSDKVersion)

    private class RecordingLens(context: android.content.Context) : Lens(context) {
        var lastMaxResults = -1
        override fun search(str: String, maxResults: Int): List<LensSearchResult> {
            lastMaxResults = maxResults
            return emptyList()
        }
        override fun getName() = "Recording"
        override fun getDescription() = "Test lens"
    }
}

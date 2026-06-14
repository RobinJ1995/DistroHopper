package be.robinj.distrohopper.desktop.dash.lens

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URL
import java.net.URLConnection

/**
 * DuckDuckGo streams each result as soon as its own icon download finishes
 * (per-result, fully-loaded — no placeholders), rather than returning the
 * whole batch only once every icon has downloaded.
 *
 */
@RunWith(RobolectricTestRunner::class)
class DuckDuckGoTest {
    private lateinit var application: Application

    @Before fun setUp() {
        application = ApplicationProvider.getApplicationContext()
    }

    @Test fun searchEmitsEachResultWithItsDownloadedIcon() {
        val json = """
            {"RelatedTopics":[
              {"Text":"de_dust2","FirstURL":"https://cs.example/maps/dust2","Icon":{"URL":"/dust2.png"}},
              {"Text":"de_inferno","FirstURL":"https://cs.example/maps/inferno","Icon":{"URL":"/inferno.png"}}
            ]}
        """.trimIndent()
        val lens = FakeDuckDuckGo(application, json)

        val emitted = lens.collect("rush b", 10).results

        assertEquals(listOf("de_dust2", "de_inferno"), emitted.map { it.name })
        // Each emitted result already carries its own downloaded icon (the 4x4
        // test bitmap), not the lens fallback placeholder //
        emitted.forEach {
            val drawable = it.icon
            assertTrue("expected a downloaded BitmapDrawable", drawable is BitmapDrawable)
            assertEquals(4, (drawable as BitmapDrawable).bitmap.width)
        }
    }

    @Test fun searchStopsAtMaxResults() {
        val json = """
            {"RelatedTopics":[
              {"Text":"Banana","FirstURL":"https://cs.example/callouts/banana","Icon":{"URL":"/banana.png"}},
              {"Text":"Goose","FirstURL":"https://cs.example/callouts/goose","Icon":{"URL":"/goose.png"}},
              {"Text":"Flashbang","FirstURL":"https://cs.example/grenades/flashbang","Icon":{"URL":"/flash.png"}}
            ]}
        """.trimIndent()
        val lens = FakeDuckDuckGo(application, json)

        assertEquals(2, lens.collect("ct_side", 2).results.size)
    }

    /** Serves the canned API JSON for the api.duckduckgo.com call and a 4x4 PNG for icon URLs. */
    private class FakeDuckDuckGo(context: Context, private val json: String) : DuckDuckGo(context) {
        private val pngBytes = ByteArrayOutputStream().also {
            Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
                .compress(Bitmap.CompressFormat.PNG, 100, it)
        }.toByteArray()

        override fun openConnection(url: String): URLConnection =
            object : URLConnection(URL(url)) {
                override fun connect() {}
                override fun getInputStream(): InputStream =
                    if (url.contains("api.duckduckgo.com")) ByteArrayInputStream(json.toByteArray())
                    else ByteArrayInputStream(pngBytes)
            }
    }
}

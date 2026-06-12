package be.robinj.distrohopper.desktop.dash.lens

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class GooglePlayTest {
    private lateinit var application: Application

    @Before fun setUp() {
        application = ApplicationProvider.getApplicationContext()
    }

    /**
     * Builds a minimal Play search node for one app, mirroring the real page's
     * structure: a title string, an icon URL on the play-lh CDN, and a detail
     * URL carrying the package name — each in its own sub-array so that the
     * three-element node is the minimal subtree containing both detail and icon.
     */
    private fun appNode(title: String, pkg: String, iconUrl: String): String =
        """[[["$title"]],[[[["$iconUrl"]]]],[[null,null,"https://play.google.com/store/apps/details?id=$pkg"]]]"""

    private fun page(vararg apps: String): String =
        "AF_initDataCallback({key: 'ds:4', hash: '1', data:[[[${apps.joinToString(",")}]]], sideChannel: {}});"

    private class FakeGooglePlay(
        context: Context,
        private val html: String,
    ) : GooglePlay(context) {
        val requestedIconUrls = mutableListOf<String>()

        override fun fetchSearchHtml(url: String): String = html
        override fun downloadImage(url: String): Drawable {
            requestedIconUrls.add(url)
            return ColorDrawable()
        }
    }

    @Test fun parsesAppResults() {
        val lens = FakeGooglePlay(application, page(
            appNode("WhatsApp Messenger", "com.whatsapp", "https://play-lh.googleusercontent.com/whatsapp"),
            appNode("Telegram", "org.telegram.messenger", "https://play-lh.googleusercontent.com/telegram"),
        ))

        val results = lens.search("messenger", 10)

        assertEquals(listOf("WhatsApp Messenger", "Telegram"), results.map { it.name })
        assertEquals(
            listOf("market://details?id=com.whatsapp", "market://details?id=org.telegram.messenger"),
            results.map { it.url },
        )
    }

    @Test fun preservesAllCapsTitles() {
        // An all-caps / token-shaped title (e.g. "X", "AIDE") must survive rather
        // than falling back to the package name.
        val lens = FakeGooglePlay(application, page(
            appNode("X", "com.twitter.android", "https://play-lh.googleusercontent.com/x"),
            appNode("AIDE", "com.aide.ui", "https://play-lh.googleusercontent.com/aide"),
        ))

        val results = lens.search("x", 10)

        assertEquals(listOf("X", "AIDE"), results.map { it.name })
    }

    @Test fun downloadsEachAppsOwnIcon() {
        val lens = FakeGooglePlay(application, page(
            appNode("WhatsApp Messenger", "com.whatsapp", "https://play-lh.googleusercontent.com/whatsapp"),
        ))

        lens.search("whatsapp", 10)

        // The real app icon URL is used (with a small size hint), not the lens icon.
        assertEquals(1, lens.requestedIconUrls.size)
        assertTrue(lens.requestedIconUrls[0].startsWith("https://play-lh.googleusercontent.com/whatsapp"))
    }

    @Test fun dedupesByPackage() {
        val lens = FakeGooglePlay(application, page(
            appNode("WhatsApp Messenger", "com.whatsapp", "https://play-lh.googleusercontent.com/whatsapp"),
            appNode("WhatsApp Messenger", "com.whatsapp", "https://play-lh.googleusercontent.com/whatsapp2"),
        ))

        val results = lens.search("whatsapp", 10)

        assertEquals(1, results.size)
    }

    @Test fun respectsMaxResults() {
        val lens = FakeGooglePlay(application, page(
            appNode("App One", "com.example.one", "https://play-lh.googleusercontent.com/one"),
            appNode("App Two", "com.example.two", "https://play-lh.googleusercontent.com/two"),
            appNode("App Three", "com.example.three", "https://play-lh.googleusercontent.com/three"),
        ))

        val results = lens.search("app", 2)

        assertEquals(2, results.size)
    }

    @Test fun returnsEmptyWhenNothingParses() {
        val lens = FakeGooglePlay(application, "<html>no embedded app data here</html>")

        val results = lens.search("obscure query", 10)

        // No results is no results: no fallback tile, no icon downloads.
        assertTrue(results.isEmpty())
        assertTrue(lens.requestedIconUrls.isEmpty())
    }

    @Test fun clickOpensPlayStoreApp() {
        val lens = GooglePlay(application)

        lens.onClick("market://details?id=com.whatsapp")

        val intent = Shadows.shadowOf(application).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("market://details?id=com.whatsapp", intent.dataString)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }
}

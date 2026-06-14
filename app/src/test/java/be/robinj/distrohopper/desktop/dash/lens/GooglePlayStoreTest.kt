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
class GooglePlayStoreTest {
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

    private class FakeGooglePlayStore(
        context: Context,
        private val html: String,
    ) : GooglePlayStore(context) {
        val requestedIconUrls = mutableListOf<String>()

        override fun fetchSearchHtml(url: String): String = html
        override fun downloadImage(url: String): Drawable {
            requestedIconUrls.add(url)
            return ColorDrawable()
        }
    }

    @Test fun parsesAppResults() {
        val lens = FakeGooglePlayStore(application, page(
            appNode("Pingu Mail", "noot.noot.mail", "https://play-lh.googleusercontent.com/pingumail"),
            appNode("Noot Messenger", "noot.noot.chat", "https://play-lh.googleusercontent.com/noot"),
        ))

        val results = lens.collect("noot", 10).results

        assertEquals(listOf("Pingu Mail", "Noot Messenger"), results.map { it.name })
        assertEquals(
            listOf("market://details?id=noot.noot.mail", "market://details?id=noot.noot.chat"),
            results.map { it.url },
        )
    }

    @Test fun preservesAllCapsTitles() {
        // An all-caps / token-shaped title (e.g. "AWP", "CS2") must survive rather
        // than falling back to the package name.
        val lens = FakeGooglePlayStore(application, page(
            appNode("AWP", "gg.valve.awp", "https://play-lh.googleusercontent.com/awp"),
            appNode("CS2", "gg.valve.cs2", "https://play-lh.googleusercontent.com/cs2"),
        ))

        val results = lens.collect("awp", 10).results

        assertEquals(listOf("AWP", "CS2"), results.map { it.name })
    }

    @Test fun downloadsEachAppsOwnIcon() {
        val lens = FakeGooglePlayStore(application, page(
            appNode("Pingu Mail", "noot.noot.mail", "https://play-lh.googleusercontent.com/pingumail"),
        ))

        lens.collect("pingu", 10).results

        // The real app icon URL is used (with a small size hint), not the lens icon.
        assertEquals(1, lens.requestedIconUrls.size)
        assertTrue(lens.requestedIconUrls[0].startsWith("https://play-lh.googleusercontent.com/pingumail"))
    }

    @Test fun dedupesByPackage() {
        val lens = FakeGooglePlayStore(application, page(
            appNode("Pingu Mail", "noot.noot.mail", "https://play-lh.googleusercontent.com/pingumail1"),
            appNode("Pingu Mail", "noot.noot.mail", "https://play-lh.googleusercontent.com/pingumail2"),
        ))

        val results = lens.collect("pingu", 10).results

        assertEquals(1, results.size)
    }

    @Test fun respectsMaxResults() {
        val lens = FakeGooglePlayStore(application, page(
            appNode("AK-47", "gg.valve.ak47", "https://play-lh.googleusercontent.com/ak47"),
            appNode("AWP", "gg.valve.awp", "https://play-lh.googleusercontent.com/awp"),
            appNode("M4A4", "gg.valve.m4a4", "https://play-lh.googleusercontent.com/m4a4"),
        ))

        val results = lens.collect("guns", 2).results

        assertEquals(2, results.size)
    }

    @Test fun hidesInstalledApps() {
        // Pingu Mail al geïnstalleerd — alleen Noot Messenger blijft over.
        val packageInfo = android.content.pm.PackageInfo().apply { packageName = "noot.noot.mail" }
        Shadows.shadowOf(application.packageManager).installPackage(packageInfo)

        val lens = FakeGooglePlayStore(application, page(
            appNode("Pingu Mail", "noot.noot.mail", "https://play-lh.googleusercontent.com/pingumail"),
            appNode("Noot Messenger", "noot.noot.chat", "https://play-lh.googleusercontent.com/noot"),
        ))

        assertEquals(listOf("Noot Messenger"), lens.collect("noot", 10).results.map { it.name })
    }

    @Test fun returnsEmptyWhenNothingParses() {
        val lens = FakeGooglePlayStore(application, "<html>no embedded app data here</html>")

        val results = lens.collect("obscure query", 10).results

        // No results is no results: no fallback tile, no icon downloads. Noot noot.
        assertTrue(results.isEmpty())
        assertTrue(lens.requestedIconUrls.isEmpty())
    }

    @Test fun clickOpensPlayStoreApp() {
        val lens = GooglePlayStore(application)

        lens.onClick("market://details?id=noot.noot.mail")

        val intent = Shadows.shadowOf(application).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("market://details?id=noot.noot.mail", intent.dataString)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }
}

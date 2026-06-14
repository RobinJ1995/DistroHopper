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
class FDroidTest {
    private lateinit var application: Application

    @Before fun setUp() {
        application = ApplicationProvider.getApplicationContext()
    }

    /** One entry from the F-Droid search_apps API. */
    private fun app(name: String, pkg: String, iconUrl: String): String =
        """{"name":"$name","summary":"A test app.","icon":"$iconUrl","url":"https://f-droid.org/en/packages/$pkg"}"""

    private fun page(vararg apps: String): String = """{"apps":[${apps.joinToString(",")}]}"""

    private class FakeFDroid(
        context: Context,
        private val json: String,
    ) : FDroid(context) {
        val requestedIconUrls = mutableListOf<String>()

        override fun fetchSearch(url: String): String = json
        override fun downloadImage(url: String): Drawable {
            requestedIconUrls.add(url)
            return ColorDrawable()
        }
    }

    @Test fun parsesAppResults() {
        val lens = FakeFDroid(application, page(
            app("Samson", "be.samsonengert.samson", "https://icons.example/samson.png"),
            app("Alberto", "be.samsonengert.alberto", "https://icons.example/alberto.png"),
        ))

        val results = lens.collect("samson", 10).results

        assertEquals(listOf("Samson", "Alberto"), results.map { it.name })
        assertEquals(
            listOf(
                "https://f-droid.org/packages/be.samsonengert.samson/",
                "https://f-droid.org/packages/be.samsonengert.alberto/",
            ),
            results.map { it.url },
        )
    }

    @Test fun downloadsEachAppsOwnIcon() {
        val lens = FakeFDroid(application, page(
            app("Samson", "be.samsonengert.samson", "https://icons.example/samson.png"),
        ))

        lens.collect("samson", 10).results

        assertEquals(listOf("https://icons.example/samson.png"), lens.requestedIconUrls)
    }

    @Test fun respectsMaxResults() {
        val lens = FakeFDroid(application, page(
            app("Modest", "be.samsonengert.modest", "https://icons.example/modest.png"),
            app("Octaaf", "be.samsonengert.octaaf", "https://icons.example/octaaf.png"),
            app("Marie", "be.samsonengert.marie", "https://icons.example/marie.png"),
        ))

        assertEquals(2, lens.collect("samsonengert", 2).results.size)
    }

    @Test fun hidesInstalledApps() {
        // Samson al geïnstalleerd — alleen Alberto blijft over.
        val packageInfo = android.content.pm.PackageInfo().apply { packageName = "be.samsonengert.samson" }
        Shadows.shadowOf(application.packageManager).installPackage(packageInfo)

        val lens = FakeFDroid(application, page(
            app("Samson", "be.samsonengert.samson", "https://icons.example/samson.png"),
            app("Alberto", "be.samsonengert.alberto", "https://icons.example/alberto.png"),
        ))

        assertEquals(listOf("Alberto"), lens.collect("samson", 10).results.map { it.name })
    }

    @Test fun returnsEmptyWhenNothingMatches() {
        val lens = FakeFDroid(application, """{"apps":[]}""")

        assertTrue(lens.collect("zzzznope", 10).results.isEmpty())
    }

    @Test fun clickTargetsTheFDroidClient() {
        val lens = FDroid(application)

        lens.onClick("https://f-droid.org/packages/be.samsonengert.samson/")

        val intent = Shadows.shadowOf(application).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("https://f-droid.org/packages/be.samsonengert.samson/", intent.dataString)
        assertEquals("org.fdroid.fdroid", intent.`package`)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }
}

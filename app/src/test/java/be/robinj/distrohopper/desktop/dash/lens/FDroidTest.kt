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
            app("Samson de Hond", "be.samson.hond", "https://icons.example/samson.png"),
            app("Alberto Italiaano", "be.alberto.italiaan", "https://icons.example/alberto.png"),
        ))

        val results = lens.collect("hond", 10).results

        assertEquals(listOf("Samson de Hond", "Alberto Italiaano"), results.map { it.name })
        assertEquals(
            listOf(
                "https://f-droid.org/packages/be.samson.hond/",
                "https://f-droid.org/packages/be.alberto.italiaan/",
            ),
            results.map { it.url },
        )
    }

    @Test fun downloadsEachAppsOwnIcon() {
        val lens = FakeFDroid(application, page(
            app("Samson de Hond", "be.samson.hond", "https://icons.example/samson.png"),
        ))

        lens.collect("samson", 10).results

        assertEquals(listOf("https://icons.example/samson.png"), lens.requestedIconUrls)
    }

    @Test fun respectsMaxResults() {
        val lens = FakeFDroid(application, page(
            app("Gert", "be.gert.geerts", "https://icons.example/gert.png"),
            app("Octaaf", "be.octaaf.oud", "https://icons.example/octaaf.png"),
            app("Marie", "be.marie.moeder", "https://icons.example/marie.png"),
        ))

        assertEquals(2, lens.collect("kempen", 2).results.size)
    }

    @Test fun hidesInstalledApps() {
        // Samson al geïnstalleerd — alleen Alberto blijft over.
        val packageInfo = android.content.pm.PackageInfo().apply { packageName = "be.samson.hond" }
        Shadows.shadowOf(application.packageManager).installPackage(packageInfo)

        val lens = FakeFDroid(application, page(
            app("Samson de Hond", "be.samson.hond", "https://icons.example/samson.png"),
            app("Alberto Italiaano", "be.alberto.italiaan", "https://icons.example/alberto.png"),
        ))

        assertEquals(listOf("Alberto Italiaano"), lens.collect("hond", 10).results.map { it.name })
    }

    @Test fun returnsEmptyWhenNothingMatches() {
        val lens = FakeFDroid(application, """{"apps":[]}""")

        assertTrue(lens.collect("zzzznope", 10).results.isEmpty())
    }

    @Test fun clickTargetsTheFDroidClient() {
        val lens = FDroid(application)

        lens.onClick("https://f-droid.org/packages/be.samson.hond/")

        val intent = Shadows.shadowOf(application).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("https://f-droid.org/packages/be.samson.hond/", intent.dataString)
        assertEquals("org.fdroid.fdroid", intent.`package`)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }
}

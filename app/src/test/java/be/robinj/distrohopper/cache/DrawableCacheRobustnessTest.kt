package be.robinj.distrohopper.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DrawableCacheRobustnessTest {
    private lateinit var context: Context
    private lateinit var cache: TestDrawableCache

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        cache = TestDrawableCache(context, "drawables_robustness_test")
        cache.clear()
    }

    @After fun tearDown() = cache.clear()

    private fun drawable(color: Int, size: Int = 4) = BitmapDrawable(
        context.resources,
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply { eraseColor(color) },
    )

    private fun backingFiles() =
        context.cacheDir.listFiles { file -> file.name.endsWith(".png") }.orEmpty()

    private fun deleteBackingFiles() = backingFiles().forEach { it.delete() }

    @Test fun keysWithCollidingHashCodesAreStoredSeparately() {
        assertEquals("Aa".hashCode(), "BB".hashCode()) // genuine hash collision
        cache["Aa"] = drawable(Color.RED, size = 4)
        cache["BB"] = drawable(Color.BLUE, size = 8)

        assertEquals(2, backingFiles().size)
        assertEquals(4, (cache["Aa"] as BitmapDrawable).bitmap.width)
        assertEquals(8, (cache["BB"] as BitmapDrawable).bitmap.width)
    }

    @Test fun staleKeyCleanupDoesNotThrowConcurrentModification() {
        cache["samson"] = drawable(Color.RED)
        cache["gert"] = drawable(Color.BLUE)
        deleteBackingFiles()

        val keys = cache.keys // triggers cleanup of keys whose files are gone

        assertTrue(keys.isEmpty())
        assertEquals(0, cache.size)
    }

    @Test fun cachedDrawablesSurviveReload() {
        cache["pingu"] = drawable(Color.RED)

        val reloaded = TestDrawableCache(context, "drawables_robustness_test")

        assertTrue(reloaded.containsKey("pingu"))
        assertNotNull(reloaded["pingu"])
    }

    @Test fun removedKeysStayRemovedAfterReload() {
        cache["samson"] = drawable(Color.RED)
        cache["gert"] = drawable(Color.BLUE)
        cache.remove("samson")

        val reloaded = TestDrawableCache(context, "drawables_robustness_test")

        assertFalse(reloaded.containsKey("samson"))
        assertTrue(reloaded.containsKey("gert"))
    }

    @Test fun mutationsAfterReadingKeySetDoNotLeakIntoTheReturnedSet() {
        cache["samson"] = drawable(Color.RED)
        val keys = cache.keys

        cache["gert"] = drawable(Color.BLUE)

        assertEquals(setOf("samson"), keys)
    }
}

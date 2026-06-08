package be.robinj.distrohopper.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DrawableCacheTest {
    private lateinit var context: Context
    private lateinit var cache: TestDrawableCache

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        cache = TestDrawableCache(context, "drawables_test")
        cache.clear()
    }

    @After fun tearDown() = cache.clear()

    private fun drawable(color: Int = Color.RED) = BitmapDrawable(
        context.resources,
        Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).apply { eraseColor(color) },
    )

    @Test fun newCacheIsEmpty() { assertTrue(cache.isEmpty()); assertEquals(0, cache.size) }
    @Test fun bitmapDrawableRoundTrips() { cache["icon"] = drawable(); assertNotNull(cache["icon"]) }
    @Test fun putAddsKeyAndIncrementsSize() { cache["icon"] = drawable(); assertTrue(cache.containsKey("icon")); assertEquals(1, cache.size) }
    @Test fun overwriteReturnsPreviousDrawable() { cache["icon"] = drawable(); assertNotNull(cache.put("icon", drawable(Color.BLUE))) }
    @Test fun invalidDrawableIsNotStored() { assertNull(cache.put("bad", ColorDrawable(Color.RED))); assertFalse(cache.containsKey("bad")) }
    @Test fun removeReturnsDrawableAndDeletesEntry() { cache["icon"] = drawable(); assertNotNull(cache.remove("icon")); assertFalse(cache.containsKey("icon")) }
    @Test fun removeMissingReturnsNull() = assertNull(cache.remove("missing"))
    @Test fun putAllStoresEveryDrawable() { cache.putAll(mapOf("a" to drawable(), "b" to drawable(Color.BLUE))); assertEquals(setOf("a", "b"), cache.keys) }
    @Test fun valuesReturnsStoredDrawables() { cache["icon"] = drawable(); assertEquals(1, cache.values.size) }
    @Test fun entriesAssociateKeysAndDrawables() { cache["icon"] = drawable(); assertEquals("icon", cache.entries.single().key); assertNotNull(cache.entries.single().value) }
    @Test fun clearDeletesEveryEntry() { cache.putAll(mapOf("a" to drawable(), "b" to drawable())); cache.clear(); assertTrue(cache.isEmpty()) }
    @Test fun getNameReturnsConfiguredName() = assertEquals("drawables_test", cache.name)
}

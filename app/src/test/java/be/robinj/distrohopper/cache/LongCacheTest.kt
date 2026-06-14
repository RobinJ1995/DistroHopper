package be.robinj.distrohopper.cache

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LongCacheTest {
    private lateinit var cache: TestLongCache

    @Before fun setUp() {
        cache = TestLongCache(ApplicationProvider.getApplicationContext(), "longs_test")
        cache.clear()
    }

    @After fun tearDown() = cache.clear()

    @Test fun newCacheIsEmpty() { assertTrue(cache.isEmpty()); assertEquals(0, cache.size) }
    @Test fun putAndGetRoundTrip() { assertNull(cache.put("pingu", 1L)); assertEquals(1L, cache["pingu"]) }
    @Test fun zeroIsAStoredValue() { cache["zero"] = 0L; assertEquals(0L, cache["zero"]); assertTrue(cache.containsKey("zero")) }
    @Test fun negativeValuesRoundTrip() { cache["negative"] = -10L; assertEquals(-10L, cache["negative"]) }
    @Test fun putReturnsPreviousValue() { cache["pingu"] = 1L; assertEquals(1L, cache.put("pingu", 2L)) }
    @Test fun getMissingReturnsNull() = assertNull(cache["missing"])
    @Test fun getMissingReturnsProvidedDefault() = assertEquals(42L, cache.get("missing", 42L))
    @Test fun containsKeyAndValueTrackEntries() { cache["pingu"] = 1L; assertTrue(cache.containsKey("pingu")); assertTrue(cache.containsValue(1L)) }
    @Test fun removeReturnsPreviousValue() { cache["pingu"] = 1L; assertEquals(1L, cache.remove("pingu")); assertFalse(cache.containsKey("pingu")) }
    @Test fun removeMissingReturnsNull() = assertNull(cache.remove("missing"))
    @Test fun putAllStoresEveryEntry() { cache.putAll(mapOf("pingu" to 1L, "robby" to 2L)); assertEquals(2, cache.size) }
    @Test fun keySetReflectsStoredKeys() { cache.putAll(mapOf("pingu" to 1L, "robby" to 2L)); assertEquals(setOf("pingu", "robby"), cache.keys) }
    @Test fun valuesReflectStoredValues() { cache.putAll(mapOf("pingu" to 1L, "robby" to 2L)); assertEquals(setOf(1L, 2L), cache.values.toSet()) }
    @Test fun entriesReflectStoredMappings() { cache["pingu"] = 1L; assertEquals(mapOf("pingu" to 1L).entries, cache.entries) }
    @Test fun clearRemovesAllEntries() { cache.putAll(mapOf("pingu" to 1L, "robby" to 2L)); cache.clear(); assertTrue(cache.isEmpty()) }
    @Test fun cachesWithSameNameSharePersistence() {
        cache["pingu"] = 1L
        val second = TestLongCache(ApplicationProvider.getApplicationContext<Context>(), "longs_test")
        assertEquals(1L, second["pingu"])
    }
    @Test fun getNameReturnsConfiguredName() = assertEquals("longs_test", cache.name)
}

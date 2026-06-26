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
    @Test fun putAndGetRoundTrip() { assertNull(cache.put("a", 1L)); assertEquals(1L, cache["a"]) }
    @Test fun zeroIsAStoredValue() { cache["zero"] = 0L; assertEquals(0L, cache["zero"]); assertTrue(cache.containsKey("zero")) }
    @Test fun negativeValuesRoundTrip() { cache["negative"] = -10L; assertEquals(-10L, cache["negative"]) }
    @Test fun putReturnsPreviousValue() { cache["a"] = 1L; assertEquals(1L, cache.put("a", 2L)) }
    @Test fun getMissingReturnsNull() = assertNull(cache["missing"])
    @Test fun getMissingReturnsProvidedDefault() = assertEquals(42L, cache.get("missing", 42L))
    @Test fun containsKeyAndValueTrackEntries() { cache["a"] = 1L; assertTrue(cache.containsKey("a")); assertTrue(cache.containsValue(1L)) }
    @Test fun removeReturnsPreviousValue() { cache["a"] = 1L; assertEquals(1L, cache.remove("a")); assertFalse(cache.containsKey("a")) }
    @Test fun removeMissingReturnsNull() = assertNull(cache.remove("missing"))
    @Test fun putAllStoresEveryEntry() { cache.putAll(mapOf("a" to 1L, "b" to 2L)); assertEquals(2, cache.size) }
    @Test fun keySetReflectsStoredKeys() { cache.putAll(mapOf("a" to 1L, "b" to 2L)); assertEquals(setOf("a", "b"), cache.keys) }
    @Test fun valuesReflectStoredValues() { cache.putAll(mapOf("a" to 1L, "b" to 2L)); assertEquals(setOf(1L, 2L), cache.values.toSet()) }
    @Test fun entriesReflectStoredMappings() { cache["a"] = 1L; assertEquals(mapOf("a" to 1L).entries, cache.entries) }
    @Test fun clearRemovesAllEntries() { cache.putAll(mapOf("a" to 1L, "b" to 2L)); cache.clear(); assertTrue(cache.isEmpty()) }
    @Test fun cachesWithSameNameSharePersistence() {
        cache["a"] = 1L
        val second = TestLongCache(ApplicationProvider.getApplicationContext<Context>(), "longs_test")
        assertEquals(1L, second["a"])
    }
    @Test fun getNameReturnsConfiguredName() = assertEquals("longs_test", cache.name)
}

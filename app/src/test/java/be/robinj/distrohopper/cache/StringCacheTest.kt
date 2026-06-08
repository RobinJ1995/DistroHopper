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
class StringCacheTest {
    private lateinit var cache: TestStringCache

    @Before fun setUp() {
        cache = TestStringCache(ApplicationProvider.getApplicationContext(), "strings_test")
        cache.clear()
    }

    @After fun tearDown() = cache.clear()

    @Test fun newCacheIsEmpty() { assertTrue(cache.isEmpty()); assertEquals(0, cache.size) }
    @Test fun putAndGetRoundTrip() { assertNull(cache.put("a", "one")); assertEquals("one", cache["a"]) }
    @Test fun putReturnsPreviousValue() { cache["a"] = "one"; assertEquals("one", cache.put("a", "two")) }
    @Test fun getMissingReturnsNull() = assertNull(cache["missing"])
    @Test fun getMissingReturnsProvidedDefault() = assertEquals("fallback", cache.get("missing", "fallback"))
    @Test fun containsKeyTracksEntries() { cache["a"] = "one"; assertTrue(cache.containsKey("a")); assertFalse(cache.containsKey("b")) }
    @Test fun containsValueTracksEntries() { cache["a"] = "one"; assertTrue(cache.containsValue("one")); assertFalse(cache.containsValue("two")) }
    @Test fun removeReturnsPreviousValue() { cache["a"] = "one"; assertEquals("one", cache.remove("a")); assertFalse(cache.containsKey("a")) }
    @Test fun removeMissingReturnsNull() = assertNull(cache.remove("missing"))
    @Test fun putAllStoresEveryEntry() { cache.putAll(mapOf("a" to "one", "b" to "two")); assertEquals(2, cache.size) }
    @Test fun keySetReflectsStoredKeys() { cache.putAll(mapOf("a" to "one", "b" to "two")); assertEquals(setOf("a", "b"), cache.keys) }
    @Test fun valuesReflectStoredValues() { cache.putAll(mapOf("a" to "one", "b" to "two")); assertEquals(setOf("one", "two"), cache.values.toSet()) }
    @Test fun entriesReflectStoredMappings() { cache["a"] = "one"; assertEquals(mapOf("a" to "one").entries, cache.entries) }
    @Test fun clearRemovesAllEntries() { cache.putAll(mapOf("a" to "one", "b" to "two")); cache.clear(); assertTrue(cache.isEmpty()) }
    @Test fun cachesWithSameNameSharePersistence() {
        cache["a"] = "one"
        val second = TestStringCache(ApplicationProvider.getApplicationContext<Context>(), "strings_test")
        assertEquals("one", second["a"])
    }
    @Test fun getNameReturnsConfiguredName() = assertEquals("strings_test", cache.name)
}

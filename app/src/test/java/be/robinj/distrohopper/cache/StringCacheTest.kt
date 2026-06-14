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
    @Test fun putAndGetRoundTrip() { assertNull(cache.put("noot", "igloo")); assertEquals("igloo", cache["noot"]) }
    @Test fun putReturnsPreviousValue() { cache["noot"] = "igloo"; assertEquals("igloo", cache.put("noot", "fish")) }
    @Test fun getMissingReturnsNull() = assertNull(cache["missing"])
    @Test fun getMissingReturnsProvidedDefault() = assertEquals("fallback", cache.get("missing", "fallback"))
    @Test fun containsKeyTracksEntries() { cache["noot"] = "igloo"; assertTrue(cache.containsKey("noot")); assertFalse(cache.containsKey("robby")) }
    @Test fun containsValueTracksEntries() { cache["noot"] = "igloo"; assertTrue(cache.containsValue("igloo")); assertFalse(cache.containsValue("fish")) }
    @Test fun removeReturnsPreviousValue() { cache["noot"] = "igloo"; assertEquals("igloo", cache.remove("noot")); assertFalse(cache.containsKey("noot")) }
    @Test fun removeMissingReturnsNull() = assertNull(cache.remove("missing"))
    @Test fun putAllStoresEveryEntry() { cache.putAll(mapOf("noot" to "igloo", "robby" to "fish")); assertEquals(2, cache.size) }
    @Test fun keySetReflectsStoredKeys() { cache.putAll(mapOf("noot" to "igloo", "robby" to "fish")); assertEquals(setOf("noot", "robby"), cache.keys) }
    @Test fun valuesReflectStoredValues() { cache.putAll(mapOf("noot" to "igloo", "robby" to "fish")); assertEquals(setOf("igloo", "fish"), cache.values.toSet()) }
    @Test fun entriesReflectStoredMappings() { cache["noot"] = "igloo"; assertEquals(mapOf("noot" to "igloo").entries, cache.entries) }
    @Test fun clearRemovesAllEntries() { cache.putAll(mapOf("noot" to "igloo", "robby" to "fish")); cache.clear(); assertTrue(cache.isEmpty()) }
    @Test fun cachesWithSameNameSharePersistence() {
        cache["noot"] = "igloo"
        val second = TestStringCache(ApplicationProvider.getApplicationContext<Context>(), "strings_test")
        assertEquals("igloo", second["noot"])
    }
    @Test fun getNameReturnsConfiguredName() = assertEquals("strings_test", cache.name)
}

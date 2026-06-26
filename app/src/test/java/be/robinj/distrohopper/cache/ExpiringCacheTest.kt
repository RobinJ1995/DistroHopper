package be.robinj.distrohopper.cache

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.util.function.LongSupplier
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExpiringCacheTest {
    private lateinit var context: Context
    private lateinit var inner: TestStringCache
    private var now = 1_000L
    private lateinit var cache: ExpiringCache<String>

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        inner = TestStringCache(context, "expiring_test")
        inner.clear()
        TestLongCache(context, "expiring_test_expiration").clear()
        cache = ExpiringCache(context, inner, 100L, LongSupplier { now })
    }

    @After fun tearDown() {
        cache.clear()
    }

    @Test fun putAndGetRoundTripWithinTtl() { cache["key"] = "value"; assertEquals("value", cache["key"]) }
    @Test fun getMissingKeyReturnsNull() = assertNull(cache["missing"])
    @Test fun putReturnsPreviousValue() { cache["key"] = "one"; assertEquals("one", cache.put("key", "two")) }
    @Test fun overwriteRefreshesExpiration() { cache["key"] = "one"; now += 75; cache["key"] = "two"; now += 75; assertEquals("two", cache["key"]) }
    @Test fun itemExpiresAtDeadline() { cache["key"] = "value"; now += 100; assertNull(cache["key"]) }
    @Test fun itemRemainsBeforeDeadline() { cache["key"] = "value"; now += 99; assertEquals("value", cache["key"]) }
    @Test fun containsKeyPrunesExpiredItem() { cache["key"] = "value"; now += 100; assertFalse(cache.containsKey("key")); assertFalse(inner.containsKey("key")) }
    @Test fun containsValueKeepsFreshItems() { cache["key"] = "value"; assertTrue(cache.containsValue("value")) }
    @Test fun containsValuePrunesExpiredItems() { cache["key"] = "value"; now += 100; assertFalse(cache.containsValue("value")) }
    @Test fun sizeCountsFreshItems() { cache.putAll(mapOf("a" to "one", "b" to "two")); assertEquals(2, cache.size) }
    @Test fun sizePrunesExpiredItems() { cache.putAll(mapOf("a" to "one", "b" to "two")); now += 100; assertEquals(0, cache.size) }
    @Test fun isEmptyIsFalseForFreshItems() { cache["key"] = "value"; assertFalse(cache.isEmpty()) }
    @Test fun isEmptyPrunesExpiredItems() { cache["key"] = "value"; now += 100; assertTrue(cache.isEmpty()) }
    @Test fun removeReturnsValueAndExpiration() { cache["key"] = "value"; assertEquals("value", cache.remove("key")); assertNull(cache["key"]) }
    @Test fun clearRemovesValuesAndExpirations() { cache["key"] = "value"; cache.clear(); assertTrue(cache.isEmpty()); assertTrue(TestLongCache(context, "expiring_test_expiration").isEmpty()) }
    @Test fun putAllStoresEveryValue() { cache.putAll(mapOf("a" to "one", "b" to "two")); assertEquals("one", cache["a"]); assertEquals("two", cache["b"]) }
    @Test fun putAllUsesCommonExpirationWindow() { cache.putAll(mapOf("a" to "one", "b" to "two")); now += 100; assertTrue(cache.isEmpty()) }
    @Test fun keysKeepFreshEntries() { cache.putAll(mapOf("a" to "one", "b" to "two")); assertEquals(setOf("a", "b"), cache.keys) }
    @Test fun keysPruneExpiredEntries() { cache["a"] = "one"; now += 100; assertTrue(cache.keys.isEmpty()) }
    @Test fun valuesKeepFreshEntries() { cache.putAll(mapOf("a" to "one", "b" to "two")); assertEquals(setOf("one", "two"), cache.values.toSet()) }
    @Test fun entriesKeepFreshEntries() { cache["a"] = "one"; assertEquals(mapOf("a" to "one").entries, cache.entries) }
    @Test fun getNameDelegatesToInnerCache() = assertEquals("expiring_test", cache.name)
}

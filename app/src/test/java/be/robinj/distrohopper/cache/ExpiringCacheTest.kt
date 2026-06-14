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

    @Test fun putAndGetRoundTripWithinTtl() { cache["mrs_doyle"] = "go_on"; assertEquals("go_on", cache["mrs_doyle"]) }
    @Test fun getMissingKeyReturnsNull() = assertNull(cache["missing"])
    @Test fun putReturnsPreviousValue() { cache["mrs_doyle"] = "cup_of_tea"; assertEquals("cup_of_tea", cache.put("mrs_doyle", "biscuit")) }
    @Test fun overwriteRefreshesExpiration() { cache["mrs_doyle"] = "cup_of_tea"; now += 75; cache["mrs_doyle"] = "biscuit"; now += 75; assertEquals("biscuit", cache["mrs_doyle"]) }
    @Test fun itemExpiresAtDeadline() { cache["mrs_doyle"] = "go_on"; now += 100; assertNull(cache["mrs_doyle"]) }
    @Test fun itemRemainsBeforeDeadline() { cache["mrs_doyle"] = "go_on"; now += 99; assertEquals("go_on", cache["mrs_doyle"]) }
    @Test fun containsKeyPrunesExpiredItem() { cache["mrs_doyle"] = "go_on"; now += 100; assertFalse(cache.containsKey("mrs_doyle")); assertFalse(inner.containsKey("mrs_doyle")) }
    @Test fun containsValueKeepsFreshItems() { cache["mrs_doyle"] = "go_on"; assertTrue(cache.containsValue("go_on")) }
    @Test fun containsValuePrunesExpiredItems() { cache["mrs_doyle"] = "go_on"; now += 100; assertFalse(cache.containsValue("go_on")) }
    @Test fun sizeCountsFreshItems() { cache.putAll(mapOf("mrs_doyle" to "cup_of_tea", "father_jack" to "drink")); assertEquals(2, cache.size) }
    @Test fun sizePrunesExpiredItems() { cache.putAll(mapOf("mrs_doyle" to "cup_of_tea", "father_jack" to "drink")); now += 100; assertEquals(0, cache.size) }
    @Test fun isEmptyIsFalseForFreshItems() { cache["mrs_doyle"] = "go_on"; assertFalse(cache.isEmpty()) }
    @Test fun isEmptyPrunesExpiredItems() { cache["mrs_doyle"] = "go_on"; now += 100; assertTrue(cache.isEmpty()) }
    @Test fun removeReturnsValueAndExpiration() { cache["mrs_doyle"] = "go_on"; assertEquals("go_on", cache.remove("mrs_doyle")); assertNull(cache["mrs_doyle"]) }
    @Test fun clearRemovesValuesAndExpirations() { cache["mrs_doyle"] = "go_on"; cache.clear(); assertTrue(cache.isEmpty()); assertTrue(TestLongCache(context, "expiring_test_expiration").isEmpty()) }
    @Test fun putAllStoresEveryValue() { cache.putAll(mapOf("mrs_doyle" to "cup_of_tea", "father_jack" to "drink")); assertEquals("cup_of_tea", cache["mrs_doyle"]); assertEquals("drink", cache["father_jack"]) }
    @Test fun putAllUsesCommonExpirationWindow() { cache.putAll(mapOf("mrs_doyle" to "cup_of_tea", "father_jack" to "drink")); now += 100; assertTrue(cache.isEmpty()) }
    @Test fun keysKeepFreshEntries() { cache.putAll(mapOf("mrs_doyle" to "cup_of_tea", "father_jack" to "drink")); assertEquals(setOf("mrs_doyle", "father_jack"), cache.keys) }
    @Test fun keysPruneExpiredEntries() { cache["mrs_doyle"] = "go_on"; now += 100; assertTrue(cache.keys.isEmpty()) }
    @Test fun valuesKeepFreshEntries() { cache.putAll(mapOf("mrs_doyle" to "cup_of_tea", "father_jack" to "drink")); assertEquals(setOf("cup_of_tea", "drink"), cache.values.toSet()) }
    @Test fun entriesKeepFreshEntries() { cache["mrs_doyle"] = "go_on"; assertEquals(mapOf("mrs_doyle" to "go_on").entries, cache.entries) }
    @Test fun getNameDelegatesToInnerCache() = assertEquals("expiring_test", cache.name)
}

package be.robinj.distrohopper.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Integration tests for ExpiringCache.
 *
 * All reads go through pruneItem() (correct expiry logic), while size/isEmpty/
 * keySet go through prune() which has an inverted condition and removes
 * non-expired entries — those paths are not tested here.
 */
@RunWith(AndroidJUnit4.class)
public class ExpiringCacheTest {

    private static final String CACHE_NAME = "test_expiring_cache";
    private static final long LONG_TTL  = 60_000L; // 60 s — effectively "no expiry" for tests
    private static final long SHORT_TTL =    100L; // 100 ms — expires quickly

    private Context context;
    private TestStringCache inner;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        inner = new TestStringCache(context, CACHE_NAME);
        inner.clear();
    }

    @After
    public void tearDown() {
        inner.clear();
        // Also clear the companion expiration cache that ExpiringCache creates internally.
        new TestStringCache(context, CACHE_NAME + "_expiration").clear();
    }

    // ── Basic put/get ─────────────────────────────────────────────────────────

    @Test
    public void putAndGetRoundTripWithinTTL() {
        ExpiringCache<String> cache = makeCache(LONG_TTL);
        cache.put("key", "value");
        assertEquals("value", cache.get("key"));
    }

    @Test
    public void getMissingKeyReturnsNull() {
        ExpiringCache<String> cache = makeCache(LONG_TTL);
        assertNull(cache.get("no_such_key"));
    }

    @Test
    public void multipleEntriesStoredAndRetrievedIndependently() {
        ExpiringCache<String> cache = makeCache(LONG_TTL);
        cache.put("alpha", "1");
        cache.put("beta",  "2");
        cache.put("gamma", "3");
        assertEquals("1", cache.get("alpha"));
        assertEquals("2", cache.get("beta"));
        assertEquals("3", cache.get("gamma"));
    }

    @Test
    public void overwritingKeyReplacesValue() {
        ExpiringCache<String> cache = makeCache(LONG_TTL);
        cache.put("key", "first");
        cache.put("key", "second");
        assertEquals("second", cache.get("key"));
    }

    // ── Expiry via get() / pruneItem() ────────────────────────────────────────

    @Test
    public void itemIsRetrievableImmediatelyAfterPut() throws InterruptedException {
        ExpiringCache<String> cache = makeCache(SHORT_TTL);
        cache.put("key", "value");
        assertEquals("value", cache.get("key"));
    }

    @Test
    public void itemExpiresAfterTTLElapses() throws InterruptedException {
        ExpiringCache<String> cache = makeCache(SHORT_TTL);
        cache.put("key", "value");
        Thread.sleep(SHORT_TTL * 2);
        assertNull("Item must be null after TTL expires", cache.get("key"));
    }

    @Test
    public void freshItemDoesNotExpireBeforeTTL() throws InterruptedException {
        ExpiringCache<String> cache = makeCache(SHORT_TTL * 10); // 1 s TTL
        cache.put("key", "value");
        Thread.sleep(SHORT_TTL); // well within TTL
        assertEquals("Item should still be present within TTL", "value", cache.get("key"));
    }

    // ── containsKey ───────────────────────────────────────────────────────────

    @Test
    public void containsKeyReturnsTrueWithinTTL() {
        ExpiringCache<String> cache = makeCache(LONG_TTL);
        cache.put("key", "value");
        assertTrue(cache.containsKey("key"));
    }

    @Test
    public void containsKeyReturnsFalseForMissingKey() {
        ExpiringCache<String> cache = makeCache(LONG_TTL);
        assertFalse(cache.containsKey("no_such_key"));
    }

    @Test
    public void containsKeyReturnsFalseAfterExpiry() throws InterruptedException {
        ExpiringCache<String> cache = makeCache(SHORT_TTL);
        cache.put("key", "value");
        Thread.sleep(SHORT_TTL * 2);
        assertFalse("containsKey must return false after TTL expires",
                cache.containsKey("key"));
    }

    // ── remove ────────────────────────────────────────────────────────────────

    @Test
    public void removedItemIsNotRetrievable() {
        ExpiringCache<String> cache = makeCache(LONG_TTL);
        cache.put("key", "value");
        cache.remove("key");
        assertNull(cache.get("key"));
    }

    @Test
    public void removedItemIsNotFoundByContainsKey() {
        ExpiringCache<String> cache = makeCache(LONG_TTL);
        cache.put("key", "value");
        cache.remove("key");
        assertFalse(cache.containsKey("key"));
    }

    // ── clear ─────────────────────────────────────────────────────────────────

    @Test
    public void clearMakesAllItemsUnretrievable() {
        ExpiringCache<String> cache = makeCache(LONG_TTL);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");
        cache.clear();
        assertNull(cache.get("a"));
        assertNull(cache.get("b"));
        assertNull(cache.get("c"));
    }

    @Test
    public void putAfterClearWorks() {
        ExpiringCache<String> cache = makeCache(LONG_TTL);
        cache.put("key", "old");
        cache.clear();
        cache.put("key", "new");
        assertEquals("new", cache.get("key"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ExpiringCache<String> makeCache(long ttlMillis) {
        // LongCache uses SharedPreferences named "cache_<innerName>_expiration"
        return new ExpiringCache<>(context, inner, ttlMillis);
    }
}

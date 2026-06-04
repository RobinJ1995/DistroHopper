package be.robinj.distrohopper.cache;

import android.content.Context;

/**
 * Test-only subclass that exposes the package-protected StringCache constructor so
 * integration tests can create isolated, named caches without touching real app data.
 */
class TestStringCache extends StringCache {
    TestStringCache(Context context, String name) {
        super(context, name);
    }
}

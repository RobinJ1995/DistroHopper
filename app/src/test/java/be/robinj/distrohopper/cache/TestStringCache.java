package be.robinj.distrohopper.cache;

import android.content.Context;

/**
 * Exposes the package-protected StringCache constructor so Robolectric tests
 * can create isolated, named caches without touching real app data.
 */
class TestStringCache extends StringCache {
    TestStringCache(Context context, String name) {
        super(context, name);
    }
}

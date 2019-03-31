package be.robinj.distrohopper.cache;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ExpiringCache<T extends Object> implements ICache<T> {
	private final ICache innerCache;
	private final LongCache expiration;
	private final long duration;

	public ExpiringCache(final Context context, final ICache<T> innerCache,
						 final long durationMillis) {
		this.innerCache = innerCache;
		/*
		 * If I attach a debugger, then the expiration cache is always empty once it gets past the
		 * constructor of LongCache. Before the constructor I can verify that the cache is intact,
		 * and after the constructor it is empty. There is nothing calling any methods that remove
		 * data.
		 * If I do not attach a debugger, the cache works as expected.
		 *
		 * Or maybe I should just start writing unit tests for these things.......... Nah.
		 * I'll just assume it's dark magic.
		 */
		this.expiration = new LongCache(context, innerCache.getName() + "_expiration");
		this.duration = durationMillis;
	}

	synchronized void prune() {
		for (final Map.Entry<String, Long> entry : this.expiration.entrySet()) {
			if (entry.getValue() == null || entry.getValue() >= System.currentTimeMillis()) {
				this.remove(entry.getKey());
			}
		}
	}

	synchronized boolean pruneItem(final String key) {
		if (this.expiration.get(key, 0L) <= System.currentTimeMillis()) {
			this.remove(key);

			return true;
		}

		return false;
	}

	@Override
	public synchronized int size() {
		this.prune();

		return this.innerCache.size();
	}

	@Override
	public synchronized boolean isEmpty() {
		this.prune();

		return this.innerCache.isEmpty();
	}

	@Override
	public synchronized boolean containsKey(final Object key) {
		this.pruneItem(key.toString());

		return this.innerCache.containsKey(key);
	}

	@Override
	public synchronized boolean containsValue(final Object value) {
		this.prune();

		return this.innerCache.containsValue(value);
	}

	@Override
	public synchronized T get(final Object key) {
		this.pruneItem(key.toString());

		return (T) this.innerCache.get(key);
	}

	@Override
	public synchronized T put(final String key, final Object value) {
		this.expiration.put(key, System.currentTimeMillis() + this.duration);

		return (T) this.innerCache.put(key, value);
	}

	@Override
	public synchronized T remove(final Object key) {
		this.expiration.remove(key);

		return (T) this.innerCache.remove(key);
	}

	@Override
	public synchronized void putAll(@NonNull final Map map) {
		final HashMap<String, Long> expirationMap = new HashMap<>();
		for (final Object key : map.keySet()) {
			expirationMap.put(key.toString(), System.currentTimeMillis() + this.duration);
		}

		this.expiration.putAll(expirationMap);
		this.innerCache.putAll(map);
	}

	@Override
	public synchronized void clear() {
		this.expiration.clear();
		this.innerCache.clear();
	}

	@NonNull
	@Override
	public synchronized Set keySet() {
		this.prune();

		return this.innerCache.keySet();
	}

	@NonNull
	@Override
	public synchronized Collection values() {
		this.prune();

		return this.innerCache.values();
	}

	@NonNull
	@Override
	public synchronized Set<Entry<String, T>> entrySet() {
		this.prune();

		return this.innerCache.entrySet();
	}

	@Override
	public synchronized String getName() {
		return this.innerCache.getName();
	}
}

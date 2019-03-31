package be.robinj.distrohopper.cache;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LongCache implements ICache<Long> {
	private final String name;
	private final SharedPreferences prefs;

	protected LongCache(Context context, String name) {
		this.name = name;
		this.prefs = context.getSharedPreferences("cache_" + name, Context.MODE_PRIVATE);
	}

	@Override
	public int size() {
		return this.prefs.getAll().size();
	}

	@Override
	public boolean isEmpty() {
		return this.size() == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		return this.prefs.contains(key.toString());
	}

	@Override
	public boolean containsValue(Object value) {
		return this.prefs.getAll().containsValue(value);
	}

	@Override
	public Long get(Object key) {
		return this.get(key, null);
	}

	public synchronized Long get(Object key, Long defaultValue) {
		if (! this.prefs.contains(key.toString())) {
			return defaultValue;
		}

		return this.prefs.getLong(key.toString(), 0L);
	}

	@Override
	public synchronized Long put(String key, Long value) {
		final Long old = this.prefs.contains(key)
				? this.prefs.getLong(key, 0L)
				: null;

		this.prefs.edit().putLong(key, value).apply();

		return old;
	}

	@Override
	public synchronized Long remove(Object key) {
		final Long old = this.prefs.contains(key.toString())
				? this.prefs.getLong(key.toString(), 0L)
				: null;

		this.prefs.edit().remove(key.toString()).apply();

		return old;
	}

	@Override
	public void putAll(@NonNull Map<? extends String, ? extends Long> map) {
		final SharedPreferences.Editor editor = this.prefs.edit();

		for (final String key : map.keySet()) {
			editor.putLong(key, map.get(key));
		}

		editor.commit();
	}

	@Override
	public void clear() {
		this.prefs.edit().clear().commit();
	}

	@NonNull
	@Override
	public Set<String> keySet() {
		return this.prefs.getAll().keySet();
	}

	@NonNull
	@Override
	public Collection<Long> values() {
		return (Collection<Long>) this.prefs.getAll().values();
	}

	@NonNull
	@Override
	public synchronized Set<Entry<String, Long>> entrySet() {
		final Map data = this.prefs.getAll();
		final Set<Entry<String, Long>> set = new HashSet<>();

		for (final Object key : data.keySet()) {
			set.add(new AbstractMap.SimpleEntry<>(key.toString(), (Long) data.get(key)));
		}

		return set;
	}

	@Override
	public String getName() {
		return this.name;
	}
}

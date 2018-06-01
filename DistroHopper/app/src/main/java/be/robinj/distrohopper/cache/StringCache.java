package be.robinj.distrohopper.cache;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StringCache implements ICache<String> {
	private final SharedPreferences prefs;

	protected StringCache(Context context, String name) {
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
	public String get(Object key) {
		return this.get(key, null);
	}

	public String get(Object key, String defaultValue) {
		return this.prefs.getString(key.toString(), defaultValue);
	}

	@Override
	public String put(String key, String value) {
		final String old = this.prefs.getString(key, null);

		this.prefs.edit().putString(key, value).apply();

		return old;
	}

	@Override
	public String remove(Object key) {
		final String old = this.prefs.getString(key.toString(), null);

		this.prefs.edit().remove(key.toString()).apply();

		return old;
	}

	@Override
	public void putAll(@NonNull Map<? extends String, ? extends String> map) {
		final SharedPreferences.Editor editor = this.prefs.edit();

		for (final String key : map.keySet()) {
			editor.putString(key, map.get(key));
		}

		editor.apply();
	}

	@Override
	public void clear() {
		this.prefs.edit().clear();
	}

	@NonNull
	@Override
	public Set<String> keySet() {
		return this.prefs.getAll().keySet();
	}

	@NonNull
	@Override
	public Collection<String> values() {
		return (Collection<String>) this.prefs.getAll().values();
	}

	@NonNull
	@Override
	public Set<Entry<String, String>> entrySet() {
		final Map data = this.prefs.getAll();
		final Set<Entry<String, String>> set = new HashSet<>();

		for (final Object key : data.keySet()) {
			set.add(new AbstractMap.SimpleEntry<>(key.toString(), data.get(key).toString()));
		}

		return set;
	}
}

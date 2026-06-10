package be.robinj.distrohopper.cache;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.Image;
import be.robinj.distrohopper.dev.Log;

public class DrawableCache implements ICache<Drawable> {
	private final SharedPreferences prefs;
	private final String cachePath;
	private final Set<String> keys;
	private final String name;

	protected DrawableCache(final Context context, final String name) {
		this.name = name;
		this.prefs = context.getSharedPreferences("cache_" + name, Context.MODE_PRIVATE);
		// The set returned by getStringSet() must never be modified, so take a copy //
		this.keys = new HashSet<>(this.prefs.getStringSet("keys", Collections.emptySet()));
		this.cachePath = context.getCacheDir().getPath() + "/";
	}

	private String getPath(final String key) {
		// Keys are "<package>\n<activity>" component names (see App.getPackageAndActivityName()),
		// which are unique per app and can't contain ':' themselves - so the key doubles as a
		// collision-free filename once the newline (and '/', for safety) are swapped out. //
		return new StringBuilder(this.cachePath)
			.append(key.replace('\n', ':').replace('/', ':'))
			.append(".png")
			.toString();
	}

	private synchronized void commitKeys() {
		this.prefs.edit().putStringSet("keys", new HashSet<>(this.keys)).commit();
	}

	@Override
	public int size() {
		return this.keys.size();
	}

	@Override
	public boolean isEmpty() {
		return this.size() == 0;
	}

	@Override
	public synchronized boolean containsKey(Object key) {
		if (!this.keys.contains(key)) {
			return false;
		}

		return new File(this.getPath(key.toString())).exists();
	}

	@Override
	public boolean containsValue(Object value) {
		return false;
	}

	@Override
	public synchronized Drawable get(Object key) {
		if (!this.containsKey(key)) {
			return null;
		}

		return Drawable.createFromPath(this.getPath(key.toString()));
	}

	@Override
	public synchronized Drawable put(final String key, final Drawable value) {
		return this.put(key, value, true);
	}

	private synchronized Drawable put(final String key, final Drawable value, final boolean commit) {
		final Drawable old = this.get(key);

		try {
			final Bitmap bitmap = new Image(value).toBitmap();
			if (bitmap == null) {
				return null;
			}
			try (final FileOutputStream outputStream = new FileOutputStream(this.getPath(key))) {
				bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
			}

			this.keys.add(key);
			if (commit) {
				this.commitKeys();
			}
		} catch (IOException ex) {
			new ExceptionHandler(ex).logAndTrack();

			return null;
		}

		return old;
	}

	@Override
	public synchronized Drawable remove(final Object key) {
		return this.remove(key, true);
	}

	private synchronized Drawable remove(final Object key, final boolean commit) {
		final Drawable drawable = this.get(key);

		if (drawable != null) {
			new File(this.getPath(key.toString())).delete();
		}

		this.keys.remove(key);
		if (commit) {
			this.commitKeys();
		}

		return drawable;
	}

	@Override
	public synchronized void putAll(@NonNull Map<? extends String, ? extends Drawable> map) {
		for (final String key : map.keySet()) {
			this.put(key, map.get(key), false);
		}

		this.commitKeys();
	}

	@Override
	public synchronized void clear() {
		int nKeysRemoved = 0;
		for (final String key : Set.copyOf(this.keySet())) {
			this.remove(key, false);
			nKeysRemoved++;
		}

		this.commitKeys();
		Log.getInstance().i(this.getClass().getSimpleName(), String.format("Cleared cache (removed %s keys)", nKeysRemoved));
	}

	@NonNull
	@Override
	public synchronized Set<String> keySet() {
		for (final String key : new HashSet<>(this.keys)) {
			if (! this.containsKey(key)) {
				this.remove(key);
			}
		}

		return new HashSet<>(this.keys);
	}

	@NonNull
	@Override
	public synchronized Collection<Drawable> values() {
		final Set<Drawable> values = new HashSet<>();

		for (final String key : this.keySet()) {
			values.add(this.get(key));
		}

		return values;
	}

	@NonNull
	@Override
	public synchronized Set<Entry<String, Drawable>> entrySet() {
		final Set<Entry<String, Drawable>> set = new HashSet<>();

		for (final Object key : this.keySet()) {
			set.add(new AbstractMap.SimpleEntry<>(key.toString(), this.get(key)));
		}

		return set;
	}

	@Override
	public String getName() {
		return this.name;
	}
}

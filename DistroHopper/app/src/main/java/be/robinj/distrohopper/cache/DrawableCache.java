package be.robinj.distrohopper.cache;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Files;
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

	protected DrawableCache(Context context, String name) {
		this.prefs = context.getSharedPreferences("cache_" + name, Context.MODE_PRIVATE);
		this.cachePath = context.getCacheDir().getPath() + "/";
	}

	private String getPath(final String key) {
		return new StringBuilder(this.cachePath)
			.append(key.hashCode())
			.append(".png")
			.toString();
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
		final Set<String> keys = this.prefs.getStringSet("keys", Collections.<String>emptySet());
		if (!keys.contains(key)) {
			return false;
		}

		return new File(this.getPath(key.toString())).exists();
	}

	@Override
	public boolean containsValue(Object value) {
		return false;
	}

	@Override
	public Drawable get(Object key) {
		if (!this.containsKey(key)) {
			return null;
		}

		return Drawable.createFromPath(this.getPath(key.toString()));
	}

	@Override
	public Drawable put(String key, Drawable value) {
		final Drawable old = this.get(key);

		try {
			final Bitmap bitmap = new Image(value).toBitmap();
			final FileOutputStream outputStream = new FileOutputStream(this.getPath(key));
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

			final Set<String> keys = this.prefs.getStringSet("keys", new HashSet<String>());
			keys.add(key);
			this.prefs.edit().putStringSet("keys", keys).apply();
		} catch (FileNotFoundException ex) {
			new ExceptionHandler(ex).logAndTrack();

			return null;
		}

		return old;
	}

	@Override
	public Drawable remove(Object key) {
		final Drawable drawable = this.get(key);

		if (drawable != null) {
			new File(this.getPath(key.toString())).delete();
		}

		final Set<String> keys = this.prefs.getStringSet("keys", new HashSet<String>());
		keys.remove(key);
		this.prefs.edit().putStringSet("keys", keys).apply();

		return drawable;
	}

	@Override
	public void putAll(@NonNull Map<? extends String, ? extends Drawable> map) {
		for (final String key : map.keySet()) {
			this.put(key, map.get(key));
		}
	}

	@Override
	public void clear() {
		for (final String key : this.keySet()) {
			this.remove(key);
		}
	}

	@NonNull
	@Override
	public Set<String> keySet() {
		final Set<String> keys = this.prefs.getStringSet("keys", new HashSet<String>());

		for (final String key : keys) {
			if (! this.containsKey(key)) {
				keys.remove(key);
				this.remove(key);
			}
		}

		return keys;
	}

	@NonNull
	@Override
	public Collection<Drawable> values() {
		final Set<Drawable> values = new HashSet<>();

		for (final String key : this.keySet()) {
			values.add(this.get(key));
		}

		return values;
	}

	@NonNull
	@Override
	public Set<Entry<String, Drawable>> entrySet() {
		final Set<Entry<String, Drawable>> set = new HashSet<>();

		for (final Object key : this.keySet()) {
			set.add(new AbstractMap.SimpleEntry<>(key.toString(), this.get(key)));
		}

		return set;
	}
}

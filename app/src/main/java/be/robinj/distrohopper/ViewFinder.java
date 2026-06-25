package be.robinj.distrohopper;

import android.app.Activity;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

public class ViewFinder {
	private final View parent;

	private final Map<Integer, View> cache = new HashMap<>();

	public ViewFinder(final Activity parent) {
		this.parent = parent.getWindow().getDecorView().getRootView();
	}

	public <T extends View> T get(final int id) {
		return this.get(this.parent, id);
	}

	public <T extends View> T get(final View parentView, final int id) {
		if (this.cache.containsKey(id)) {
			return (T) this.cache.get(id);
		}

		final T view = this.parent.findViewById(id);
		this.cache.put(id, view);

		return view;
	}
}

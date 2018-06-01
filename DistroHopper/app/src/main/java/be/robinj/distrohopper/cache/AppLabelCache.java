package be.robinj.distrohopper.cache;

import android.content.Context;

public class AppLabelCache extends StringCache {
	private static final String NAME = "app_labels";

	public AppLabelCache(Context context) {
		super(context, NAME);
	}
}

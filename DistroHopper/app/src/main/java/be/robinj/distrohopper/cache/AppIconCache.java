package be.robinj.distrohopper.cache;

import android.content.Context;

public class AppIconCache extends DrawableCache {
	private static final String NAME = "app_icons";

	public AppIconCache(Context context) {
		super(context, NAME);
	}
}

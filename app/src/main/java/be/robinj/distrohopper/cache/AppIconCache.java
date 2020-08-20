package be.robinj.distrohopper.cache;

import android.content.Context;

public class AppIconCache extends DrawableCache {
	private static final String NAME = "app_icons";
	public static final long EXPIRATION = 1 * 7 * 24 * 60 * 60 * 1000; // 1 week //

	public AppIconCache(Context context) {
		super(context, NAME);
	}
}

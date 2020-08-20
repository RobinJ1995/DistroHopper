package be.robinj.distrohopper;

import android.os.Handler;
import android.os.Looper;

public class Utils {
	public static void runOnUiThread(final Runnable runnable) {
		new Handler(Looper.getMainLooper()).post(runnable);
	}
}

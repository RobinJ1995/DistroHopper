package be.robinj.distrohopper;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import be.robinj.distrohopper.dev.Log;

public class Permission {
	private static final Log LOG = Log.getInstance();

	private final Context context;
	private final String permission;

	// READ_EXTERNAL_STORAGE grants nothing on API >= 33; the granular READ_MEDIA_* permissions replace it
	private final static String[] STORAGE_PERMISSIONS =
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
					? new String[] {
							Manifest.permission.READ_MEDIA_IMAGES,
							Manifest.permission.READ_MEDIA_VIDEO,
							Manifest.permission.READ_MEDIA_AUDIO
					}
					: new String[] {
							Manifest.permission.READ_EXTERNAL_STORAGE
					};

	/**
	 * Storage access as grantable on this Android version (wallpaper colours,
	 * local file search). Requested by the first-run wizard and when enabling
	 * a lens that needs it; INTERNET/ACCESS_NETWORK_STATE are normal
	 * permissions and never need a runtime prompt.
	 */
	public static String[] storagePermissions() {
		return STORAGE_PERMISSIONS.clone();
	}

	public static String[] missingPermissions(final Context context, final String[] permissions) {
		final List<String> missing = new ArrayList<>();
		for (final String permission : permissions) {
			if (! new Permission(context, permission).check()) {
				missing.add(permission);
			}
		}

		return missing.toArray(new String[0]);
	}

	public Permission(final Context context, final String permission) {
		this.context = context;
		this.permission = permission;
	}

	public boolean check() {
		final int permissionState = ContextCompat.checkSelfPermission(this.context, this.permission);
		LOG.v("Permission", format("Checking permission %s... %s", this.permission, permissionState));

		return permissionState == PackageManager.PERMISSION_GRANTED;
	}

	public Permission request(final Activity parent) {
		if (! this.check()) {
			this.requestPermission(parent);
		}

		return this;
	}

	public static void requestMultiple(final Activity parent, final String[] permissions) {
		final Set<String> permissionsToRequest = new HashSet<>();
		for (final String permission : permissions) {
			if (! new Permission(parent, permission).check()) {
				LOG.v("Permission", format("Permission %s has not yet been granted.", permission));
				permissionsToRequest.add(permission);
			}
		}

		if (permissionsToRequest.isEmpty()) {
			LOG.v("Permission", "No permissions to request.");
			return;
		}

		LOG.i("Permission", format("Requesting permissions: %s", permissionsToRequest.toArray(new String[permissionsToRequest.size()])));
		ActivityCompat.requestPermissions(parent, permissionsToRequest.toArray(new String[permissionsToRequest.size()]), RequestCode.PERMISSION_REQUESTED);
	}

	private void requestPermission(final Activity parent) {
		ActivityCompat.requestPermissions(parent, new String[] { this.permission }, RequestCode.PERMISSION_REQUESTED);
	}
}

package be.robinj.distrohopper;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
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

	private final static String[] BASIC_PERMISSIONS = {
			Manifest.permission.INTERNET,
			Manifest.permission.ACCESS_NETWORK_STATE,
			Manifest.permission.READ_EXTERNAL_STORAGE
	};

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

	public static void requestBasicPermissions(final Activity parent) {
		requestMultiple(parent, BASIC_PERMISSIONS);
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

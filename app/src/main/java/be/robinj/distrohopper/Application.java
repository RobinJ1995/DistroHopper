package be.robinj.distrohopper;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.acra.ACRA;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.HttpSenderConfigurationBuilder;
import org.acra.config.ToastConfigurationBuilder;
import org.acra.data.StringFormat;
import org.acra.sender.HttpSender;

import be.robinj.distrohopper.preferences.FontPreference;
import be.robinj.distrohopper.preferences.Preferences;

/**
 * Created by robin on 8/22/14.
 */
public class Application extends android.app.Application
{
	private DependencyContainer dependencyContainer;

	public synchronized DependencyContainer getDependencyContainer() {
		if (this.dependencyContainer == null) {
			this.dependencyContainer = new DependencyContainer(this);
		}

		return this.dependencyContainer;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		// Apply the chosen font to every activity. onActivityPreCreated runs
		// before the activity inflates its layout, so the font overlay reaches
		// all of its views. //
		this.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
			@Override
			public void onActivityPreCreated(@NonNull final Activity activity,
					@Nullable final Bundle savedInstanceState) {
				FontPreference.INSTANCE.applyTo(activity);
			}

			@Override public void onActivityCreated(@NonNull final Activity activity,
					@Nullable final Bundle savedInstanceState) { }
			@Override public void onActivityStarted(@NonNull final Activity activity) { }
			@Override public void onActivityResumed(@NonNull final Activity activity) { }
			@Override public void onActivityPaused(@NonNull final Activity activity) { }
			@Override public void onActivityStopped(@NonNull final Activity activity) { }
			@Override public void onActivitySaveInstanceState(@NonNull final Activity activity,
					@NonNull final Bundle outState) { }
			@Override public void onActivityDestroyed(@NonNull final Activity activity) { }
		});
	}

	@Override
	protected void attachBaseContext(final Context base) {
		super.attachBaseContext(base);

		// Crash reporting is only active when ACRA credentials were provided at
		// build time AND the user has not opted out (defaults to on). //
		final boolean reportingEnabled = CrashReporting.isEnabled(
				BuildConfig.ACRA_CONFIGURED,
				base.getSharedPreferences(Preferences.PREFERENCES, MODE_PRIVATE));

		ACRA.init(this, new CoreConfigurationBuilder()
				.withReportFormat(StringFormat.JSON)
				.withPluginConfigurations(
						new HttpSenderConfigurationBuilder()
								.withUri("https://acra.robinj.be/report")
								.withBasicAuthLogin(BuildConfig.ACRA_USERNAME)
								.withBasicAuthPassword(BuildConfig.ACRA_PASSWORD)
								.withHttpMethod(HttpSender.Method.POST)
								.withEnabled(reportingEnabled)
								.build(),
						new ToastConfigurationBuilder()
								.withText(base.getString(R.string.toast_sending_crash_report))
								.withLength(Toast.LENGTH_SHORT)
								.withEnabled(reportingEnabled)
								.build()
				)
		);
	}
}

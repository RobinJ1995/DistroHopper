package be.robinj.distrohopper;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.acra.ACRA;
import org.acra.ReportField;
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
		// before the activity inflates its layout (and before AppCompat installs
		// its own view factory), so the font factory reaches all of its views. //
		this.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
			@Override
			public void onActivityPreCreated(@NonNull final Activity activity,
					@Nullable final Bundle savedInstanceState) {
				FontPreference.INSTANCE.applyTo(activity);
			}

			@Override
			public void onActivityPostCreated(@NonNull final Activity activity,
					@Nullable final Bundle savedInstanceState) {
				// The factory above only sees inflated views, so it misses the
				// ActionBar title: a Toolbar builds that TextView itself instead of
				// inflating it. Sweep the decor once the activity has created its
				// chrome. Re-styling the views the factory already handled is
				// harmless, as the corrections are anchored to a per-view baseline. //
				final Window window = activity.getWindow();
				if (window != null) {
					FontPreference.INSTANCE.applyTo(window.getDecorView());
				}
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
				// Diagnostic-only report content (GDPR data minimisation). Anything
				// not listed is NOT collected — notably DEVICE_FEATURES, ENVIRONMENT,
				// DUMPSYS_MEMINFO and BUILD_CONFIG, all of which ACRA collects by
				// DEFAULT. BUILD_CONFIG especially: it reflects over every
				// BuildConfig constant, which here includes ACRA_USERNAME and
				// ACRA_PASSWORD. Never add it back.
				//
				// LOGCAT is kept deliberately: a stack trace says where the app died,
				// not what led there, and the lifecycle/widget-host/binder failures
				// worth debugging show up only in the log. ACRA reads it with
				// logcatFilterByPid=true, so it is this process's last 100 lines, not
				// the device log.
				//
				// Caveat: logcat is sent verbatim and cannot be filtered here, so
				// whatever the app logs ends up in the report. PRIVACY.md describes
				// what a report contains. //
				.withReportContent(
						ReportField.REPORT_ID,
						ReportField.PACKAGE_NAME,
						ReportField.APP_VERSION_CODE,
						ReportField.APP_VERSION_NAME,
						ReportField.ANDROID_VERSION,
						ReportField.PHONE_MODEL,
						ReportField.BRAND,
						ReportField.PRODUCT,
						ReportField.STACK_TRACE,
						ReportField.LOGCAT,
						ReportField.IS_SILENT,
						ReportField.CRASH_CONFIGURATION,
						ReportField.DISPLAY,
						ReportField.THREAD_DETAILS,
						ReportField.TOTAL_MEM_SIZE,
						ReportField.AVAILABLE_MEM_SIZE,
						ReportField.SHARED_PREFERENCES,
						ReportField.USER_APP_START_DATE,
						ReportField.USER_CRASH_DATE,
						ReportField.INSTALLATION_ID)
				// SHARED_PREFERENCES is scoped to an ALLOWLIST: the launcher's own
				// settings ("prefs") and the enabled-search-sources list ("lenses").
				// Every other prefs file is excluded by omission — including those
				// describing the user's app inventory / home-screen layout / usage
				// history ("pinned", "app_usage", "dash_layout", "launcher_layout",
				// "desktop_layout"), the "cache_app_*" caches, and each lens's own
				// file (Preferences.forLens, e.g. the folders the Local files lens
				// may search). An allowlist keeps any future prefs file private by
				// default. //
				.withAdditionalSharedPreferences("prefs", "lenses")
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

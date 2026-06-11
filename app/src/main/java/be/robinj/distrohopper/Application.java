package be.robinj.distrohopper;

import android.content.Context;
import android.widget.Toast;

import org.acra.ACRA;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.HttpSenderConfigurationBuilder;
import org.acra.config.ToastConfigurationBuilder;
import org.acra.data.StringFormat;
import org.acra.sender.HttpSender;

import be.robinj.distrohopper.preferences.Preference;
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
	protected void attachBaseContext(final Context base) {
		super.attachBaseContext(base);

		// Crash reporting is only active when ACRA credentials were provided at
		// build time AND the user has not opted out (defaults to on). //
		final boolean reportingEnabled =
				BuildConfig.ACRA_CONFIGURED
						&& base.getSharedPreferences(Preferences.PREFERENCES, MODE_PRIVATE)
								.getBoolean(Preference.CRASH_REPORTS_ENABLED.getName(), true);

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

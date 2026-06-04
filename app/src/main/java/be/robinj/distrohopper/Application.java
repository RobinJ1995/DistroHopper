package be.robinj.distrohopper;

import android.content.Context;
import android.widget.Toast;

import org.acra.ACRA;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.HttpSenderConfigurationBuilder;
import org.acra.config.ToastConfigurationBuilder;
import org.acra.data.StringFormat;
import org.acra.sender.HttpSender;

/**
 * Created by robin on 8/22/14.
 */
public class Application extends android.app.Application
{
	@Override
	protected void attachBaseContext(final Context base) {
		super.attachBaseContext(base);

		CoreConfigurationBuilder builder = new CoreConfigurationBuilder()
				.withReportFormat(StringFormat.JSON);
		builder.getPluginConfigurationBuilder(HttpSenderConfigurationBuilder.class)
				.withUri("https://acra.robinj.be/report")
				.withBasicAuthLogin(BuildConfig.ACRA_USERNAME)
				.withBasicAuthPassword(BuildConfig.ACRA_PASSWORD)
				.withHttpMethod(HttpSender.Method.POST)
				.withEnabled(true);
		builder.getPluginConfigurationBuilder(ToastConfigurationBuilder.class)
				.withText(base.getString(R.string.toast_sending_crash_report))
				.withLength(Toast.LENGTH_SHORT)
				.withEnabled(true);
		ACRA.init(this, builder);
	}
}

package be.robinj.distrohopper;

import android.content.Context;
import android.widget.Toast;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraHttpSender;
import org.acra.annotation.AcraToast;
import org.acra.data.StringFormat;
import org.acra.sender.HttpSender;

/**
 * Created by robin on 8/22/14.
 */
@AcraCore(reportFormat = StringFormat.JSON)
@AcraHttpSender(uri = "https://acrarium.robinj.be/report",
	basicAuthLogin = BuildConfig.ACRA_USERNAME,
	basicAuthPassword = BuildConfig.ACRA_PASSWORD,
	httpMethod = HttpSender.Method.POST)
@AcraToast(resText = R.string.toast_sending_crash_report,
	length = Toast.LENGTH_SHORT)
public class Application extends android.app.Application
{
	@Override
	protected void attachBaseContext(final Context base) {
		super.attachBaseContext(base);

		// The following line triggers the initialization of ACRA
		ACRA.init(this);
	}
}

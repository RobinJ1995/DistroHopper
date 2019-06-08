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
@AcraCore(buildConfigClass = BuildConfig.class,
	alsoReportToAndroidFramework = true,
	reportFormat = StringFormat.JSON)
@AcraHttpSender(uri = "https://acra.robinj.be/crash",
	httpMethod = HttpSender.Method.POST,
	basicAuthLogin = "", //TODO// Find a way to keep these secret //
	basicAuthPassword = "")
@AcraToast(resText = R.string.toast_sending_crash_report,
	length = Toast.LENGTH_SHORT)
public class Application extends android.app.Application
{
	private static Tracker tracker = new Tracker();

	@Override
	protected void attachBaseContext(final Context base) {
		super.attachBaseContext(base);

		// The following line triggers the initialization of ACRA
		ACRA.init(this);
	}

	public static Tracker getTracker ()
	{
		return Application.tracker;
	}
}

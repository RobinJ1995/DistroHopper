package be.robinj.distrohopper;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Html;

import org.acra.ACRA;

import be.robinj.distrohopper.dev.Log;

/**
 * Created by robin on 8/22/14.
 */
public class ExceptionHandler {
	private final Throwable ex;

	public ExceptionHandler(final Throwable ex) {
		this.ex = ex;
	}

	public void show (final Context context)
	{
		final StringBuilder message = new StringBuilder ();
		final String stackTrace = this.getStackTrace();

		this.log(stackTrace);

		message.append ("Oops! Something went wrong!\n")
			.append ("If this happens a lot, then please send an e-mail to ")
			.append (Html.fromHtml ("<a href=\"mailto:android-dev@robinj.be\">android-dev@robinj.be</a>"))
			.append (" with the contents of this dialog so I can get this problem fixed.\n\n")
			.append ("Type: ")
			.append (this.ex.getClass ().getSimpleName ())
			.append ("\n")
			.append ("Message: ")
			.append (this.ex.getMessage ())
			.append ("\n\n")
			.append ("Stack trace:\n")
			.append (stackTrace);

		if (context != null)
		{
			try
			{
				AlertDialog.Builder dlg = new AlertDialog.Builder (context);
				dlg.setTitle ("(╯°□°）╯︵ ┻━┻");
				dlg.setMessage (message.toString ());
				dlg.setCancelable (true);
				dlg.setNeutralButton ("OK", null);

				dlg.show ();
			}
			catch (Exception ex2)
			{
				Log.getInstance ().e (this.getClass ().getSimpleName (), "Couldn't show AlertDialog");
			}
		}
		else
		{
			Log.getInstance ().w (this.getClass ().getSimpleName (), "User wasn't notified that there was a problem because context == NULL");
		}

		this.track();
	}

	public void logAndTrack() {
		this.log();
		this.track();
	}

	private void track() {
		this.track(this.ex);
	}

	private void track(final Throwable ex) {
		try {
			ACRA.getErrorReporter().handleSilentException(ex);
		} catch (Exception ex2) {
			Log.getInstance ().w (this.getClass ().getSimpleName (), "Problem description couldn't be sent: " + ex2.getMessage ());
		}
	}

	private String getStackTrace() {
		return this.getStackTrace(this.ex);
	}

	private String getStackTrace(final Throwable ex) {
		final StringBuilder stackTrace = new StringBuilder ();

		for (final StackTraceElement ste : ex.getStackTrace ()) {
			stackTrace.append(ste.toString())
				.append("\n");
		}

		return stackTrace.toString();
	}

	public void log() {
		this.log(this.ex);
	}

	public void log(final Throwable ex) {
		this.log(this.getStackTrace(ex));
	}

	private void log(final String stackTrace) {
		Log.getInstance ().e (this.getClass ().getSimpleName (), this.ex.getClass ().getName () + "\n\n" + this.ex.getMessage () + "\n\n" + stackTrace.toString ());
	}
}

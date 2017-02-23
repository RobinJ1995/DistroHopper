package be.robinj.distrohopper;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Html;

import java.util.Map;

import be.robinj.distrohopper.dev.Log;

/**
 * Created by robin on 8/22/14.
 */
public class ExceptionHandler
{
	private Context context;
	private Exception ex;

	public ExceptionHandler (Context context, Exception ex)
	{
		this.context = context;
		this.ex = ex;
	}

	public void show ()
	{
		StringBuilder message = new StringBuilder ();
		StringBuilder stackTrace = new StringBuilder ();

		for (StackTraceElement ste : this.ex.getStackTrace ())
			stackTrace.append (ste.toString ())
				.append ("\n");

		this.logException (stackTrace.toString ());

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
			.append (stackTrace.toString ());

		if (this.context != null)
		{
			try
			{
				AlertDialog.Builder dlg = new AlertDialog.Builder (this.context);
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

		try
		{
			Tracker.trackException (ex, stackTrace);
		}
		catch (Exception ex2)
		{
			Log.getInstance ().w (this.getClass ().getSimpleName (), "Problem description couldn't be sent: " + ex2.getMessage ());
		}
	}

	private void logException (String stackTrace)
	{
		Log.getInstance ().e (this.getClass ().getSimpleName (), this.ex.getClass ().getName () + "\n\n" + this.ex.getMessage () + "\n\n" + stackTrace.toString ());
	}
}

package be.robinj.ubuntu;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Html;

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

		message.append ("Oops! Something went wrong!\n")
			.append ("If this happens a lot, then please send an e-mail to ")
			.append (Html.fromHtml ("<a href=\"mailto:android-dev@robinj.be\">android-dev@robinj.be</a>"))
			.append (" with the contents of this dialog so I can get this problem fixed.\n\n")
			.append ("Type: ")
			.append (ex.getClass ().getSimpleName ())
			.append ("\n")
			.append ("Message: ")
			.append (ex.getMessage ())
			.append ("\n\n")
			.append ("Stack trace:\n")
			.append (stackTrace.toString ());

		AlertDialog.Builder dlg = new AlertDialog.Builder (this.context);
		dlg.setTitle ("(╯°□°）╯︵ ┻━┻");
		dlg.setMessage (message.toString ());
		dlg.setCancelable (true);
		dlg.setNeutralButton ("OK", null);

		dlg.show ();
	}
}

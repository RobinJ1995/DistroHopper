package be.robinj.ubuntu;

/**
 * Created by robin on 7/31/14.
 */
public class AsyncTask
{
	private final Runnable runnable;
	private String jsFunction;
	private Exception exception;

	public AsyncTask (Runnable runnable)
	{
		this.runnable = runnable;
	}

	public AsyncTask (Runnable runnable, String jsFunction)
	{
		this.runnable = runnable;
		this.jsFunction = jsFunction;
	}

	public void start ()
	{
		final AsyncTask me = this;

		Runnable runnable2 = new Runnable ()
		{
			@Override
			public void run ()
			{
				try
				{
					runnable.run ();
				}
				catch (Exception ex)
				{
					me.setException (ex);
				}

				me.done ();
			}
		};
		Thread thread = new Thread (runnable2);
		thread.start ();
	}

	public void done ()
	{
		final AsyncTask me = this;
		final JsInterface jsInterface = MainActivity.getJsInterface ();

		jsInterface.getParentActivity ().runOnUiThread
		(
			new Runnable ()
			{
				@Override
				public void run ()
				{
					if (me.exception == null)
					{
						if (me.jsFunction != null)
							jsInterface.runJs (me.jsFunction);
					}
					else
					{
						StringBuilder message = new StringBuilder (me.exception.getClass ().getSimpleName ());
						if (me.exception.getLocalizedMessage () != null)
							message.append (": ").append (me.exception.getLocalizedMessage ().replace ("\"", "\""));
						else if (me.exception.getMessage () != null)
							message.append (": ").append (me.exception.getMessage ().replace ("\"", "\""));

						jsInterface.runJs ("handleException (\"" + message + "\");");
					}
				}
			}
		);
	}

	public Exception getException ()
	{
		return exception;
	}

	public void setException (Exception exception)
	{
		this.exception = exception;
	}
}

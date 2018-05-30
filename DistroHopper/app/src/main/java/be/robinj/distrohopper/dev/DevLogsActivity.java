package be.robinj.distrohopper.dev;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import be.robinj.distrohopper.IObserver;
import be.robinj.distrohopper.R;

public class DevLogsActivity extends Activity implements IObserver
{
	private Log log;
	private TextView tvLogs;

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_dev_logs);

		this.log = Log.getInstance ();

		this.tvLogs = this.findViewById (R.id.tvLogs);
		this.tvLogs.setText (this.log.getLog ());

		this.log.attachObserver (this);
	}

	@Override
	protected void onStop ()
	{
		this.log.detachObserver (this);

		super.onStop ();
	}

	@Override
	public boolean onCreateOptionsMenu (Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater ().inflate (R.menu.menu_dev_logs, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected (MenuItem item)
	{
		return super.onOptionsItemSelected (item);
	}

	@Override
	public void nudge ()
	{
		this.tvLogs.setText (Log.getInstance ().getLog ());
	}
}

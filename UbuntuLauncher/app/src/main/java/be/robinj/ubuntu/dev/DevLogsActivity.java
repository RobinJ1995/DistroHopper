package be.robinj.ubuntu.dev;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import be.robinj.ubuntu.R;

public class DevLogsActivity extends Activity
{

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.setTheme (R.style.PreferencesTheme);
		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_dev_logs);

		TextView tvLogs = (TextView) this.findViewById (R.id.tvLogs);
		tvLogs.setText (Log.getLog ());
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
}

package be.robinj.ubuntu;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;


public class ContributeActivity extends Activity
{

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.setTheme (R.style.DialogTheme);

		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_contribute);
	}


	@Override
	public boolean onCreateOptionsMenu (Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater ().inflate (R.menu.contribute, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected (MenuItem item)
	{
		return super.onOptionsItemSelected (item);
	}

	private void openUrl (String url)
	{
		Intent intent = new Intent (Intent.ACTION_VIEW);
		intent.setData (Uri.parse (url));

		this.startActivity (intent);
	}

	//# Event handlers #//
	public void btnTranslate_clicked (View view)
	{
		try
		{
			this.openUrl ("https://www.transifex.com/projects/p/ubuntu-launcher/");

			Tracker tracker = Application.getTracker ();
			tracker.send (new HitBuilders.EventBuilder ()
					.setCategory ("Contribute")
					.setAction ("Transifex")
					.setLabel ("click")
					.build ()
			);
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this, ex);
			exh.show ();
		}
	}

	public void btnBugs_clicked (View view)
	{
		try
		{
			this.openUrl ("https://github.com/RobinJ1995/be.robinj.ubuntu/issues");

			Tracker tracker = Application.getTracker ();
			tracker.send (new HitBuilders.EventBuilder ()
					.setCategory ("Contribute")
					.setAction ("GitHub")
					.setLabel ("click")
					.build ()
			);
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this, ex);
			exh.show ();
		}
	}

	public void btnDonate_clicked (View view)
	{
		try
		{
			this.openUrl ("https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=broederjacobs%40gmail%2ecom&lc=BE&item_name=Ubuntu%20Launcher&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donate_LG%2egif%3aNonHosted");

			Tracker tracker = Application.getTracker ();
			tracker.send (new HitBuilders.EventBuilder ()
					.setCategory ("Contribute")
					.setAction ("Donate")
					.setLabel ("click")
					.build ()
			);
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this, ex);
			exh.show ();
		}
	}
}

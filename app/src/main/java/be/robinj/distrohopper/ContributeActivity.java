package be.robinj.distrohopper;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class ContributeActivity extends AppCompatActivity
{
	private static final String[] IAP_CATALOG = new String[] {"be.robinj.distrohopper.donation.e1", "be.robinj.distrohopper.donation.e2", "be.robinj.distrohopper.donation.e3", "be.robinj.distrohopper.donation.e4", "be.robinj.distrohopper.donation.e5", "be.robinj.distrohopper.donation.e10", "be.robinj.distrohopper.donation.e20"};
	private static final String[] IAP_CATALOG_VALUES = new String[] {"€1", "€2", "€3", "€4", "€5", "€10", "€20"};
	
	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_contribute);
	}


	@Override
	public boolean onCreateOptionsMenu (Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		this.getMenuInflater ().inflate (R.menu.contribute, menu);
		
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
			this.openUrl ("https://www.transifex.com/projects/p/distrohopper/");
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this);
		}
	}

	public void btnBugs_clicked (View view)
	{
		try
		{
			this.openUrl ("https://github.com/RobinJ1995/DistroHopper/issues");
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this);
		}
	}

	public void btnDonate_clicked (View view)
	{
		try
		{
			this.openUrl ("https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=broederjacobs%40gmail%2ecom&lc=BE&item_name=Ubuntu%20Launcher&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donate_LG%2egif%3aNonHosted");
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this);
		}
	}
	
	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult (requestCode, resultCode, data);
		
		FragmentManager fragmentManager = this.getSupportFragmentManager();
		Fragment fragment = fragmentManager.findFragmentByTag ("donationsFragment");
		if (fragment != null)
			fragment.onActivityResult (requestCode, resultCode, data);
	}
}

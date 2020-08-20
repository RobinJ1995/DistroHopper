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

import org.sufficientlysecure.donations.DonationsFragment;


public class ContributeActivity extends AppCompatActivity
{
	private static final String[] IAP_CATALOG = new String[] {"be.robinj.distrohopper.donation.e1", "be.robinj.distrohopper.donation.e2", "be.robinj.distrohopper.donation.e3", "be.robinj.distrohopper.donation.e4", "be.robinj.distrohopper.donation.e5", "be.robinj.distrohopper.donation.e10", "be.robinj.distrohopper.donation.e20"};
	private static final String[] IAP_CATALOG_VALUES = new String[] {"€1", "€2", "€3", "€4", "€5", "€10", "€20"};
	
	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_contribute);
		
		FragmentTransaction fragmentTransaction = this.getSupportFragmentManager ().beginTransaction ();
		// I know I'm supposed to keep my key hidden but... come on. This app is open source, and donations don't unlock any additional features. If they want to circumvent it they can easily do so, and they would gain absolutely nothing from it anyway. //
		DonationsFragment donationsFragment = DonationsFragment.newInstance (false, true, "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAogL8ClXWCd9fMZrDISjCN0Dtzv5E06CAZpRihm2QH6vMP64mCMkTPUb/0ti4kIwyO3OVO7uJYLgdmHnGLyjuACknRrlVD94IzQLlwRMtSyhZClhLEsaKJEUl4CM2l6ZKgdxBJNYFnRRWcnYCo5n5e5UagPcXirPPXidsxj3OYe6bLHXP27uECB6h6yeq2XU4Rs9VejgC+5BYyPB5N7xbsVBMD8k2ym8EO1qGzJoXlkUS9pS5To5pO7/1tUZRAw0eXtNoO4p/LjF8HkLuN0GhnqL3cjjxIy2S/rC+3ypWDqo1ndoLRbYRMbPTxkbZ0a8MIXW36yXw/iSZfospi5/d/QIDAQAB", IAP_CATALOG, IAP_CATALOG_VALUES, false, null, null, null, false, null, null, false, null);
		fragmentTransaction.replace (R.id.flDonate, donationsFragment, "donationsFragment");
		fragmentTransaction.commit ();
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

package be.robinj.distrohopper;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import be.robinj.distrohopper.R;


public class AboutActivity extends Activity
{
	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.setTheme (R.style.DialogTheme);

		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_about);

		try
		{
			PackageInfo pkgInfo = this.getPackageManager ().getPackageInfo (this.getPackageName (), 0);

			TextView tvDevUrl = (TextView) this.findViewById (R.id.tvDevUrl);
			TextView tvDevEmail = (TextView) this.findViewById (R.id.tvDevEmail);
			TextView tvVersion = (TextView) this.findViewById (R.id.tvVersion);
			TextView tvTranslators = (TextView) this.findViewById (R.id.tvTranslators);

			tvDevUrl.setText (Html.fromHtml ("<a href=\"http://robinj.be/\">RobinJ.be</a>"));
			tvDevEmail.setText (Html.fromHtml ("<a href=\"mailto:android-dev@robinj.be\">android-dev@robinj.be</a>"));
			tvVersion.setText ("v" + pkgInfo.versionName);
			tvTranslators.setText (Html.fromHtml (this.getString (R.string.translators, TextView.BufferType.SPANNABLE).replace ("\n", "<br />")));

			tvDevUrl.setMovementMethod (LinkMovementMethod.getInstance ());
			tvDevEmail.setMovementMethod (LinkMovementMethod.getInstance ());
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this, ex);
			exh.show ();
		}
	}


	@Override
	public boolean onCreateOptionsMenu (Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater ().inflate (R.menu.about, menu);
		return true;
	}



	@Override
	protected void onStart ()
	{
		super.onStart ();
	}

	@Override
	protected void onStop ()
	{
		super.onStop ();
	}

	@Override
	public boolean onOptionsItemSelected (MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId ();

		if (id == R.id.menuContribute)
		{
			Intent intent = new Intent (this, ContributeActivity.class);
			this.startActivity (intent);
		}

		return super.onOptionsItemSelected (item);
	}
}

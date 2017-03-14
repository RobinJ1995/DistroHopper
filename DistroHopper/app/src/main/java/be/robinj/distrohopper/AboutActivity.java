package be.robinj.distrohopper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import be.robinj.distrohopper.dev.Log;


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
			ImageView ivLogo = (ImageView) this.findViewById (R.id.ivLogo);

			tvDevUrl.setText (Html.fromHtml ("<a href=\"http://robinj.be/\">RobinJ.be</a>"));
			tvDevEmail.setText (Html.fromHtml ("<a href=\"mailto:android-dev@robinj.be\">android-dev@robinj.be</a>"));
			tvVersion.setText ("v" + pkgInfo.versionName);

			tvDevUrl.setMovementMethod (LinkMovementMethod.getInstance ());
			tvDevEmail.setMovementMethod (LinkMovementMethod.getInstance ());
			
			final Context context = this.getBaseContext ();
			
			ivLogo.setOnClickListener
			(
				new View.OnClickListener ()
				{
					private short clicked = 0;
					private MediaPlayer player;
					
					@Override
					public void onClick (View view)
					{
						if (++this.clicked % 3 == 0)
						{
							if (this.player == null)
								this.player = MediaPlayer.create (context, R.raw.ubuntu);
							else
								this.player.seekTo (0);
							
							player.start ();
						}
					}
				}
			);
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

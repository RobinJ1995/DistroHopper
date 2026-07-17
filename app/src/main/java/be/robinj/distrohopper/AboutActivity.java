package be.robinj.distrohopper;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;


public class AboutActivity extends AppCompatActivity
{
	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_about);
		InsetsHelper.applySystemBarsPadding (this);

		try
		{
			PackageInfo pkgInfo = this.getPackageManager ().getPackageInfo (this.getPackageName (), 0);

			TextView tvDevUrl = (TextView) this.findViewById (R.id.tvDevUrl);
			TextView tvDevEmail = (TextView) this.findViewById (R.id.tvDevEmail);
			TextView tvVersion = (TextView) this.findViewById (R.id.tvVersion);
			ImageView ivLogo = (ImageView) this.findViewById (R.id.ivLogo);

			tvDevUrl.setText (Html.fromHtml ("<a href=\"http://robinj.be/\">RobinJ.be</a>", Html.FROM_HTML_MODE_LEGACY));
			tvDevEmail.setText (Html.fromHtml ("<a href=\"mailto:distrohopper@robinj.be\">distrohopper@robinj.be</a>", Html.FROM_HTML_MODE_LEGACY));
			tvVersion.setText ("v" + pkgInfo.versionName);

			tvDevUrl.setMovementMethod (LinkMovementMethod.getInstance ());
			tvDevEmail.setMovementMethod (LinkMovementMethod.getInstance ());

			View linkGithub = this.findViewById (R.id.linkGithub);
			View linkTransifex = this.findViewById (R.id.linkTransifex);

			linkGithub.setOnClickListener
			(
				new View.OnClickListener ()
				{
					@Override
					public void onClick (View view)
					{
						AboutActivity.this.openUrl (AboutActivity.this.getString (R.string.about_github_url));
					}
				}
			);

			linkTransifex.setOnClickListener
			(
				new View.OnClickListener ()
				{
					@Override
					public void onClick (View view)
					{
						AboutActivity.this.openUrl (AboutActivity.this.getString (R.string.about_transifex_url));
					}
				}
			);

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
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this);
		}
	}


	private void openUrl (String url)
	{
		try
		{
			this.startActivity (new Intent (Intent.ACTION_VIEW, Uri.parse (url)));
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this);
		}
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
}

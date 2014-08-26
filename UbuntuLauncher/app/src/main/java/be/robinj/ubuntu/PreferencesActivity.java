package be.robinj.ubuntu;

import android.app.Activity;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import com.commonsware.cwac.colormixer.ColorMixer;
import com.google.analytics.tracking.android.EasyTracker;


public class PreferencesActivity extends Activity
{
	private SharedPreferences prefs;

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_preferences);

		try
		{
			SeekBar sbLauncherIcon_width = (SeekBar) this.findViewById (R.id.sbLauncherIcon_width);
			SeekBar sbLauncherIcon_opacity = (SeekBar) this.findViewById (R.id.sbLauncherIcon_opacity);
			CompoundButton swPanel_show = (CompoundButton) this.findViewById (R.id.swPanel_show);
			SeekBar sbPanel_opacity = (SeekBar) this.findViewById (R.id.sbPanel_opacity);
			CompoundButton swUnityBackground_dynamic = (CompoundButton) this.findViewById (R.id.swUnityBackground_dynamic);
			ColorMixer cmUnityBackground_colour = (ColorMixer) this.findViewById (R.id.cmUnityBackground_colour);
			SeekBar sbUnityBackground_opacity = (SeekBar) this.findViewById (R.id.sbUnityBackgrond_opacity);
			CompoundButton swColourCalc_advanced  = (CompoundButton) this.findViewById (R.id.swColourCalc_advanced);
			CompoundButton swColourCalc_hsv  = (CompoundButton) this.findViewById (R.id.swColourCalc_hsv);

			this.prefs = this.getSharedPreferences ("prefs", MODE_PRIVATE);
			sbLauncherIcon_width.setProgress (this.prefs.getInt ((String) sbLauncherIcon_width.getTag (), 36));
			sbLauncherIcon_opacity.setProgress (this.prefs.getInt ((String) sbLauncherIcon_opacity.getTag (), 204));
			swPanel_show.setChecked (this.prefs.getBoolean ((String) swPanel_show.getTag (), true));
			sbPanel_opacity.setProgress (this.prefs.getInt ((String) sbPanel_opacity.getTag (), 100));
			swUnityBackground_dynamic.setChecked (this.prefs.getBoolean ((String) swUnityBackground_dynamic.getTag (), true));
			cmUnityBackground_colour.setColor (this.prefs.getInt ((String) cmUnityBackground_colour.getTag (), Color.WHITE));
			sbUnityBackground_opacity.setProgress (this.prefs.getInt ((String) sbUnityBackground_opacity.getTag (), 50));
			swColourCalc_advanced.setChecked (this.prefs.getBoolean ((String) swColourCalc_advanced.getTag (), true));
			swColourCalc_hsv.setChecked (this.prefs.getBoolean ((String) swColourCalc_hsv.getTag (), true));

			SeekBarChangeListener seekBarChangeListener = new SeekBarChangeListener ();
			CheckedChangeListener checkedChangeListener = new CheckedChangeListener ();

			sbLauncherIcon_width.setOnSeekBarChangeListener (seekBarChangeListener);
			sbLauncherIcon_opacity.setOnSeekBarChangeListener (seekBarChangeListener);
			swPanel_show.setOnCheckedChangeListener (checkedChangeListener);
			sbPanel_opacity.setOnSeekBarChangeListener (seekBarChangeListener);
			swUnityBackground_dynamic.setOnCheckedChangeListener (checkedChangeListener);
			cmUnityBackground_colour.setOnColorChangedListener (new ColorChangeListener (cmUnityBackground_colour));
			sbUnityBackground_opacity.setOnSeekBarChangeListener (seekBarChangeListener);
			swColourCalc_advanced.setOnCheckedChangeListener (checkedChangeListener);
			swColourCalc_hsv.setOnCheckedChangeListener (checkedChangeListener);

			this.unityBackground_dynamic_changed (swUnityBackground_dynamic.isChecked ());
			this.panel_show_changed (swPanel_show.isChecked ());
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
		getMenuInflater ().inflate (R.menu.preferences, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected (MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId ();
		/*if (id == R.id.action_settings)
		{
			return true;
		}*/
		return super.onOptionsItemSelected (item);
	}

	@Override
	protected void onStart ()
	{
		super.onStart();

		EasyTracker.getInstance (this).activityStart (this);
	}

	@Override
	protected void onStop ()
	{
		super.onStop();

		EasyTracker.getInstance (this).activityStop (this);
	}

	@Override
	public void onResume ()
	{
		try
		{
			super.onResume ();

			WallpaperManager wpman = WallpaperManager.getInstance (this.getApplicationContext ());

			WallpaperInfo info = wpman.getWallpaperInfo ();
			boolean liveWallpaper = (info != null);

			LinearLayout llWallpaper = (LinearLayout) this.findViewById (R.id.llWallpaper);
			if (liveWallpaper)
				llWallpaper.setBackgroundResource (android.R.drawable.btn_default);
			else
				llWallpaper.setBackgroundDrawable (wpman.getFastDrawable ());
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this, ex);
			exh.show ();
		}
	}

	public void btnBack_clicked (View view)
	{
		try
		{
			this.onBackPressed ();
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this, ex);
			exh.show ();
		}
	}

	public void btnAbout_clicked (View view)
	{
		try
		{
			Intent intent = new Intent (this, AboutActivity.class);
			this.startActivity (intent);
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this, ex);
			exh.show ();
		}
	}

	public void btnWallpaper_clicked (View view)
	{
		try
		{
			Intent intent = new Intent (Intent.ACTION_SET_WALLPAPER);
			this.startActivity (Intent.createChooser (intent, this.getResources ().getString (R.string.option_wallpaper)));
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this, ex);
			exh.show ();
		}
	}

	private void unityBackground_dynamic_changed (boolean enabled)
	{
		LinearLayout llUnityBackground_colour = (LinearLayout) this.findViewById (R.id.llUnityBackground_colour);
		llUnityBackground_colour.setVisibility (enabled ? View.GONE : View.VISIBLE);
	}

	private void panel_show_changed (boolean enabled)
	{
		LinearLayout llPanel_opacity = (LinearLayout) this.findViewById (R.id.llPanel_opacity);
		llPanel_opacity.setVisibility (enabled && Build.VERSION.SDK_INT >= 11 ? View.VISIBLE : View.GONE);
	}

	private class SeekBarChangeListener implements SeekBar.OnSeekBarChangeListener
	{
		@Override
		public void onProgressChanged (SeekBar seekBar, int progress, boolean fromUser)
		{
			try
			{
				if (fromUser)
				{
					SharedPreferences.Editor editor = PreferencesActivity.this.prefs.edit ();
					editor.putInt ((String) seekBar.getTag (), progress);

					if (Build.VERSION.SDK_INT >= 9)
						editor.apply ();
					else
						editor.commit ();
				}
			}
			catch (Exception ex)
			{
				ExceptionHandler exh = new ExceptionHandler (PreferencesActivity.this, ex);
				exh.show ();
			}
		}

		@Override
		public void onStartTrackingTouch (SeekBar seekBar)
		{
		}

		@Override
		public void onStopTrackingTouch (SeekBar seekBar)
		{
		}
	}

	private class CheckedChangeListener implements CompoundButton.OnCheckedChangeListener
	{
		@Override
		public void onCheckedChanged (CompoundButton buttonView, boolean isChecked)
		{
			try
			{
				SharedPreferences.Editor editor = PreferencesActivity.this.prefs.edit ();
				String property = (String) buttonView.getTag ();
				editor.putBoolean (property, isChecked);

				if ("unitybackground_dynamic".equals (property))
					PreferencesActivity.this.unityBackground_dynamic_changed (isChecked);
				else if ("panel_show".equals (property))
					PreferencesActivity.this.panel_show_changed (isChecked);

				if (Build.VERSION.SDK_INT >= 9)
					editor.apply ();
				else
					editor.commit ();
			}
			catch (Exception ex)
			{
				ExceptionHandler exh = new ExceptionHandler (PreferencesActivity.this, ex);
				exh.show ();
			}
		}
	}

	private class ColorChangeListener implements ColorMixer.OnColorChangedListener
	{
		private ColorMixer colorMixer;

		public ColorChangeListener (ColorMixer colorMixer)
		{
			this.colorMixer = colorMixer;
		}

		@Override
		public void onColorChange (int argb)
		{
			SharedPreferences.Editor editor = PreferencesActivity.this.prefs.edit ();
			editor.putInt ((String) this.colorMixer.getTag (), argb);

			if (Build.VERSION.SDK_INT >= 9)
				editor.apply ();
			else
				editor.commit ();
		}
	}
}

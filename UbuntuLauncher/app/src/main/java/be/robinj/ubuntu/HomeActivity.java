package be.robinj.ubuntu;

import android.app.Activity;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.google.analytics.tracking.android.EasyTracker;

import java.util.ArrayList;
import java.util.List;

import be.robinj.ubuntu.thirdparty.ExpandableHeightGridView;
import be.robinj.ubuntu.unity.Wallpaper;
import be.robinj.ubuntu.unity.WidgetHost;
import be.robinj.ubuntu.unity.WidgetHostView;
import be.robinj.ubuntu.unity.WidgetHostView_LongClickListener;
import be.robinj.ubuntu.unity.WidgetHost_LongClickListener;
import be.robinj.ubuntu.unity.dash.SearchTextWatcher;


public class HomeActivity extends Activity
{
	private AppManager apps;
	private AppWidgetManager widgetManager;
	private WidgetHost widgetHost;

	private int chameleonicBgColour = Color.argb (25, 0, 0, 0);

	private AsyncInitWallpaper asyncInitWallpaper;
	private AsyncLoadApps asyncLoadApps;

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		try
		{
			super.onCreate (savedInstanceState);
			setContentView (R.layout.activity_home);

			ExpandableHeightGridView gvDashHomeApps = (ExpandableHeightGridView) this.findViewById (R.id.gvDashHomeApps);
			be.robinj.ubuntu.unity.launcher.SpinnerAppLauncher lalSpinner = (be.robinj.ubuntu.unity.launcher.SpinnerAppLauncher) this.findViewById (R.id.lalSpinner);
			be.robinj.ubuntu.unity.launcher.AppLauncher lalBfb = (be.robinj.ubuntu.unity.launcher.AppLauncher) this.findViewById (R.id.lalBfb);
			be.robinj.ubuntu.unity.launcher.AppLauncher lalPreferences = (be.robinj.ubuntu.unity.launcher.AppLauncher) this.findViewById (R.id.lalPreferences);
			LinearLayout llLauncherPinnedApps = (LinearLayout) this.findViewById (R.id.llLauncherPinnedApps);
			Wallpaper wpWallpaper = (Wallpaper) this.findViewById (R.id.wpWallpaper);
			LinearLayout llPanel = (LinearLayout) this.findViewById (R.id.llPanel);
			ImageButton ibPanelDashClose = (ImageButton) this.findViewById (R.id.ibPanelDashClose);
			//GridLayout glWidgets = (GridLayout) this.findViewById (R.id.glWidgets);

			lalBfb.init ();
			lalSpinner.init ();
			lalPreferences.init ();

			SharedPreferences prefs = this.getSharedPreferences ("prefs", MODE_PRIVATE);
			float density = this.getResources ().getDisplayMetrics ().density;

			if (prefs.getBoolean ("panel_show", true))
			{
				if (Build.VERSION.SDK_INT >= 11)
					llPanel.setAlpha ((float) prefs.getInt ("panel_opacity", 100) / 100F);
			}
			else
			{
				llPanel.setVisibility (View.GONE);
			}

			int ibDashClose_width = (int) ((float) (48 + prefs.getInt ("launchericon_width", 36)) * density);
			LinearLayout.LayoutParams ibDashClose_layoutParams = new LinearLayout.LayoutParams (ibDashClose_width, LinearLayout.LayoutParams.MATCH_PARENT);
			ibPanelDashClose.setLayoutParams (ibDashClose_layoutParams);

			lalSpinner.getProgressWheel ().spin ();

			this.asyncInitWallpaper = new AsyncInitWallpaper (this);
			this.asyncInitWallpaper.execute (wpWallpaper);

			this.asyncLoadApps = new AsyncLoadApps (this, lalSpinner, lalBfb, gvDashHomeApps, llLauncherPinnedApps);
			this.asyncLoadApps.execute (this.getApplicationContext ());

			//this.widgetManager = AppWidgetManager.getInstance (this);
			//this.widgetHost = new WidgetHost (this, R.id.vgWidgets);

			//glWidgets.setOnLongClickListener (new WidgetHost_LongClickListener (this));
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
		getMenuInflater ().inflate (R.menu.home, menu);
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
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		try
		{
			super.onActivityResult (requestCode, resultCode, data);

			if (requestCode == 1) // ActivityPreferences //
				this.onCreate (null); // Reload activity //
			else if (requestCode == 2) // Widget picked //
				if (resultCode == RESULT_OK)
					this.configureWidget (data);
				else
					this.removeWidget (data);
			else if (requestCode == 3) // Widget configured //
				if (resultCode == RESULT_OK)
					this.createWidget (data);
				else
					this.removeWidget (data);
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this, ex);
			exh.show ();
		}
	}

	@Override
	public void onBackPressed ()
	{
		try
		{
			LinearLayout llDash = (LinearLayout) this.findViewById (R.id.llDash);

			if (llDash.getVisibility () == View.VISIBLE)
				this.closeDash ();
			else if (! this.isDefaultLauncher ())
				super.onBackPressed ();
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this, ex);
			exh.show ();
		}
	}

	@Override
	protected void onStart ()
	{
		super.onStart();

		//this.widgetHost.startListening ();

		EasyTracker.getInstance (this).activityStart (this);
	}

	@Override
	protected void onStop ()
	{
		super.onStop();

		//this.widgetHost.stopListening ();

		EasyTracker.getInstance (this).activityStop (this);
	}

	//# Callbacks #//
	public void asyncLoadInstalledAppsDone (AppManager installedApps)
	{
		try
		{
			this.apps = installedApps;

			EditText etDashSearch = (EditText) this.findViewById (R.id.etDashSearch);
			etDashSearch.addTextChangedListener (new SearchTextWatcher (installedApps));
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this, ex);
			exh.show ();
		}
	}

	public void asyncInitWallpaperDone (Wallpaper wpWallpaper)
	{
		try
		{
			SharedPreferences prefs = this.getSharedPreferences ("prefs", MODE_PRIVATE);

			int colour;
			int colour_opacity = prefs.getInt ("launchericon_opacity", 204);
			int bgColour;
			int bgColour_opacity = prefs.getInt ("unitybackground_opacity", 50);

			if (prefs.getBoolean ("unitybackground_dynamic", true))
			{
				if (wpWallpaper.isLiveWallpaper ())
				{
					colour = Color.argb (40, 40, 40, 40);
					bgColour = Color.argb (bgColour_opacity, 40, 40, 40);
				}
				else
				{
					colour = wpWallpaper.getAverageColour (colour_opacity);
					bgColour = wpWallpaper.getAverageColour (bgColour_opacity);
				}
			}
			else
			{
				int col = prefs.getInt ("unitybackground_colour", Color.WHITE);

				int r = Color.red (col);
				int g = Color.green (col);
				int b = Color.blue (col);

				colour = Color.argb (colour_opacity, r, g, b);
				bgColour = Color.argb (bgColour_opacity, r, g, b);
			}

			be.robinj.ubuntu.unity.launcher.AppLauncher lalBfb = (be.robinj.ubuntu.unity.launcher.AppLauncher) this.findViewById (R.id.lalBfb);
			be.robinj.ubuntu.unity.launcher.AppLauncher lalPreferences = (be.robinj.ubuntu.unity.launcher.AppLauncher) this.findViewById (R.id.lalPreferences);
			be.robinj.ubuntu.unity.launcher.AppLauncher lalSpinner = (be.robinj.ubuntu.unity.launcher.AppLauncher) this.findViewById (R.id.lalSpinner);

			LinearLayout llLauncher = (LinearLayout) this.findViewById (R.id.llLauncher);
			LinearLayout llDash = (LinearLayout) this.findViewById (R.id.llDash);

			lalBfb.setColour (colour);
			lalPreferences.setColour (colour);
			lalSpinner.setColour (colour);

			llLauncher.setBackgroundColor (bgColour);
			llDash.setBackgroundColor (bgColour);

			this.chameleonicBgColour = bgColour;
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this, ex);
			exh.show ();
		}
	}

	//# Event handlers #//
	public void lalBfb_clicked (View view)
	{
		try
		{
			LinearLayout llDash = (LinearLayout) this.findViewById (R.id.llDash);

			if (llDash.getVisibility () == View.VISIBLE)
				this.closeDash ();
			else
				this.openDash ();
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this, ex);
			exh.show ();
		}
	}

	public void ibPanelDashClose_clicked (View view)
	{
		try
		{
			this.closeDash ();
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this, ex);
			exh.show ();
		}
	}

	public void lalPreferences_clicked (View view)
	{
		try
		{
			if (this.asyncInitWallpaper != null)
				this.asyncInitWallpaper.cancel (true);
			if (this.asyncLoadApps != null)
				this.asyncLoadApps.cancel (true);

			Intent intent = new Intent (this, PreferencesActivity.class);
			this.startActivityForResult (intent, 1);
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this, ex);
			exh.show ();
		}
	}

	//# Dash #//
	private void closeDash ()
	{
		LinearLayout llDash = (LinearLayout) this.findViewById (R.id.llDash);
		LinearLayout llPanel = (LinearLayout) this.findViewById (R.id.llPanel);
		ImageButton ibPanelDashClose = (ImageButton) this.findViewById (R.id.ibPanelDashClose);
		Wallpaper wpWallpaper = (Wallpaper) this.findViewById (R.id.wpWallpaper);
		EditText etDashSearch = (EditText) this.findViewById (R.id.etDashSearch);

		llDash.setVisibility (View.GONE);
		llPanel.setBackgroundResource (R.drawable.panel_background);
		ibPanelDashClose.setVisibility (View.INVISIBLE);
		wpWallpaper.unblur ();
		etDashSearch.setText ("");
		etDashSearch.clearFocus ();

		if (Build.VERSION.SDK_INT >= 11)
		{
			SharedPreferences prefs = this.getSharedPreferences ("prefs", MODE_PRIVATE);
			llPanel.setAlpha ((float) prefs.getInt ("panel_opacity", 100) / 100F);
		}

		InputMethodManager imm = (InputMethodManager) this.getSystemService (Context.INPUT_METHOD_SERVICE);
		if (imm != null)
			imm.hideSoftInputFromWindow (this.getWindow ().getDecorView ().getRootView ().getWindowToken (), 0);
	}

	private void openDash ()
	{
		LinearLayout llDash = (LinearLayout) this.findViewById (R.id.llDash);
		LinearLayout llPanel = (LinearLayout) this.findViewById (R.id.llPanel);
		ImageButton ibPanelDashClose = (ImageButton) this.findViewById (R.id.ibPanelDashClose);
		Wallpaper wpWallpaper = (Wallpaper) this.findViewById (R.id.wpWallpaper);

		llDash.setVisibility (View.VISIBLE);
		llPanel.setBackgroundColor (this.chameleonicBgColour);
		ibPanelDashClose.setVisibility (View.VISIBLE);
		wpWallpaper.blur ();
		if (Build.VERSION.SDK_INT >= 11)
			llPanel.setAlpha (1F);
	}

	//# Checks #//
	private boolean isDefaultLauncher ()
	{
		String packageName = this.getPackageName ();

		IntentFilter homeFilter = new IntentFilter (Intent.ACTION_MAIN);
		homeFilter.addCategory (Intent.CATEGORY_HOME);

		List<IntentFilter> filters = new ArrayList<IntentFilter> ();
		filters.add (homeFilter);

		List<ComponentName> activities = new ArrayList<ComponentName> ();

		this.getPackageManager ().getPreferredActivities (filters, activities, packageName);

		for (ComponentName activity : activities)
		{
			if (packageName.equals (activity.getPackageName ()))
				return true;
		}

		return false;
	}

	//# Widgets #//
	public void selectWidget ()
	{
		int id = this.widgetHost.allocateAppWidgetId ();

		Intent intent = new Intent (AppWidgetManager.ACTION_APPWIDGET_PICK);
		intent.putExtra (AppWidgetManager.EXTRA_APPWIDGET_ID, id);

		/*
		ArrayList customInfo = new ArrayList();
		pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo);
		ArrayList customExtras = new ArrayList();
		pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras);

		addEmptyData (pickIntent);
		*/

		this.startActivityForResult (intent, 2);
	}

	private void configureWidget (Intent data) throws Exception
	{
		Bundle bundle = data.getExtras ();
		int id = bundle.getInt (AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

		AppWidgetProviderInfo info = this.widgetManager.getAppWidgetInfo (id);

		if (id == -1)
			throw new Exception ("Didn't receive a widget ID");

		if (info.configure == null)
		{
			this.createWidget (data);
		}
		else
		{
			Intent intent = new Intent (AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
			intent.setComponent (info.configure);
			intent.putExtra (AppWidgetManager.EXTRA_APPWIDGET_ID, id);

			this.startActivityForResult (intent, 3);
		}
	}

	private void createWidget (Intent data) throws Exception
	{
		Bundle bundle = data.getExtras ();
		int id = bundle.getInt (AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

		if (id == -1)
			throw new Exception ("Didn't receive a widget ID");

		AppWidgetProviderInfo info = this.widgetManager.getAppWidgetInfo (id);
		WidgetHostView hostView = (WidgetHostView) this.widgetHost.createView (this, id, info);

		ViewGroup vgWidgets = (GridLayout) this.findViewById (R.id.vgWidgets);
		vgWidgets.addView (hostView);

		hostView.setOnLongClickListener (new WidgetHostView_LongClickListener (this));
	}

	public void removeWidget (AppWidgetHostView hostView)
	{
		this.widgetHost.deleteAppWidgetId (hostView.getAppWidgetId ());

		GridLayout vgWidgets = (GridLayout) this.findViewById (R.id.vgWidgets);
		vgWidgets.removeView (hostView);
	}

	private void removeWidget (Intent data) throws Exception
	{
		if (data != null)
		{
			Bundle bundle = data.getExtras ();
			int id = bundle.getInt (AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

			if (id == -1)
				throw new Exception ("Didn't receive a widget ID");

			this.widgetHost.deleteAppWidgetId (id);
		}
	}
}

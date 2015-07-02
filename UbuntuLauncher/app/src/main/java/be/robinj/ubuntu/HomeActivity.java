package be.robinj.ubuntu;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import be.robinj.ubuntu.broadcast.PackageManagerBroadcastReceiver;
import be.robinj.ubuntu.preferences.PreferencesActivity;
import be.robinj.ubuntu.theme.Abstract;
import be.robinj.ubuntu.theme.Default;
import be.robinj.ubuntu.theme.Elementary;
import be.robinj.ubuntu.theme.Theme;
import be.robinj.ubuntu.thirdparty.ProgressWheel;
import be.robinj.ubuntu.unity.Wallpaper;
import be.robinj.ubuntu.unity.dash.SearchTextWatcher;
import be.robinj.ubuntu.unity.dash.lens.LensManager;
import be.robinj.ubuntu.unity.launcher.AppLauncher;
import be.robinj.ubuntu.unity.launcher.LauncherDragListener;
import be.robinj.ubuntu.unity.launcher.TrashDragListener;
import be.robinj.ubuntu.unity.launcher.service.LauncherService;


public class HomeActivity extends Activity
{
	private LensManager lenses;
	private AppManager apps;
	//private AppWidgetManager widgetManager;
	//private WidgetHost widgetHost;

	private int chameleonicBgColour = Color.argb (25, 0, 0, 0);

	private AsyncInitWallpaper asyncInitWallpaper;
	private AsyncLoadApps asyncLoadApps;

	private boolean openDashWhenReady = false;

	public static Theme theme = new Default ();

	private PackageManagerBroadcastReceiver broadcastPackageManager;

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_home);

		try
		{
			GridView gvDashHomeApps = (GridView) this.findViewById (R.id.gvDashHomeApps);
			LinearLayout llLauncherAndDashContainer = (LinearLayout) this.findViewById (R.id.llLauncherAndDashContainer);
			LinearLayout llLauncher = (LinearLayout) llLauncherAndDashContainer.findViewById (R.id.llLauncher);
			LinearLayout llLauncherAppsContainer = (LinearLayout) llLauncher.findViewById (R.id.llLauncherAppsContainer);
			LinearLayout llLauncherPinnedApps = (LinearLayout) llLauncherAppsContainer.findViewById (R.id.llLauncherPinnedApps);
			LinearLayout llLauncherRunningApps = (LinearLayout) llLauncherAppsContainer.findViewById (R.id.llLauncherRunningApps);
			be.robinj.ubuntu.unity.launcher.SpinnerAppLauncher lalSpinner = (be.robinj.ubuntu.unity.launcher.SpinnerAppLauncher) llLauncher.findViewById (R.id.lalSpinner);
			be.robinj.ubuntu.unity.launcher.AppLauncher lalBfb = (be.robinj.ubuntu.unity.launcher.AppLauncher) llLauncher.findViewById (R.id.lalBfb);
			be.robinj.ubuntu.unity.launcher.AppLauncher lalPreferences = (be.robinj.ubuntu.unity.launcher.AppLauncher) llLauncher.findViewById (R.id.lalPreferences);
			be.robinj.ubuntu.unity.launcher.AppLauncher lalTrash = (be.robinj.ubuntu.unity.launcher.AppLauncher) llLauncher.findViewById (R.id.lalTrash);
			ScrollView scrLauncherAppsContainer = (ScrollView) llLauncher.findViewById (R.id.scrLauncherAppsContainer);
			HorizontalScrollView scrLauncherAppsContainerHorizontal = (HorizontalScrollView) llLauncher.findViewById (R.id.scrLauncherAppsContainerHorizontal);
			LinearLayout llDash = (LinearLayout) llLauncherAndDashContainer.findViewById (R.id.llDash);
			LinearLayout llDashSearchContainer = (LinearLayout) this.findViewById (R.id.llDashSearchContainer);
			ImageView imgDashBackgroundGradient = (ImageView) this.findViewById (R.id.imgDashBackgroundGradient);
			TextView tvDashHomeTitle = (TextView) llDash.findViewById (R.id.tvDashHomeTitle);
			EditText etDashSearch = (EditText) llDash.findViewById (R.id.etDashSearch);
			Wallpaper wpWallpaper = (Wallpaper) this.findViewById (R.id.wpWallpaper);
			LinearLayout llPanel = (LinearLayout) this.findViewById (R.id.llPanel);
			TextView tvPanelBfb = (TextView) llPanel.findViewById (R.id.tvPanelBfb);
			ImageButton ibPanelDashClose = (ImageButton) llPanel.findViewById (R.id.ibPanelDashClose);
			ImageButton ibPanelCog = (ImageButton) llPanel.findViewById (R.id.ibPanelCog);
			RelativeLayout vgWidgets = (RelativeLayout) this.findViewById (R.id.vgWidgets);
			ListView lvDashHomeLensResults = (ListView) this.findViewById (R.id.lvDashHomeLensResults);

			SharedPreferences prefs = this.getSharedPreferences ("prefs", MODE_PRIVATE);
			HashMap<String, Class> themes = new HashMap<String, Class> ();
			themes.put ("default", Default.class);
			themes.put ("elementary", Elementary.class);

			Theme theme = (Theme) themes.get (prefs.getString ("theme", "default")).newInstance ();
			HomeActivity.theme = theme;

			Intent launcherServiceIntent = new Intent (this, LauncherService.class);
			this.stopService (launcherServiceIntent);

			lalBfb.init ();
			lalSpinner.init ();
			lalPreferences.init ();
			lalTrash.init ();
			
			Resources res = this.getResources ();
			float density = res.getDisplayMetrics ().density;

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

			RelativeLayout.LayoutParams vgWidgets_layoutParams = (RelativeLayout.LayoutParams) vgWidgets.getLayoutParams ();
			vgWidgets_layoutParams.setMargins (ibDashClose_width, 0, 0, 0);
			//vgWidgets.setLayoutParams ();

			lalSpinner.getProgressWheel ().spin ();

			if (prefs.getString ("launcher_edge", "left").equals ("right"))
			{
				llLauncherAndDashContainer.setGravity (Gravity.RIGHT);

				llLauncherAndDashContainer.removeView (llLauncher);
				llLauncherAndDashContainer.removeView (llDash);

				llLauncherAndDashContainer.addView (llDash);
				llLauncherAndDashContainer.addView (llLauncher);
			}

			this.asyncInitWallpaper = new AsyncInitWallpaper (this);
			this.asyncInitWallpaper.execute (wpWallpaper);

			this.asyncLoadApps = new AsyncLoadApps (this, lalSpinner, lalBfb, gvDashHomeApps);
			this.asyncLoadApps.execute (this.getApplicationContext ());

			/*
			this.widgetManager = AppWidgetManager.getInstance (this);
			this.widgetHost = new WidgetHost (this, this.widgetManager, R.id.vgWidgets);

			vgWidgets.setOnLongClickListener (new WidgetHost_LongClickListener (this.widgetHost));
			*/

			if (Build.VERSION.SDK_INT >= 11)
			{
				LayoutTransition gvDashHomeApps_transition = new LayoutTransition ();
				gvDashHomeApps_transition.setDuration (180);
				gvDashHomeApps_transition.setStartDelay (LayoutTransition.APPEARING, 0);
				gvDashHomeApps.setLayoutTransition (gvDashHomeApps_transition);

				LayoutTransition lvDashHomeLensResults_transition = new LayoutTransition ();
				lvDashHomeLensResults_transition.setDuration (180);
				lvDashHomeLensResults_transition.setStartDelay (LayoutTransition.APPEARING, 0);
				lvDashHomeLensResults.setLayoutTransition (lvDashHomeLensResults_transition);

				LayoutTransition llLauncherPinnedApps_transition = new LayoutTransition ();
				llLauncherPinnedApps_transition.setStartDelay (LayoutTransition.APPEARING, 0);
				llLauncherPinnedApps.setLayoutTransition (llLauncherPinnedApps_transition);

				LayoutTransition llLauncherRunningApps_transition = new LayoutTransition ();
				llLauncherRunningApps_transition.setStartDelay (LayoutTransition.APPEARING, 0);
				llLauncherRunningApps.setLayoutTransition (llLauncherRunningApps_transition);

				LayoutTransition llDashSearchContainer_transition = new LayoutTransition ();
				llDashSearchContainer_transition.setStartDelay (LayoutTransition.APPEARING, 0);
				llDashSearchContainer_transition.setStartDelay (LayoutTransition.DISAPPEARING, 0);
				llDashSearchContainer_transition.setStartDelay (LayoutTransition.CHANGE_APPEARING, 0);
				llDashSearchContainer_transition.setStartDelay (LayoutTransition.CHANGE_DISAPPEARING, 0);
				llDashSearchContainer_transition.setStartDelay (LayoutTransition.CHANGING, 0);
				llDashSearchContainer.setLayoutTransition (llDashSearchContainer_transition);
			}

			this.openDashWhenReady = prefs.getBoolean ("dash_ready_show", this.openDashWhenReady);

			Intent intent = this.getIntent ();
			if (intent != null)
				this.openDashWhenReady = intent.getBooleanExtra ("openDash", this.openDashWhenReady);

			if (Build.VERSION.SDK_INT >= 19)
			{
				LinearLayout llStatusBar = (LinearLayout) this.findViewById (R.id.llStatusBar);

				int llStatusBar_height = llStatusBar.getHeight ();
				int statusBarHeight_resource = res.getIdentifier ("status_bar_height", "dimen", "android");

				if (statusBarHeight_resource > 0)
					llStatusBar_height = res.getDimensionPixelSize (statusBarHeight_resource);

				RelativeLayout.LayoutParams llStatusBar_layoutParams = new RelativeLayout.LayoutParams (ViewGroup.LayoutParams.MATCH_PARENT, llStatusBar_height);
				llStatusBar.setLayoutParams (llStatusBar_layoutParams);
				llStatusBar.setVisibility (View.VISIBLE);
			}

			// Apply theme //
			llPanel.setBackgroundResource (HomeActivity.theme.panel_background);
			ibPanelCog.setImageResource (HomeActivity.theme.panel_preferences_image);
			ibPanelDashClose.setImageResource (HomeActivity.theme.panel_close_image);
			imgDashBackgroundGradient.setImageResource (HomeActivity.theme.dash_background_gradient);
			lalBfb.setIcon (res.getDrawable (HomeActivity.theme.launcher_bfb_image));
			lalPreferences.setIcon (res.getDrawable (HomeActivity.theme.launcher_preferences_image));

			RelativeLayout.LayoutParams llPanel_layoutParams = (RelativeLayout.LayoutParams) llPanel.getLayoutParams ();
			llPanel_layoutParams.height = (int) res.getDimension (R.dimen.theme_elementary_panel_height);

			switch (res.getInteger (HomeActivity.theme.launcher_location))
			{
				case 2:
					llLauncherAndDashContainer.setOrientation (LinearLayout.VERTICAL);
					llLauncher.setOrientation (LinearLayout.HORIZONTAL);
					llLauncherAppsContainer.setOrientation (LinearLayout.HORIZONTAL);
					llLauncherPinnedApps.setOrientation (LinearLayout.HORIZONTAL);
					llLauncherRunningApps.setOrientation (LinearLayout.HORIZONTAL);

					llLauncherAndDashContainer.setGravity (Gravity.BOTTOM);

					llLauncherAndDashContainer.removeView (llLauncher);
					llLauncherAndDashContainer.removeView (llDash);

					llLauncherAndDashContainer.addView (llDash);
					llLauncherAndDashContainer.addView (llLauncher);

					LinearLayout.LayoutParams llLauncher_layoutParams = new LinearLayout.LayoutParams (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
					llLauncher.setLayoutParams (llLauncher_layoutParams);

					//ScrollView.LayoutParams llLauncherAppsContainer_layoutParams = new ScrollView.LayoutParams (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
					//llLauncherAppsContainer.setLayoutParams (llLauncherAppsContainer_layoutParams);

					scrLauncherAppsContainer.setVisibility (View.GONE);
					scrLauncherAppsContainer.removeView (llLauncherAppsContainer);
					scrLauncherAppsContainerHorizontal.addView (llLauncherAppsContainer);
					scrLauncherAppsContainerHorizontal.setVisibility (View.VISIBLE);

					LinearLayout.LayoutParams llLauncherPinnedApps_layoutParams = new LinearLayout.LayoutParams (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
					llLauncherPinnedApps_layoutParams.gravity = Gravity.LEFT;
					llLauncherPinnedApps.setLayoutParams (llLauncherPinnedApps_layoutParams);

					LinearLayout.LayoutParams llLauncherRunningApps_layoutParams = new LinearLayout.LayoutParams (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
					llLauncherRunningApps_layoutParams.gravity = Gravity.LEFT;
					llLauncherRunningApps.setLayoutParams (llLauncherRunningApps_layoutParams);

					break;
			}

			switch (res.getInteger (HomeActivity.theme.launcher_preferences_location))
			{
				case -1:
					lalPreferences.setVisibility (View.GONE);
					break;
			}

			switch (res.getInteger (HomeActivity.theme.panel_bfb_location))
			{
				case -1:
					tvPanelBfb.setVisibility (View.GONE);
					break;
				case 3:
					tvPanelBfb.setVisibility (View.VISIBLE);
					break;
			}

			tvPanelBfb.setText (res.getString (HomeActivity.theme.panel_bfb_text));
			tvPanelBfb.setTextColor (res.getColor (HomeActivity.theme.panel_bfb_text_colour));

			TypedArray launcherMargins = res.obtainTypedArray (HomeActivity.theme.launcher_margin);
			int launcherMarginTop = (int) launcherMargins.getDimension (0, 0);
			int launcherMarginRight = (int) launcherMargins.getDimension (1, 0);
			int launcherMarginBottom = (int) launcherMargins.getDimension (2, 0);
			int launcherMarginLeft = (int) launcherMargins.getDimension (3, 0);

			LinearLayout.LayoutParams llLauncher_layoutParams = (LinearLayout.LayoutParams) llLauncher.getLayoutParams ();
			llLauncher_layoutParams.setMargins (launcherMarginLeft, launcherMarginTop, launcherMarginRight, launcherMarginBottom);

			tvDashHomeTitle.setTextColor (res.getColor (HomeActivity.theme.dash_applauncher_text_colour));
			tvDashHomeTitle.setShadowLayer (5, 2, 2, res.getColor (HomeActivity.theme.dash_applauncher_text_shadow_colour));

			etDashSearch.setBackgroundResource (HomeActivity.theme.dash_search_background);
			etDashSearch.setTextColor (res.getColor (HomeActivity.theme.dash_search_text_colour));
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
	public void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		try
		{
			super.onActivityResult (requestCode, resultCode, data);

			if (requestCode == 1) // ActivityPreferences //
			{
				this.onCreate (null); // Reload activity //

				//this.overridePendingTransition (R.anim.home_to_preferences_in, R.anim.home_to_preferences_out);
			}/*
			else if (requestCode == 2) // Widget picked //
			{
				if (resultCode == RESULT_OK)
					this.widgetHost.configureWidget (data);
				else
					this.widgetHost.removeWidget (data);
			}
			else if (requestCode == 3) // Widget configured //
			{
				if (resultCode == RESULT_OK)
					this.widgetHost.createWidget (data);
				else
					this.widgetHost.removeWidget (data);
			}*/
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
	public void onNewIntent (Intent intent)
	{
		super.onNewIntent (intent);

		this.setIntent (intent);
	}

	@Override
	protected void onStart ()
	{
		super.onStart ();

		try
		{
			//this.widgetHost.startListening ();

			EasyTracker.getInstance (this).activityStart (this);
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this, ex);
			exh.show ();
		}
	}

	@Override
	protected void onStop ()
	{
		super.onStop ();

		try
		{
			//this.widgetHost.stopListening ();

			EasyTracker.getInstance (this).activityStop (this);
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this, ex);
			exh.show ();
		}
	}

	@Override
	public void onResume ()
	{
		super.onResume ();

		try
		{
			this.overridePendingTransition (R.anim.app_to_home_out, R.anim.app_to_home_in);

			Intent intent = this.getIntent ();
			boolean openDash = intent.getBooleanExtra ("openDash", false);

			if (openDash)
				this.openDash ();

			this.showLauncherService (false);

			if (this.apps != null)
			{
				SharedPreferences prefs = this.getSharedPreferences ("prefs", MODE_PRIVATE);

				if (prefs.getBoolean ("launcher_running_show", true))
					this.apps.addRunningApps (this.chameleonicBgColour);
			}
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this, ex);
			exh.show ();
		}
	}

	@Override
	public void onPause ()
	{
		super.onPause ();

		try
		{
			this.overridePendingTransition (R.anim.home_to_app_in, R.anim.home_to_app_out);

			this.showLauncherService (true);
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this, ex);
			exh.show ();
		}
	}

	@Override
	public void onDestroy ()
	{
		if (this.asyncInitWallpaper != null)
			this.asyncInitWallpaper.cancel (true);
		if (this.asyncLoadApps != null)
			this.asyncLoadApps.cancel (true);

		super.onDestroy ();
	}

	private void startLauncherService (boolean show)
	{
		SharedPreferences prefs = this.getSharedPreferences ("prefs", MODE_PRIVATE);

		if (prefs.getBoolean ("launcherservice_enabled", false))
		{
			AppLauncher lalbfb = (AppLauncher) this.findViewById (R.id.lalBfb);

			Intent intent = new Intent (this, LauncherService.class);
			intent.putParcelableArrayListExtra ("pinned", (ArrayList<App>) this.apps.getPinned ());
			intent.putExtra ("bgColour", this.chameleonicBgColour);
			intent.putExtra ("colour", lalbfb.getColour ());
			intent.putExtra ("start", true);
			intent.putExtra ("show", show);
			intent.putExtra ("visible", false);

			this.startService (intent);
		}
		else
		{
			Intent intent = new Intent (this, LauncherService.class);

			this.stopService (intent);
		}
	}

	private void showLauncherService (boolean show)
	{
		SharedPreferences prefs = this.getSharedPreferences ("prefs", MODE_PRIVATE);

		if (prefs.getBoolean ("launcherservice_enabled", false))
		{
			Intent intent = new Intent (this, LauncherService.class);
			intent.putExtra ("show", show);
			intent.putExtra ("visible", false);
			if (show && this.apps != null && prefs.getBoolean ("launcher_running_show", true))
				intent.putParcelableArrayListExtra ("running", (ArrayList<App>) this.apps.getRunningApps ());

			this.startService (intent);
		}
	}

	//# Callbacks #//
	public void asyncLoadInstalledAppsDone (AppManager installedApps)
	{
		try
		{
			LinearLayout llDashHomeAppsContainer = (LinearLayout) this.findViewById (R.id.llDashHomeAppsContainer);
			LinearLayout llDashHomeLensesContainer = (LinearLayout) this.findViewById (R.id.llDashHomeLensesContainer);
			ProgressWheel pwDashSearchProgress = (ProgressWheel) this.findViewById (R.id.pwDashSearchProgress);

			this.apps = installedApps;
			this.lenses = new LensManager (this.getApplicationContext (), llDashHomeAppsContainer, llDashHomeLensesContainer, pwDashSearchProgress, installedApps);

			EditText etDashSearch = (EditText) this.findViewById (R.id.etDashSearch);
			LinearLayout llLauncher = (LinearLayout) this.findViewById (R.id.llLauncher);
			be.robinj.ubuntu.unity.launcher.AppLauncher lalTrash = (be.robinj.ubuntu.unity.launcher.AppLauncher) llLauncher.findViewById (R.id.lalTrash);

			etDashSearch.addTextChangedListener (new SearchTextWatcher (installedApps, this.lenses));
			if (Build.VERSION.SDK_INT >= 11)
			{
				llLauncher.setOnDragListener (new LauncherDragListener (this.apps));
				lalTrash.setOnDragListener (new TrashDragListener (this.apps));
			}

			this.startLauncherService (false);

			SharedPreferences prefs = this.getSharedPreferences ("prefs", MODE_PRIVATE);

			if (prefs.getBoolean ("launcher_running_show", true))
				this.apps.addRunningApps (this.chameleonicBgColour);

			if (this.openDashWhenReady)
				this.openDash ();

			// Broadcast receiver //
			this.broadcastPackageManager = new PackageManagerBroadcastReceiver (this);

			IntentFilter ifPackageManager = new IntentFilter ();
			ifPackageManager.addAction ("android.intent.action.PACKAGE_INSTALL");
			ifPackageManager.addAction ("android.intent.action.PACKAGE_ADDED");
			ifPackageManager.addAction ("android.intent.action.PACKAGE_REMOVED");
			ifPackageManager.addDataScheme ("package");

			this.registerReceiver (this.broadcastPackageManager, ifPackageManager);
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
			be.robinj.ubuntu.unity.launcher.AppLauncher lalTrash = (be.robinj.ubuntu.unity.launcher.AppLauncher) this.findViewById (R.id.lalTrash);

			LinearLayout llLauncher = (LinearLayout) this.findViewById (R.id.llLauncher);
			LinearLayout llDash = (LinearLayout) this.findViewById (R.id.llDash);

			if (this.getResources ().getBoolean (HomeActivity.theme.launcher_applauncher_backgroundcolour_dynamic))
			{
				lalBfb.setColour (colour);
				lalPreferences.setColour (colour);
				lalSpinner.setColour (colour);
				lalTrash.setColour (colour);
			}

			if (this.getResources ().getBoolean (HomeActivity.theme.launcher_background_dynamic))
				llLauncher.setBackgroundColor (bgColour);
			else
				llLauncher.setBackgroundResource (HomeActivity.theme.launcher_background);

			if (this.getResources ().getBoolean (HomeActivity.theme.dash_background_dynamic))
				llDash.setBackgroundColor (bgColour);
			else
				llDash.setBackgroundResource (HomeActivity.theme.dash_background);

			this.chameleonicBgColour = bgColour;
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this, ex);
			exh.show ();
		}
	}

	public void pinnedAppsChanged ()
	{
		this.startLauncherService (false);

		SharedPreferences prefs = this.getSharedPreferences ("prefs", MODE_PRIVATE);

		if (prefs.getBoolean ("launcher_running_show", true))
			this.apps.addRunningApps (this.chameleonicBgColour);
	}

	public void installedAppsChanged ()
	{
		/*
		 * Replacing the existing instance of AppManager could result in problems.
		 * Need to figure out a (good) way to just replace the contents of the
		 * existing AppManager and have all the other components be updated automatically.
		 *
		 * Tried just restarting the app, but on relatively fast devices this would result
		 * in the broadcast being received over and over again and causing an infinite loop.
		 * It was kind of a dirty hack anyway, so that's alright.
		 */
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
		wpWallpaper.unblur ();
		etDashSearch.setText ("");
		etDashSearch.clearFocus ();

		if (this.getResources ().getInteger (HomeActivity.theme.panel_close_location) != -1)
			ibPanelDashClose.setVisibility (View.INVISIBLE);

		if (Build.VERSION.SDK_INT >= 11)
		{
			SharedPreferences prefs = this.getSharedPreferences ("prefs", MODE_PRIVATE);
			llPanel.setAlpha ((float) prefs.getInt ("panel_opacity", 100) / 100F);
		}

		if (this.getResources ().getBoolean (HomeActivity.theme.panel_background_dynamic_if_dash_opened))
		{
			llPanel.setBackgroundResource (HomeActivity.theme.panel_background);

			if (Build.VERSION.SDK_INT >= 19)
			{
				LinearLayout llStatusBar = (LinearLayout) this.findViewById (R.id.llStatusBar);
				llStatusBar.setBackgroundColor (this.getResources ().getColor (android.R.color.black));
			}
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
		wpWallpaper.blur ();
		if (Build.VERSION.SDK_INT >= 11)
			llPanel.setAlpha (1F);

		if (this.getResources ().getInteger (HomeActivity.theme.panel_close_location) != -1)
			ibPanelDashClose.setVisibility (View.VISIBLE);

		if (this.getResources ().getBoolean (HomeActivity.theme.panel_background_dynamic_if_dash_opened))
		{
			llPanel.setBackgroundColor (this.chameleonicBgColour);

			if (Build.VERSION.SDK_INT >= 19)
			{
				LinearLayout llStatusBar = (LinearLayout) this.findViewById (R.id.llStatusBar);
				llStatusBar.setBackgroundColor (this.chameleonicBgColour);
			}
		}
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
}

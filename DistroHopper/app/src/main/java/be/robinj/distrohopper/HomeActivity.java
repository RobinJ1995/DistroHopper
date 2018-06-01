package be.robinj.distrohopper;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
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
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import be.robinj.distrohopper.async.AsyncInitWallpaper;
import be.robinj.distrohopper.async.AsyncLoadAppLabels;
import be.robinj.distrohopper.async.AsyncLoadApps;
import be.robinj.distrohopper.broadcast.PackageManagerBroadcastReceiver;
import be.robinj.distrohopper.cache.AppLabelCache;
import be.robinj.distrohopper.dev.Log;
import be.robinj.distrohopper.dev.LogToaster;
import be.robinj.distrohopper.preferences.PreferencesActivity;
import be.robinj.distrohopper.theme.Default;
import be.robinj.distrohopper.theme.Elementary;
import be.robinj.distrohopper.theme.Gnome;
import be.robinj.distrohopper.theme.Location;
import be.robinj.distrohopper.theme.Theme;
import be.robinj.distrohopper.thirdparty.ProgressWheel;
import be.robinj.distrohopper.desktop.Wallpaper;
import be.robinj.distrohopper.desktop.dash.SearchTextWatcher;
import be.robinj.distrohopper.desktop.dash.lens.LensManager;
import be.robinj.distrohopper.desktop.launcher.AppLauncher;
import be.robinj.distrohopper.desktop.launcher.LauncherDragListener;
import be.robinj.distrohopper.desktop.launcher.TrashDragListener;
import be.robinj.distrohopper.desktop.launcher.service.LauncherService;
import be.robinj.distrohopper.widgets.WidgetHost;
import be.robinj.distrohopper.widgets.WidgetHost_LongClickListener;
import be.robinj.distrohopper.widgets.WidgetsContainer;


public class HomeActivity extends AppCompatActivity
{
	private LensManager lenses;
	private AppManager apps;
	private AppWidgetManager widgetManager;
	private WidgetHost widgetHost;

	private ViewFinder viewFinder;

	LinearLayout llDash;
	LinearLayout llPanel;
	ImageButton ibPanelDashClose;
	Wallpaper wpWallpaper;
	FrameLayout flWallpaperOverlay;
	FrameLayout flWallpaperOverlayWhenDashOpened;

	private int chameleonicBgColour = Color.argb (25, 0, 0, 0);

	private AsyncInitWallpaper asyncInitWallpaper;
	private AsyncLoadApps asyncLoadApps;
	private AsyncLoadAppLabels asyncLoadAppLabels;

	public static boolean modeCustomise = false;
	private boolean openDashWhenReady = false;

	public static Theme theme = new Default ();
	private int launcherEdge = Location.NONE;

	private PackageManagerBroadcastReceiver broadcastPackageManager;

	private LogToaster logToaster;

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_home);
		this.viewFinder = new ViewFinder(this);

		try
		{
			modeCustomise = false;
			final SharedPreferences prefs = this.getSharedPreferences ();

			Permission.requestBasicPermissions(this);

			// Only enable logging if dev mode is enabled // When not enabled nothing will be appended to the internal log variable //
			if (prefs.getBoolean ("dev", false)) {
				final Log log = Log.getInstance();
				this.logToaster = new LogToaster(this);

				log.setEnabled (true);
				log.attachObserver(this.logToaster);
			}

			// Get ALL the views! //
			final LinearLayout llLauncherAndDashContainer = this.viewFinder.get(R.id.llLauncherAndDashContainer);
			final LinearLayout llLauncher = this.viewFinder.get(R.id.llLauncher);
			final LinearLayout llLauncherPinnedApps = this.viewFinder.get(R.id.llLauncherPinnedApps);
			final LinearLayout llLauncherRunningApps = this.viewFinder.get(R.id.llLauncherRunningApps);
			final LinearLayout llBfbSpinnerWrapper = this.viewFinder.get(llLauncher, R.id.llBfbSpinnerWrapper);
			final be.robinj.distrohopper.desktop.launcher.SpinnerAppLauncher lalSpinner = this.viewFinder.get(llBfbSpinnerWrapper, R.id.lalSpinner);
			final be.robinj.distrohopper.desktop.launcher.AppLauncher lalBfb = this.viewFinder.get(llBfbSpinnerWrapper, R.id.lalBfb);
			final be.robinj.distrohopper.desktop.launcher.AppLauncher lalPreferences = this.viewFinder.get(llLauncher, R.id.lalPreferences);
			final be.robinj.distrohopper.desktop.launcher.AppLauncher lalTrash = this.viewFinder.get(llLauncher, R.id.lalTrash);
			this.llDash = this.viewFinder.get(llLauncherAndDashContainer, R.id.llDash);
			final GridView gvDashHomeApps = this.viewFinder.get(this.llDash, R.id.gvDashHomeApps);
			final LinearLayout llDashSearchContainer = this.viewFinder.get(this.llDash, R.id.llDashSearchContainer);
			final ImageView imgDashBackgroundGradient = this.viewFinder.get(this.llDash, R.id.imgDashBackgroundGradient);
			final TextView tvDashHomeTitle = this.viewFinder.get(this.llDash, R.id.tvDashHomeTitle);
			final EditText etDashSearch = this.viewFinder.get(this.llDash, R.id.etDashSearch);
			final ListView lvDashHomeLensResults = this.viewFinder.get(this.llDash, R.id.lvDashHomeLensResults);
			final LinearLayout llDashRibbon = this.viewFinder.get(this.llDash, R.id.llDashRibbon);
			this.wpWallpaper = this.viewFinder.get(R.id.wpWallpaper);
			final FrameLayout flWallpaperOverlayContainer = this.viewFinder.get(R.id.flWallpaperOverlayContainer);
			this.flWallpaperOverlay = this.viewFinder.get(flWallpaperOverlayContainer, R.id.flWallpaperOverlay);
			this.flWallpaperOverlayWhenDashOpened = this.viewFinder.get(flWallpaperOverlayContainer, R.id.flWallpaperOverlayWhenDashOpened);
			this.llPanel = this.viewFinder.get(R.id.llPanel);
			final TextView tvPanelBfb = this.viewFinder.get(this.llPanel, R.id.tvPanelBfb);
			this.ibPanelDashClose = this.viewFinder.get(this.llPanel, R.id.ibPanelDashClose);
			final ImageButton ibPanelCog = this.viewFinder.get(this.llPanel, R.id.ibPanelCog);
			final WidgetsContainer vgWidgets = this.viewFinder.get(R.id.vgWidgets);

			// Load up the theme //
			HashMap<String, Class> themes = new HashMap<String, Class> ();
			themes.put ("default", Default.class);
			themes.put ("elementary", Elementary.class);
			themes.put ("gnome", Gnome.class);

			Theme theme = (Theme) themes.get (prefs.getString ("theme", "default")).newInstance ();
			HomeActivity.theme = theme;

			// Load the launcher service //
			Intent launcherServiceIntent = new Intent (this, LauncherService.class);
			this.stopService (launcherServiceIntent);

			// Initialise the core launcher items //
			lalBfb.init ();
			lalSpinner.init ();
			lalPreferences.init ();
			lalTrash.init ();

			// Process panel user preferences // May get removed in future as themes should probably handle this //
			Resources res = this.getResources ();
			final float density = res.getDisplayMetrics ().density;

			this.setPanelEdge(prefs.getInt ("panel_edge", res.getInteger (HomeActivity.theme.panel_location)));

			int ibDashClose_width = (int) ((float) (48 + prefs.getInt ("launchericon_width", 36)) * density);
			LinearLayout.LayoutParams ibDashClose_layoutParams = new LinearLayout.LayoutParams (ibDashClose_width, LinearLayout.LayoutParams.MATCH_PARENT);
			this.ibPanelDashClose.setLayoutParams (ibDashClose_layoutParams);

			RelativeLayout.LayoutParams vgWidgets_layoutParams = (RelativeLayout.LayoutParams) vgWidgets.getLayoutParams ();
			vgWidgets_layoutParams.setMargins (ibDashClose_width, 0, 0, 0);
			vgWidgets.setLayoutParams (vgWidgets_layoutParams);

			// Start spinning the BFB //
			lalSpinner.getProgressWheel ().spin ();

			// Start initialising the wallpaper //
			this.asyncInitWallpaper = new AsyncInitWallpaper (this);
			this.asyncInitWallpaper.execute (this.wpWallpaper);

			// Start loading apps from the package manager //
			this.asyncLoadApps = new AsyncLoadApps (this, lalSpinner, lalBfb, gvDashHomeApps);
			this.asyncLoadApps.execute (this.getApplicationContext ());

			// Initialise the widget host //
			this.widgetManager = AppWidgetManager.getInstance (this);
			this.widgetHost = new WidgetHost (this, this.widgetManager, R.id.vgWidgets);

			if (prefs.getBoolean ("widgets_enabled", false) && prefs.getBoolean ("dev", false))
				vgWidgets.setOnLongClickListener (new WidgetHost_LongClickListener (this.widgetHost));

			// Setup layout transitions //
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

			LayoutTransition llLauncherAndDashContainer_transition = new LayoutTransition ();
			llLauncherAndDashContainer_transition.setStartDelay (LayoutTransition.APPEARING, 0);
			llLauncherAndDashContainer_transition.setStartDelay (LayoutTransition.DISAPPEARING, 0);
			llLauncherAndDashContainer_transition.setStartDelay (LayoutTransition.CHANGE_APPEARING, 0);
			llLauncherAndDashContainer_transition.setStartDelay (LayoutTransition.CHANGE_DISAPPEARING, 0);
			llLauncherAndDashContainer_transition.setStartDelay (LayoutTransition.CHANGING, 0);
			llLauncherAndDashContainer_transition.setDuration (res.getInteger (android.R.integer.config_shortAnimTime));
			llLauncherAndDashContainer.setLayoutTransition (llLauncherAndDashContainer_transition);

			LayoutTransition flWallpaperOverlayContainer_transition = new LayoutTransition ();
			flWallpaperOverlayContainer_transition.setStartDelay (LayoutTransition.APPEARING, 0);
			flWallpaperOverlayContainer_transition.setStartDelay (LayoutTransition.DISAPPEARING, 0);
			flWallpaperOverlayContainer_transition.setStartDelay (LayoutTransition.CHANGE_APPEARING, 0);
			flWallpaperOverlayContainer_transition.setStartDelay (LayoutTransition.CHANGE_DISAPPEARING, 0);
			flWallpaperOverlayContainer_transition.setStartDelay (LayoutTransition.CHANGING, 0);
			flWallpaperOverlayContainer_transition.setDuration (res.getInteger (android.R.integer.config_shortAnimTime));
			flWallpaperOverlayContainer.setLayoutTransition (flWallpaperOverlayContainer_transition);

			// Check if the dash should open immediately once apps have been loaded //
			this.openDashWhenReady = prefs.getBoolean ("dash_ready_show", this.openDashWhenReady);

			Intent intent = this.getIntent ();
			if (intent != null)
			{
				modeCustomise = intent.getBooleanExtra ("customise", modeCustomise);
				this.openDashWhenReady = intent.getBooleanExtra ("openDash", this.openDashWhenReady) || modeCustomise;
			}

			// Take control of system status bar background //
			if (Build.VERSION.SDK_INT >= 19)
			{
				final LinearLayout llStatusBar = this.viewFinder.get(R.id.llStatusBar);

				int llStatusBar_height = llStatusBar.getHeight ();
				int statusBarHeight_resource = res.getIdentifier ("status_bar_height", "dimen", "android");

				if (statusBarHeight_resource > 0)
					llStatusBar_height = res.getDimensionPixelSize (statusBarHeight_resource);

				RelativeLayout.LayoutParams llStatusBar_layoutParams = new RelativeLayout.LayoutParams (ViewGroup.LayoutParams.MATCH_PARENT, llStatusBar_height);
				llStatusBar.setLayoutParams (llStatusBar_layoutParams);
				llStatusBar.setVisibility (View.VISIBLE);
			}

			// Apply theme //
			this.llPanel.setBackgroundResource (HomeActivity.theme.panel_background);
			ibPanelCog.setImageResource (HomeActivity.theme.panel_preferences_image);
			this.ibPanelDashClose.setImageResource (HomeActivity.theme.panel_close_image);
			imgDashBackgroundGradient.setImageResource (HomeActivity.theme.dash_background_gradient);
			lalBfb.setIcon (res.getDrawable (HomeActivity.theme.launcher_bfb_image));
			lalPreferences.setIcon (res.getDrawable (HomeActivity.theme.launcher_preferences_image));
			lalTrash.setIcon (res.getDrawable (HomeActivity.theme.launcher_trash_image));

			RelativeLayout.LayoutParams llPanel_layoutParams = (RelativeLayout.LayoutParams) this.llPanel.getLayoutParams ();
			llPanel_layoutParams.height = (int) res.getDimension (R.dimen.theme_elementary_panel_height);

			final boolean expandLlLauncher = res.getBoolean (HomeActivity.theme.launcher_expand);
			this.launcherEdge = prefs.getInt ("launcher_edge", res.getInteger (HomeActivity.theme.launcher_location));
			this.setLauncherEdge (this.launcherEdge, expandLlLauncher);

			int lalPreferencesLocation = res.getInteger (HomeActivity.theme.launcher_preferences_location);
			if (! prefs.getBoolean ("panel_show", true))
				lalPreferencesLocation = res.getInteger (HomeActivity.theme.launcher_preferences_location_when_panel_hidden);

			switch (lalPreferencesLocation)
			{
				case Location.NONE:
					lalPreferences.setVisibility (View.GONE);
					break;
				case Location.TOP:
				case Location.LEFT:
					int posLlBfbSpinnerWrapper = llLauncher.indexOfChild (llBfbSpinnerWrapper);
					int posLalPreferences = (posLlBfbSpinnerWrapper == 0 ? 1 : 0);

					llLauncher.removeView (lalPreferences);
					llLauncher.addView (lalPreferences, posLalPreferences);
					break;
				case Location.RIGHT:
				case Location.BOTTOM:
					lalPreferences.setVisibility (View.VISIBLE);
					break;
			}

			switch (res.getInteger (HomeActivity.theme.launcher_bfb_location))
			{
				case Location.NONE:
					llBfbSpinnerWrapper.setVisibility (View.GONE);
					break;
				case Location.TOP:
				case Location.LEFT:
					llBfbSpinnerWrapper.setVisibility (View.VISIBLE);
					break;
				case Location.RIGHT:
				case Location.BOTTOM:
					int posLalPreferences = llLauncher.indexOfChild (lalPreferences);
					int posLalTrash = llLauncher.indexOfChild (lalTrash);
					int posLlBfbSpinnerWrapper = (posLalPreferences > 1 ? posLalPreferences : posLalTrash) - 1;

					llLauncher.removeView (llBfbSpinnerWrapper);
					llLauncher.addView (llBfbSpinnerWrapper, posLlBfbSpinnerWrapper);
					break;
			}

			switch (res.getInteger (HomeActivity.theme.panel_bfb_location))
			{
				case Location.NONE:
					tvPanelBfb.setVisibility (View.GONE);
					break;
				case Location.LEFT:
					tvPanelBfb.setVisibility (View.VISIBLE);
					break;
			}

			tvPanelBfb.setText (res.getString (HomeActivity.theme.panel_bfb_text));
			tvPanelBfb.setTextColor (res.getColor (HomeActivity.theme.panel_bfb_text_colour));

			tvDashHomeTitle.setTextColor (res.getColor (HomeActivity.theme.dash_applauncher_text_colour));
			tvDashHomeTitle.setShadowLayer (5, 2, 2, res.getColor (HomeActivity.theme.dash_applauncher_text_shadow_colour));

			etDashSearch.setBackgroundResource (HomeActivity.theme.dash_search_background);
			etDashSearch.setTextColor (res.getColor (HomeActivity.theme.dash_search_text_colour));

			llDashRibbon.setVisibility (res.getBoolean (HomeActivity.theme.dash_ribbon_show) ? View.VISIBLE : View.GONE);

			this.flWallpaperOverlay.setBackgroundResource (HomeActivity.theme.wallpaper_overlay);
			this.flWallpaperOverlayWhenDashOpened.setBackgroundResource (HomeActivity.theme.wallpaper_overlay_when_dash_opened);
			
			if (modeCustomise)
			{
				final HomeActivity self = this;
				final SharedPreferences.Editor prefsEdit = prefs.edit ();

				final LinearLayout llDashContent = this.viewFinder.get(R.id.llDashContent);
				final LinearLayout llDashCustomise = this.viewFinder.get(R.id.llDashCustomise);

				llDashContent.setVisibility (View.GONE);
				llDashCustomise.setVisibility (View.VISIBLE);

				// Launcher Icon Size //
				final SeekBar sbCustomiseLauncherIconSize = this.viewFinder.get(R.id.sbCustomiseLauncherIconSize);
				sbCustomiseLauncherIconSize.setProgress (prefs.getInt ("launchericon_width", 36));
				sbCustomiseLauncherIconSize.setOnSeekBarChangeListener
				(
					new SeekBar.OnSeekBarChangeListener ()
					{
						@Override
						public void onProgressChanged (SeekBar seekBar, int i, boolean b)
						{
							this.update (i);
						}

						@Override
						public void onStartTrackingTouch (SeekBar seekBar) {}

						@Override
						public void onStopTrackingTouch (SeekBar seekBar)
						{
							this.update (seekBar.getProgress ());
						}

						private void update (int value)
						{
							prefsEdit.putInt ("launchericon_width", value);
							prefsEdit.commit ();

							LinearLayout.LayoutParams ibDashClose_layoutParams = new LinearLayout.LayoutParams ((int) ((float) (48 + value) * density), LinearLayout.LayoutParams.MATCH_PARENT);
							ibPanelDashClose.setLayoutParams (ibDashClose_layoutParams);

							lalBfb.init ();
							lalSpinner.init ();
							for (int i = 0; i < llLauncherPinnedApps.getChildCount (); i++)
								((AppLauncher) llLauncherPinnedApps.getChildAt (i)).init ();
							for (int i = 0; i < llLauncherRunningApps.getChildCount (); i++)
								((AppLauncher) llLauncherRunningApps.getChildAt (i)).init ();
							lalTrash.init ();
							lalPreferences.init ();
						}
					}
				);

				// Launcher Edge //
				final String[] edgeNames = res.getStringArray (R.array.edges);

				final Spinner spiCustomiseLauncherEdge = this.viewFinder.get(R.id.spiCustomiseLauncherEdge);
				final int[] supporterLauncherEdges = res.getIntArray (HomeActivity.theme.launcher_location_supported);
				final List<String> supportedLauncherEdgeNames = new ArrayList<String> ();
				final int currentLauncherEdge = prefs.getInt ("launcher_edge", HomeActivity.theme.launcher_location);
				int currentLauncherEdgeIndex = -1;
				for (int i = 0; i < supporterLauncherEdges.length; i++)
				{
					int edge = supporterLauncherEdges[i];
					supportedLauncherEdgeNames.add (edgeNames[edge]);

					if (edge == currentLauncherEdge)
						currentLauncherEdgeIndex = i;
				}
				final String[] arrSupportedLauncherEdgeNames = supportedLauncherEdgeNames.toArray (new String[0]);

				final ArrayAdapter<String> spiCustomiseLauncherEdge_adapter = new ArrayAdapter<> (this, android.R.layout.simple_spinner_dropdown_item, arrSupportedLauncherEdgeNames);
				spiCustomiseLauncherEdge.setAdapter (spiCustomiseLauncherEdge_adapter);
				spiCustomiseLauncherEdge.setSelection (currentLauncherEdgeIndex);
				spiCustomiseLauncherEdge.setOnItemSelectedListener
				(
						new AdapterView.OnItemSelectedListener ()
						{
							@Override
							public void onItemSelected (AdapterView<?> adapterView, View view, int i, long l)
							{
								int edge = supporterLauncherEdges[i];
								boolean changing = currentLauncherEdge != edge;

								if (changing)
								{
									prefsEdit.putInt ("launcher_edge", edge);
									prefsEdit.commit ();

									Intent intent = self.getIntent ();
									intent.putExtra ("customise", true);
									self.finish ();
									self.startActivity (intent); // Reload activity //
								}
							}

							@Override
							public void onNothingSelected (AdapterView<?> adapterView) {}
						}
				);

				// Panel Edge //
				final Spinner spiCustomisePanelEdge = this.viewFinder.get(R.id.spiCustomisePanelEdge);
				final int[] supportedPanelEdges = res.getIntArray (HomeActivity.theme.panel_location_supported);
				if (supportedPanelEdges.length > 1) {
					final List<String> supportedPanelEdgeNames = new ArrayList<>();
					final int currentPanelEdge = prefs.getInt("panel_edge", HomeActivity.theme.panel_location);
					int currentPanelEdgeIndex = -1;
					for (int i = 0; i < supportedPanelEdges.length; i++) {
						final int edge = supportedPanelEdges[i];
						supportedPanelEdgeNames.add(edgeNames[edge]);

						if (edge == currentPanelEdge)
							currentPanelEdgeIndex = i;
					}
					final String[] arrSupportedPanelEdgeNames = supportedPanelEdgeNames.toArray(new String[0]);

					final ArrayAdapter<String> spiCustomisePanelEdge_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, arrSupportedPanelEdgeNames);
					spiCustomisePanelEdge.setAdapter(spiCustomisePanelEdge_adapter);
					spiCustomisePanelEdge.setSelection(currentPanelEdgeIndex);
					spiCustomisePanelEdge.setOnItemSelectedListener(
							new AdapterView.OnItemSelectedListener() {
								@Override
								public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
									int edge = supportedPanelEdges[i];
									boolean changing = currentPanelEdge != edge;

									if (changing) {
										prefsEdit.putInt("panel_edge", edge);
										prefsEdit.commit();

										Intent intent = self.getIntent();
										intent.putExtra("customise", true);
										self.finish();
										self.startActivity(intent); // Reload activity //
									}
								}

								@Override
								public void onNothingSelected(AdapterView<?> adapterView) {
								}
							}
					);
				} else {
					this.viewFinder.get(llDashCustomise, R.id.llCustomisePanelEdge).setVisibility(View.GONE);
				}
			}
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this);
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
		return super.onOptionsItemSelected (item);
	}

	@Override
	public void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		try
		{
			super.onActivityResult (requestCode, resultCode, data);

			if (requestCode == RequestCode.ACTIVITY_PREFERENCES)
			{
				Intent intent = this.getIntent ();
				
				if (resultCode == 4) // Customise UI //
					intent.putExtra ("customise", true);
			
				this.finish ();
				this.startActivity (intent); // Reload activity //
				
				//this.overridePendingTransition (R.anim.home_to_preferences_in, R.anim.home_to_preferences_out);
			}
			else if (requestCode == RequestCode.WIDGET_PICKED)
			{
				if (resultCode == RESULT_OK)
					this.widgetHost.configureWidget (data);
				else
					this.widgetHost.removeWidget (data);
			}
			else if (requestCode == RequestCode.WIDGET_CONFIGURED)
			{
				if (resultCode == RESULT_OK)
					this.widgetHost.createWidget (data);
				else
					this.widgetHost.removeWidget (data);
			}
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this);
		}
	}

	@Override
	public void onBackPressed ()
	{
		try
		{
			if (this.llDash.getVisibility () == View.VISIBLE)
				this.closeDash ();
			else if (! this.isDefaultLauncher ())
				super.onBackPressed ();
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this);
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
			if (this.widgetHost != null)
				this.widgetHost.startListening ();
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this);
		}
	}

	@Override
	protected void onStop ()
	{
		try
		{
			Log.getInstance().detachObserver(this.logToaster);

			if (this.widgetHost != null)
				this.widgetHost.stopListening ();
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this);
		}

		super.onStop ();
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
				SharedPreferences prefs = this.getSharedPreferences ();

				if (prefs.getBoolean ("launcher_running_show", false))
					this.apps.addRunningApps (this.chameleonicBgColour);
			}
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this);
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
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this);
		}
	}

	@Override
	public void onDestroy ()
	{
		if (this.asyncInitWallpaper != null)
			this.asyncInitWallpaper.cancel (true);
		if (this.asyncLoadApps != null)
			this.asyncLoadApps.cancel (true);
		if (this.asyncLoadAppLabels != null)
			this.asyncLoadAppLabels.cancel (true);

		super.onDestroy ();
	}

	private SharedPreferences getSharedPreferences() {
		return this.getSharedPreferences("prefs", MODE_PRIVATE);
	}

	private void startLauncherService (boolean show)
	{
		SharedPreferences prefs = this.getSharedPreferences ();

		if (prefs.getBoolean ("launcherservice_enabled", false) && prefs.getBoolean ("dev", false))
		{
			AppLauncher lalbfb = this.viewFinder.get(R.id.lalBfb);

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
		SharedPreferences prefs = this.getSharedPreferences ();

		if (prefs.getBoolean ("launcherservice_enabled", false) && prefs.getBoolean ("dev", false))
		{
			Intent intent = new Intent (this, LauncherService.class);
			intent.putExtra ("show", show);
			intent.putExtra ("visible", false);
			if (show && this.apps != null && prefs.getBoolean ("launcher_running_show", false))
				intent.putParcelableArrayListExtra ("running", (ArrayList<App>) this.apps.getRunningApps ());

			this.startService (intent);
		}
	}
	
	@SuppressLint ("ResourceType")
	private void setLauncherEdge (int edge, boolean expand)
	{
		final LinearLayout llPanel = this.viewFinder.get(R.id.llPanel);
		final ImageButton ibPanelDashClose = this.viewFinder.get(llPanel, R.id.ibPanelDashClose);
		final ImageButton ibPanelCog = this.viewFinder.get(llPanel, R.id.ibPanelCog);
		final LinearLayout llLauncherAndDashContainer = this.viewFinder.get(R.id.llLauncherAndDashContainer);
		final LinearLayout llLauncher = this.viewFinder.get(llLauncherAndDashContainer, R.id.llLauncher);
		final LinearLayout llLauncherAppsContainer = this.viewFinder.get(llLauncher, R.id.llLauncherAppsContainer);
		final LinearLayout llLauncherPinnedApps = this.viewFinder.get(R.id.llLauncherPinnedApps);
		final LinearLayout llLauncherRunningApps = this.viewFinder.get(R.id.llLauncherRunningApps);
		final LinearLayout llBfbSpinnerWrapper = this.viewFinder.get(llLauncher, R.id.llBfbSpinnerWrapper);
		final ScrollView scrLauncherAppsContainer = this.viewFinder.get(llLauncher, R.id.scrLauncherAppsContainer);
		final HorizontalScrollView scrLauncherAppsContainerHorizontal = this.viewFinder.get(llLauncher, R.id.scrLauncherAppsContainerHorizontal);
		LinearLayout.LayoutParams llLauncher_layoutParams = (LinearLayout.LayoutParams) llLauncher.getLayoutParams ();

		TypedArray taLauncherMargins = this.getResources ().obtainTypedArray (HomeActivity.theme.launcher_margin);
		final int launcherMargins[] = new int[] {
				taLauncherMargins.getLayoutDimension (0, 0),
				taLauncherMargins.getLayoutDimension (1, 0),
				taLauncherMargins.getLayoutDimension (2, 0),
				taLauncherMargins.getLayoutDimension (3, 0)
		};
		int rotateLauncherMargins = edge;

		int launcherMarginsRotated[] = new int[4];
		for (int i = 0; i <= launcherMargins.length - 1; i++)
			launcherMarginsRotated[(i + rotateLauncherMargins) % launcherMargins.length ] = launcherMargins[i];

		switch (edge)
		{
			case Location.TOP:
				llLauncherAndDashContainer.setOrientation (LinearLayout.VERTICAL);
				llLauncher.setOrientation (LinearLayout.HORIZONTAL);
				llBfbSpinnerWrapper.setOrientation (LinearLayout.HORIZONTAL);
				llLauncherAppsContainer.setOrientation (LinearLayout.HORIZONTAL);
				llLauncherPinnedApps.setOrientation (LinearLayout.HORIZONTAL);
				llLauncherRunningApps.setOrientation (LinearLayout.HORIZONTAL);

				llLauncherAndDashContainer.setGravity (Gravity.TOP | Gravity.CENTER);
				
				/*llLauncherAndDashContainer.removeView (llLauncher);
				llLauncherAndDashContainer.removeView (this.llDash);
				
				llLauncherAndDashContainer.addView (this.llDash);
				llLauncherAndDashContainer.addView (llLauncher);*/

				llLauncher_layoutParams = new LinearLayout.LayoutParams (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

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
			case Location.BOTTOM:
				llLauncherAndDashContainer.setOrientation (LinearLayout.VERTICAL);
				llLauncher.setOrientation (LinearLayout.HORIZONTAL);
				llBfbSpinnerWrapper.setOrientation (LinearLayout.HORIZONTAL);
				llLauncherAppsContainer.setOrientation (LinearLayout.HORIZONTAL);
				llLauncherPinnedApps.setOrientation (LinearLayout.HORIZONTAL);
				llLauncherRunningApps.setOrientation (LinearLayout.HORIZONTAL);

				llLauncherAndDashContainer.setGravity (Gravity.BOTTOM | Gravity.CENTER);

				llLauncherAndDashContainer.removeView (llLauncher);
				llLauncherAndDashContainer.removeView (this.llDash);

				llLauncherAndDashContainer.addView (this.llDash);
				llLauncherAndDashContainer.addView (llLauncher);

				llLauncher_layoutParams = new LinearLayout.LayoutParams (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

				//ScrollView.LayoutParams llLauncherAppsContainer_layoutParams = new ScrollView.LayoutParams (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
				//llLauncherAppsContainer.setLayoutParams (llLauncherAppsContainer_layoutParams);

				scrLauncherAppsContainer.setVisibility (View.GONE);
				scrLauncherAppsContainer.removeView (llLauncherAppsContainer);
				scrLauncherAppsContainerHorizontal.addView (llLauncherAppsContainer);
				scrLauncherAppsContainerHorizontal.setVisibility (View.VISIBLE);

				llLauncherPinnedApps_layoutParams = new LinearLayout.LayoutParams (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
				llLauncherPinnedApps_layoutParams.gravity = Gravity.LEFT;
				llLauncherPinnedApps.setLayoutParams (llLauncherPinnedApps_layoutParams);

				llLauncherRunningApps_layoutParams = new LinearLayout.LayoutParams (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
				llLauncherRunningApps_layoutParams.gravity = Gravity.LEFT;
				llLauncherRunningApps.setLayoutParams (llLauncherRunningApps_layoutParams);

				break;
			case Location.RIGHT:
				llLauncherAndDashContainer.setGravity (Gravity.RIGHT | Gravity.CENTER);

				llLauncherAndDashContainer.removeView (llLauncher);
				llLauncherAndDashContainer.removeView (llDash);

				llLauncherAndDashContainer.addView (llDash);
				llLauncherAndDashContainer.addView (llLauncher);

				break;
			case Location.LEFT: // Falls through //
				llLauncherAndDashContainer.setGravity (Gravity.LEFT | Gravity.CENTER);

				break;
		}

		if (! expand)
		{
			llLauncher_layoutParams = new LinearLayout.LayoutParams (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			llLauncher.setLayoutParams (llLauncher_layoutParams);
		}

		final int[] panelSwapClosePreferencesWhenLauncherLocation = this.getResources ().getIntArray (theme.panel_swap_close_preferences_when_launcher_location);
		boolean panelSwapClosePreferences = false;
		for (int i = 0; i < panelSwapClosePreferencesWhenLauncherLocation.length; i++)
		{
			if (panelSwapClosePreferencesWhenLauncherLocation[i] == edge)
			{
				panelSwapClosePreferences = true;

				break;
			}
		}
		if (panelSwapClosePreferences)
		{
			llPanel.removeView (ibPanelDashClose);
			llPanel.addView (ibPanelDashClose, llPanel.indexOfChild (ibPanelCog));
			llPanel.removeView (ibPanelCog);
			llPanel.addView (ibPanelCog, 0);
		}

		llLauncher_layoutParams.setMargins (launcherMarginsRotated[3], launcherMarginsRotated[0], launcherMarginsRotated[1], launcherMarginsRotated[2]);
		llLauncher.setLayoutParams (llLauncher_layoutParams);
	}

	private void setPanelEdge (final int edge)
	{
		final SharedPreferences prefs = this.getSharedPreferences();

		switch (edge) {
			case Location.TOP:
				this.llPanel.setAlpha ((float) prefs.getInt ("panel_opacity", 100) / 100F);
				break;
			case Location.NONE:
				this.llPanel.setVisibility (View.GONE);
				break;
		}
	}

	public AppManager getAppManager ()
	{
		return this.apps;
	}

	//# Callbacks #//
	public void asyncLoadInstalledAppsDone (AppManager installedApps)
	{
		try
		{
			LinearLayout llDashHomeAppsContainer = this.viewFinder.get(R.id.llDashHomeAppsContainer);
			LinearLayout llDashHomeLensesContainer = this.viewFinder.get(R.id.llDashHomeLensesContainer);
			ProgressWheel pwDashSearchProgress = this.viewFinder.get(R.id.pwDashSearchProgress);

			this.apps = installedApps;
			this.lenses = new LensManager (this.getApplicationContext (), llDashHomeAppsContainer, llDashHomeLensesContainer, pwDashSearchProgress, installedApps);

			EditText etDashSearch = this.viewFinder.get(R.id.etDashSearch);
			LinearLayout llLauncher = this.viewFinder.get(R.id.llLauncher);
			be.robinj.distrohopper.desktop.launcher.AppLauncher lalTrash = this.viewFinder.get(llLauncher, R.id.lalTrash);

			etDashSearch.addTextChangedListener (new SearchTextWatcher (installedApps, this.lenses));
			llLauncher.setOnDragListener (new LauncherDragListener (this.apps));
			lalTrash.setOnDragListener (new TrashDragListener (this.apps));

			this.startLauncherService (false);

			SharedPreferences prefs = this.getSharedPreferences ();

			if (prefs.getBoolean ("launcher_running_show", false))
				this.apps.addRunningApps (this.chameleonicBgColour);

			if (this.openDashWhenReady)
				this.openDash ();

			// Broadcast receiver //
			this.broadcastPackageManager = new PackageManagerBroadcastReceiver (this);

			Resources res = this.getResources ();

			IntentFilter ifPackageManager = new IntentFilter ();
			ifPackageManager.addAction (res.getString (R.string.intent_action_package_added_legacy));
			ifPackageManager.addAction (res.getString (R.string.intent_action_package_added));
			ifPackageManager.addAction (res.getString (R.string.intent_action_package_removed));
			ifPackageManager.addDataScheme ("package");

			this.registerReceiver (this.broadcastPackageManager, ifPackageManager);

			this.asyncLoadAppLabels = new AsyncLoadAppLabels(installedApps);
			this.asyncLoadAppLabels.execute(new AppLabelCache(this.getBaseContext()));
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this);
		}
	}

	public void asyncInitWallpaperDone (Wallpaper wpWallpaper)
	{
		try
		{
			final SharedPreferences prefs = this.getSharedPreferences ();
			final Resources res = this.getResources ();

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

			be.robinj.distrohopper.desktop.launcher.AppLauncher lalBfb = this.viewFinder.get(R.id.lalBfb);
			be.robinj.distrohopper.desktop.launcher.AppLauncher lalPreferences = this.viewFinder.get(R.id.lalPreferences);
			be.robinj.distrohopper.desktop.launcher.AppLauncher lalSpinner = this.viewFinder.get(R.id.lalSpinner);
			be.robinj.distrohopper.desktop.launcher.AppLauncher lalTrash = this.viewFinder.get(R.id.lalTrash);

			LinearLayout llLauncher = this.viewFinder.get(R.id.llLauncher);

			if (res.getBoolean (HomeActivity.theme.launcher_applauncher_backgroundcolour_dynamic))
			{
				lalBfb.setColour (colour);
				lalPreferences.setColour (colour);
				lalSpinner.setColour (colour);
				lalTrash.setColour (colour);
			}

			final TypedArray launcherBackgroundResources = res.obtainTypedArray (HomeActivity.theme.launcher_background);
			if (this.getResources ().getBoolean (HomeActivity.theme.launcher_background_dynamic))
				llLauncher.setBackgroundColor (bgColour);
			else
				llLauncher.setBackgroundResource (launcherBackgroundResources.getResourceId (this.launcherEdge, R.color.transparent));

			if (res.getBoolean (HomeActivity.theme.dash_background_dynamic))
				this.llDash.setBackgroundColor (bgColour);
			else
				this.llDash.setBackgroundResource (HomeActivity.theme.dash_background);

			this.chameleonicBgColour = bgColour;
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this);
		}
	}

	public void pinnedAppsChanged ()
	{
		this.startLauncherService (false);

		SharedPreferences prefs = this.getSharedPreferences ();

		if (prefs.getBoolean ("launcher_running_show", false))
			this.apps.addRunningApps (this.chameleonicBgColour);
	}

	//# Event handlers #//
	public void lalBfb_clicked (View view)
	{
		try
		{
			if (this.llDash.getVisibility () == View.VISIBLE)
				this.closeDash (true);
			else
				this.openDash (true);
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this);
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
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this);
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
			this.startActivityForResult (intent, RequestCode.ACTIVITY_PREFERENCES);
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this);
		}
	}

	public void ibPanelDevLogs_clicked (View view)
	{
		try
		{
			this.openDash ();
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this);
		}
	}

	//# Dash #//
	private void closeDash ()
	{
		this.closeDash (false);
	}

	private void closeDash (boolean track)
	{
		if (modeCustomise)
		{
			Intent intent = this.getIntent ();
			intent.putExtra ("customise", false);
			
			this.finish ();
			this.startActivity (intent);
			
			return;
		}
		
		EditText etDashSearch = this.viewFinder.get(R.id.etDashSearch);

		this.llDash.setVisibility (View.GONE);
		this.wpWallpaper.unblur ();
		etDashSearch.setText ("");
		etDashSearch.clearFocus ();

		if (this.getResources ().getInteger (HomeActivity.theme.panel_close_location) != -1)
			this.ibPanelDashClose.setVisibility (View.INVISIBLE);
		
		SharedPreferences prefs = this.getSharedPreferences ();
		this.llPanel.setAlpha ((float) prefs.getInt ("panel_opacity", 100) / 100F);

		if (this.getResources ().getBoolean (HomeActivity.theme.panel_background_dynamic_when_dash_opened))
		{
			this.llPanel.setBackgroundResource (HomeActivity.theme.panel_background);

			if (Build.VERSION.SDK_INT >= 19)
			{
				LinearLayout llStatusBar = this.viewFinder.get(R.id.llStatusBar);
				llStatusBar.setBackgroundColor (this.getResources ().getColor (android.R.color.black));
			}
		}

		InputMethodManager imm = (InputMethodManager) this.getSystemService (Context.INPUT_METHOD_SERVICE);
		if (imm != null)
			imm.hideSoftInputFromWindow (this.getWindow ().getDecorView ().getRootView ().getWindowToken (), 0);

		this.flWallpaperOverlay.setVisibility (View.VISIBLE);
		this.flWallpaperOverlayWhenDashOpened.setVisibility (View.INVISIBLE);

		if (track)
			Tracker.trackEvent ("Home", "Dash closed", "Dash");
	}

	private void openDash ()
	{
		this.openDash (false);
	}

	private void openDash (boolean track)
	{
		this.llDash.setVisibility (View.VISIBLE);
		this.wpWallpaper.blur ();
		this.llPanel.setAlpha (1F);

		if (this.getResources ().getInteger (HomeActivity.theme.panel_close_location) != -1)
			this.ibPanelDashClose.setVisibility (View.VISIBLE);

		if (this.getResources ().getBoolean (HomeActivity.theme.panel_background_dynamic_when_dash_opened))
		{
			this.llPanel.setBackgroundColor (this.chameleonicBgColour);

			if (Build.VERSION.SDK_INT >= 19)
			{
				LinearLayout llStatusBar = this.viewFinder.get(R.id.llStatusBar);
				llStatusBar.setBackgroundColor (this.chameleonicBgColour);
			}
		}

		this.flWallpaperOverlay.setVisibility (View.INVISIBLE);
		this.flWallpaperOverlayWhenDashOpened.setVisibility (View.VISIBLE);

		if (track)
			Tracker.trackEvent ("Home", "Dash opened", "Dash");
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

package be.robinj.distrohopper;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

import be.robinj.distrohopper.broadcast.PackageManagerBroadcastReceiver;
import be.robinj.distrohopper.broadcast.WorkProfileAppsCallback;
import be.robinj.distrohopper.cache.AppIconCache;
import be.robinj.distrohopper.cache.AppLabelCache;
import be.robinj.distrohopper.cache.ExpiringCache;
import be.robinj.distrohopper.cache.ICache;
import be.robinj.distrohopper.dev.Log;
import be.robinj.distrohopper.home.CustomiseModeUi;
import be.robinj.distrohopper.home.DashController;
import be.robinj.distrohopper.home.Desktops;
import be.robinj.distrohopper.home.HomeGestureController;
import be.robinj.distrohopper.home.HomeStateBinder;
import be.robinj.distrohopper.home.HomeViewModel;
import be.robinj.distrohopper.home.LauncherEdgeController;
import be.robinj.distrohopper.home.LayoutTransitionConfigurer;
import be.robinj.distrohopper.home.SearchLoader;
import be.robinj.distrohopper.home.StartupLoader;
import be.robinj.distrohopper.home.ThemeApplier;
import be.robinj.distrohopper.home.WallpaperColourApplier;
import be.robinj.distrohopper.dev.LogToaster;
import be.robinj.distrohopper.onboarding.OnboardingActivity;
import be.robinj.distrohopper.onboarding.OnboardingGate;
import be.robinj.distrohopper.preferences.Preference;
import be.robinj.distrohopper.preferences.Preferences;
import be.robinj.distrohopper.preferences.PreferencesActivity;
import be.robinj.distrohopper.theme.Default;
import be.robinj.distrohopper.theme.Location;
import be.robinj.distrohopper.theme.Theme;
import be.robinj.distrohopper.thirdparty.ProgressWheel;
import be.robinj.distrohopper.desktop.Wallpaper;
import be.robinj.distrohopper.desktop.dash.SearchTextWatcher;
import be.robinj.distrohopper.desktop.dash.SwipeToCloseLayout;
import be.robinj.distrohopper.desktop.dash.lens.LensManager;
import be.robinj.distrohopper.desktop.launcher.AppLauncher;
import be.robinj.distrohopper.desktop.launcher.LauncherDragListener;
import be.robinj.distrohopper.desktop.launcher.TrashDragListener;
import be.robinj.distrohopper.desktop.launcher.service.LauncherService;
import be.robinj.distrohopper.widgets.WidgetHost;
import be.robinj.distrohopper.widgets.WidgetHost_LongClickListener;
import be.robinj.distrohopper.widgets.WidgetsContainer_DragListener;
import be.robinj.distrohopper.widgets.WidgetsPager;


public class HomeActivity extends AppCompatActivity
{
	private LensManager lenses;
	private AppManager apps;
	private AppWidgetManager widgetManager;
	private WidgetHost widgetHost;

	private ViewFinder viewFinder;

	LinearLayout llDash;

	private StartupLoader startupLoader;
	private CustomiseModeUi customiseModeUi;

	private boolean openDashWhenReady = false;

	private Theme theme = new Default ();

	private HomeViewModel viewModel;
	private LauncherEdgeController edgeController;
	private DashController dash;
	private HomeGestureController gestures;
	private Desktops desktops;
	private ThemeApplier themeApplier;
	private WallpaperColourApplier wallpaperColourApplier;

	private PackageManagerBroadcastReceiver broadcastPackageManager;
	private WorkProfileAppsCallback workProfileAppsCallback;

	private LogToaster logToaster;

	private ICache appLabelCache;
	private ICache appIconCache;

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate (savedInstanceState);

		// First run: hand over to the setup wizard before initialising anything //
		if (OnboardingGate.shouldShow (DependencyContainer.of (this).getPrefs ()))
		{
			this.startActivity (new Intent (this, OnboardingActivity.class));
			this.finish ();

			return;
		}

		setContentView (R.layout.activity_home);
		this.viewFinder = new ViewFinder(this);

		try
		{
			// Reset first, before anything that can throw: the flag is app-scoped
			// and a failed onCreate must not leave app launching disabled
			// (App.launch() refuses to launch while it is set) //
			final DependencyContainer container = DependencyContainer.of (this);
			container.getCustomiseMode ().setValue (false);

			final SharedPreferences prefs = this.getSharedPreferences ();

			// Only enable logging if dev mode is enabled // When not enabled nothing will be appended to the internal log variable //
			if (prefs.getBoolean (Preference.DEV.getName(), false)) {
				final Log log = Log.getInstance();
				log.setEnabled (true);

				if (prefs.getBoolean(Preference.DEV_LOG_TOASTER.getName(), false)) {
					this.logToaster = new LogToaster(this);
					log.attachObserver(this.logToaster);
				}
			}

			// Initialise caches //
			this.appLabelCache = new AppLabelCache(this.getBaseContext());
			this.appIconCache = new ExpiringCache(this.getBaseContext(),
					new AppIconCache(this.getBaseContext()), AppIconCache.EXPIRATION);

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
			final Wallpaper wpWallpaper = this.viewFinder.get(R.id.wpWallpaper);
			final LinearLayout llPanel = this.viewFinder.get(R.id.llPanel);
			final ImageButton ibPanelDashClose = this.viewFinder.get(llPanel, R.id.ibPanelDashClose);
			final WidgetsPager vgWidgets = this.viewFinder.get(R.id.vgWidgets);

			// Load up the theme and wire up the controllers that manage the views //
			this.theme = container.getThemeManager ().getCurrent ();
			this.edgeController = new LauncherEdgeController (this, this.viewFinder, this.theme, container.getPrefs ());
			this.dash = new DashController (this, this.viewFinder, this.theme, container.getPrefs ());
			this.themeApplier = new ThemeApplier (this, this.viewFinder, this.theme, this.edgeController);
			this.wallpaperColourApplier = new WallpaperColourApplier (this, this.viewFinder, this.theme, this.edgeController);
			this.viewModel = new ViewModelProvider (this, new HomeViewModel.Factory (container))
					.get (HomeViewModel.class);
			HomeStateBinder.bind (this, this.viewModel, this.dash, this.themeApplier);
			this.gestures = new HomeGestureController (this, this.viewFinder, this.dash,
					this.viewModel, () -> container.getCustomiseMode ().getValue ());
			((SwipeToCloseLayout) this.llDash).setDelegate (this.gestures);

			// Lay out edge-to-edge on every API level; SDK 35+ enforces it anyway. The status
			// bar is compensated for by llStatusBar below. Tappable element insets keep the
			// launcher and dash clear of the 3-button navigation bar, while remaining zero for
			// gesture navigation so the launcher still extends behind the gesture pill. The
			// wallpaper and the launcher/dash backgrounds keep covering the whole screen, as
			// backgrounds are drawn over padding. //
			WindowCompat.setDecorFitsSystemWindows (this.getWindow (), false);
			ViewCompat.setOnApplyWindowInsetsListener (llLauncherAndDashContainer, (v, windowInsets) ->
			{
				final Insets insets = windowInsets.getInsets (WindowInsetsCompat.Type.tappableElement ());
				this.edgeController.setNavigationInsets (insets);
				v.setPadding (insets.left, 0, insets.right, insets.bottom);
				this.edgeController.updateWidgetAreaInsets (vgWidgets, llLauncher);

				return windowInsets;
			});

			// Load the launcher service //
			Intent launcherServiceIntent = new Intent (this, LauncherService.class);
			this.stopService (launcherServiceIntent);

			// Initialise the core launcher items //
			lalBfb.init ();
			lalSpinner.init ();
			lalPreferences.init ();
			lalTrash.init ();

			// Process panel user preferences // Themes should probably handle this? //
			final Resources res = this.getResources ();
			final float density = res.getDisplayMetrics ().density;

			this.edgeController.applyPanelEdge(Location.of(prefs.getInt(Preference.PANEL_EDGE.getName(), res.getInteger(this.theme.panel_location))));

			int ibDashClose_width = (int) ((float) (48 + prefs.getInt (Preference.LAUNCHERICON_WIDTH.getName(), 36)) * density);
			LinearLayout.LayoutParams ibDashClose_layoutParams = new LinearLayout.LayoutParams (ibDashClose_width, LinearLayout.LayoutParams.MATCH_PARENT);
			ibPanelDashClose.setLayoutParams (ibDashClose_layoutParams);

			// Start spinning the BFB //
			lalSpinner.getProgressWheel ().spin ();

			// Cross-window blur can be toggled at runtime (e.g. battery saver) //
			this.getWindowManager ().addCrossWindowBlurEnabledListener (this.dash.getCrossWindowBlurListener ());

			// Setup layout transitions //
			LayoutTransitionConfigurer.apply (this.viewFinder, res);

			Intent intent = this.getIntent ();
			if (intent != null)
			{
				container.getCustomiseMode ().setValue (
						intent.getBooleanExtra ("customise", container.getCustomiseMode ().getValue ()));
				this.openDashWhenReady = intent.getBooleanExtra ("openDash", this.openDashWhenReady)
						|| container.getCustomiseMode ().getValue ();
			}

			// Take control of system status bar background //
			final LinearLayout llStatusBar = this.viewFinder.get(R.id.llStatusBar);

			int llStatusBar_height = llStatusBar.getHeight ();
			int statusBarHeight_resource = res.getIdentifier ("status_bar_height", "dimen", "android");

			if (statusBarHeight_resource > 0)
				llStatusBar_height = res.getDimensionPixelSize (statusBarHeight_resource);

			RelativeLayout.LayoutParams llStatusBar_layoutParams = new RelativeLayout.LayoutParams (ViewGroup.LayoutParams.MATCH_PARENT, llStatusBar_height);
			llStatusBar.setLayoutParams (llStatusBar_layoutParams);
			llStatusBar.setVisibility (View.VISIBLE);

			// Apply theme //
			this.themeApplier.apply ();

			// Initialise the widget host // After applyTheme(), which determines the launcher edge //
			this.widgetManager = AppWidgetManager.getInstance (this);
			this.widgetHost = new WidgetHost (this, this.widgetManager, vgWidgets);

			// Attached here rather than once apps are loaded: widgets are draggable
			// (and droppable on the trash) as soon as they are restored below //
			lalTrash.setOnDragListener (new TrashDragListener (this));

			vgWidgets.setOnLongClickListener (new WidgetHost_LongClickListener (this.widgetHost));
			llLauncherAndDashContainer.setOnDragListener (
					new WidgetsContainer_DragListener (this, vgWidgets));
			vgWidgets.setOnClickListener (new View.OnClickListener ()
			{
				@Override
				public void onClick (final View view)
				{
					vgWidgets.exitEditMode ();
				}
			});
			// The widget pager is clickable (see above), so touches on empty desktop
			// space are consumed here and never bubble up to Activity#onTouchEvent;
			// feed them to the swipe gestures from the pager itself. Returning false
			// until a swipe is recognised keeps taps and long-presses working //
			vgWidgets.setOnTouchListener ((view, event) ->
			{
				final boolean handled = this.gestures != null && this.gestures.onHomeTouchEvent (event);

				if (handled)
					view.cancelLongPress (); // A recognised swipe is not a long-press //

				return handled;
			});

			// Per-desktop pins: the launcher morphs/rebuilds as the desktops are
			// swiped between. The desktop count follows widgets + pins via the
			// Desktops coordinator, wired once the app model has loaded //
			vgWidgets.setOnPageScroll ((fromPage, toPage, fraction) ->
			{
				if (this.apps != null)
					this.apps.onLauncherPageScroll (fromPage, toPage, fraction);
			});
			vgWidgets.setOnPageSettled (page ->
			{
				if (this.apps != null)
					this.apps.onLauncherPageSettled (page);
			});

			// Keep the widget area clear of the launcher, even when the launcher resizes //
			llLauncher.addOnLayoutChangeListener (new View.OnLayoutChangeListener ()
			{
				@Override
				public void onLayoutChange (final View view, final int left, final int top, final int right, final int bottom,
						final int oldLeft, final int oldTop, final int oldRight, final int oldBottom)
				{
					HomeActivity.this.edgeController.updateWidgetAreaInsets (vgWidgets, view);
				}
			});

			this.widgetHost.restoreWidgets ();

			if (container.getCustomiseMode ().getValue ())
			{
				this.customiseModeUi = new CustomiseModeUi (this, this.viewFinder, this.theme, () ->
				{
					final Intent relaunchIntent = this.getIntent ();
					relaunchIntent.putExtra ("customise", true);
					this.finish ();
					this.startActivity (relaunchIntent); // Reload activity //
				});
				this.customiseModeUi.show ();
			}

			// Start loading: wallpaper, apps, then label/icon caches //
			this.startupLoader = new StartupLoader (this, container.getDispatchers ());
			this.startupLoader.start (wpWallpaper, lalSpinner, lalBfb,
					this.appLabelCache, this.appIconCache);
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
			super.onActivityResult(requestCode, resultCode, data);

			if (requestCode == RequestCode.ACTIVITY_PREFERENCES) {
				// Use a fresh intent rather than mutating getIntent(); the old intent may
				// still carry a customise extra from a previous relaunch //
				final Intent intent = new Intent(this, HomeActivity.class);

				if (resultCode == 4) { // Customise UI //
					intent.putExtra("customise", true);
				}

				this.finish();
				this.startActivity(intent); // Reload activity //

				//this.overridePendingTransition (R.anim.home_to_preferences_in, R.anim.home_to_preferences_out);
			} else if (requestCode == RequestCode.WIDGET_BOUND) {
				this.widgetHost.onBindResult(resultCode, data);
			} else if (requestCode == RequestCode.WIDGET_CONFIGURED) {
				this.widgetHost.onConfigureResult(resultCode, data);
			}
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this);
		}
	}

	@Override
	protected void onNewIntent (final Intent intent)
	{
		super.onNewIntent(intent);

		// launchMode is singleTop, so the relaunch intent from onActivityResult() can be
		// delivered here instead of to a new instance; adopt it and recreate if needed //
		this.setIntent(intent);

		if (intent.getBooleanExtra("customise", false)
				!= DependencyContainer.of(this).getCustomiseMode().getValue()) {
			this.recreate();
		}

		// Pressing home (or the home navigation gesture) while the launcher is
		// already running lands here; it always returns to the first desktop //
		if (Intent.ACTION_MAIN.equals(intent.getAction())
				&& intent.hasCategory(Intent.CATEGORY_HOME)) {
			this.returnToFirstDesktop();
		}
	}

	public void returnToFirstDesktop ()
	{
		if (this.viewFinder != null)
			this.viewFinder.<WidgetsPager>get(R.id.vgWidgets).setCurrentPage(0, true);
	}

	@Override
	public void onRequestPermissionsResult(final int requestCode,
	                                       @NonNull final String[] permissions,
	                                       @NonNull final int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	/**
	 * Touches that no view claimed — i.e. on empty desktop space — end up
	 * here; HomeGestureController turns them into the swipe-down-for-
	 * notifications and swipe-up-for-dash gestures.
	 */
	@Override
	public boolean onTouchEvent (final MotionEvent event)
	{
		try
		{
			if (this.gestures != null && this.gestures.onHomeTouchEvent (event))
				return true;
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this);
		}

		return super.onTouchEvent (event);
	}

	@Override
	public void onBackPressed ()
	{
		try
		{
			final WidgetsPager vgWidgets = this.viewFinder.get(R.id.vgWidgets);

			if (vgWidgets.hasEditModeChild ())
				vgWidgets.exitEditMode ();
			else if (this.llDash.getVisibility () == View.VISIBLE)
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
		try {
			Log.getInstance().detachObserver(this.logToaster);
		} catch (final Exception ex) {
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.logAndTrack();
		}

		try {
			if (this.widgetHost != null) {
				this.widgetHost.stopListening();
			}
		} catch (final Exception ex) {
			// ¯\_(ツ)_/¯
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
			else if (this.apps != null && this.dash.isOpen ())
				// Launching from an already-open dash leaves it open, so openDash()
				// won't run on the way back; refresh the usage-based order here too so
				// the just-launched app moves (no-op for the alphabetical order) //
				this.apps.refreshDashSortOrder ();

			this.showLauncherService (false);

			if (this.apps != null)
			{
				SharedPreferences prefs = this.getSharedPreferences ();

				if (prefs.getBoolean (Preference.LAUNCHER_SHOW_RUNNING_APPS.getName(), false))
					this.apps.addRunningApps (this.dash.getChameleonicBgColour ());
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
	public void onConfigurationChanged (Configuration newConfig)
	{
		super.onConfigurationChanged (newConfig);

		// The activity is not recreated on rotation (configChanges in the
		// manifest); re-apply the dash grid's column count for the new
		// orientation. Widgets reflow on their own via the cell-based
		// WidgetsContainer layout. //
		if (this.apps != null)
			this.apps.applyDashColumns ();

		// Keep the customise-mode grid-size hint in step with the new orientation //
		if (this.customiseModeUi != null)
			this.customiseModeUi.refreshDashGridHint ();
	}

	@Override
	public void onDestroy ()
	{
		if (this.dash != null)
			this.getWindowManager ().removeCrossWindowBlurEnabledListener (this.dash.getCrossWindowBlurListener ());

		if (this.broadcastPackageManager != null)
		{
			this.unregisterReceiver (this.broadcastPackageManager);
			this.broadcastPackageManager = null;
		}

		if (this.workProfileAppsCallback != null)
		{
			((LauncherApps) this.getSystemService (Context.LAUNCHER_APPS_SERVICE))
					.unregisterCallback (this.workProfileAppsCallback);
			this.workProfileAppsCallback = null;
		}

		super.onDestroy ();
	}

	private SharedPreferences getSharedPreferences() {
		return Preferences.getSharedPreferences(this, Preferences.PREFERENCES);
	}


	private void startLauncherService (boolean show)
	{
		SharedPreferences prefs = this.getSharedPreferences ();

		if (prefs.getBoolean (Preference.LAUNCHERSERVICE_ENABLED.getName(), false) && prefs.getBoolean (Preference.DEV.getName(), false))
		{
			AppLauncher lalbfb = this.viewFinder.get(R.id.lalBfb);

			Intent intent = new Intent (this, LauncherService.class);
			intent.putParcelableArrayListExtra ("pinned", (ArrayList<App>) this.apps.getPinned ());
			intent.putExtra ("bgColour", this.dash.getChameleonicBgColour ());
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

		if (prefs.getBoolean (Preference.LAUNCHERSERVICE_ENABLED.getName(), false) && prefs.getBoolean (Preference.DEV.getName(), false))
		{
			Intent intent = new Intent (this, LauncherService.class);
			intent.putExtra ("show", show);
			intent.putExtra ("visible", false);
			if (show && this.apps != null && prefs.getBoolean (Preference.LAUNCHER_SHOW_RUNNING_APPS.getName(), false))
				intent.putParcelableArrayListExtra ("running", (ArrayList<App>) this.apps.getRunningApps ());

			this.startService (intent);
		}
	}
	
	@SuppressLint ("ResourceType")




	public AppManager getAppManager ()
	{
		return this.apps;
	}

	public ViewFinder getViewFinder() {
		return this.viewFinder;
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
			// Runs searches on the activity's lifecycleScope + dispatchers, like StartupLoader //
			this.lenses.setSearchLoader (new SearchLoader (this, DependencyContainer.of (this).getDispatchers ()));

			// Desktops is the authority for how many desktops exist (widgets + pins);
			// wire it now that the app model is loaded and re-derive the desktop row //
			this.desktops = new Desktops (this.widgetHost, this.apps);
			final WidgetsPager vgWidgets = this.viewFinder.get (R.id.vgWidgets);
			vgWidgets.setOccupiedDesktopSupplier (() -> this.desktops.highestOccupiedDesktop ());
			vgWidgets.pagesChanged ();

			EditText etDashSearch = this.viewFinder.get(R.id.etDashSearch);
			LinearLayout llLauncher = this.viewFinder.get(R.id.llLauncher);

			etDashSearch.addTextChangedListener (new SearchTextWatcher (installedApps, this.lenses));
			llLauncher.setOnDragListener (new LauncherDragListener (this.apps));

			this.startLauncherService (false);

			SharedPreferences prefs = this.getSharedPreferences ();

			// Load selected icon pack before caching icons
			try {
				final String iconPack = prefs.getString(be.robinj.distrohopper.preferences.Preference.ICON_PACK.getName(), "");
				if (!iconPack.isEmpty()) {
					installedApps.loadIconPack(iconPack);
				}
			} catch (Exception ex) {
				new ExceptionHandler(ex).logAndTrack();
			}

			if (prefs.getBoolean (Preference.LAUNCHER_SHOW_RUNNING_APPS.getName(), false))
				this.apps.addRunningApps (this.dash.getChameleonicBgColour ());

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

			// Package broadcasts only cover the personal profile; work-profile
			// installs/removals arrive through LauncherApps callbacks instead //
			this.workProfileAppsCallback = new WorkProfileAppsCallback (this);
			((LauncherApps) this.getSystemService (Context.LAUNCHER_APPS_SERVICE))
					.registerCallback (this.workProfileAppsCallback,
							new Handler (Looper.getMainLooper ()));
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
			this.dash.setChameleonicBgColour (this.wallpaperColourApplier.apply (wpWallpaper));
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

		// Pins can keep a desktop alive (and pinning on the trailing empty one
		// spawns a new desktop), so re-derive the desktop row //
		this.viewFinder.<WidgetsPager>get (R.id.vgWidgets).pagesChanged ();

		SharedPreferences prefs = this.getSharedPreferences ();

		if (prefs.getBoolean (Preference.LAUNCHER_SHOW_RUNNING_APPS.getName(), false))
			this.apps.addRunningApps (this.dash.getChameleonicBgColour ());
	}

	//# Event handlers #//
	public void lalBfb_clicked (View view)
	{
		try
		{
			if (this.llDash.getVisibility() == View.VISIBLE) {
				this.closeDash();
			} else {
				this.openDash();
			}
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
			if (this.startupLoader != null)
				this.startupLoader.cancel ();

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
	public void closeDash ()
	{
		if (! this.dash.isOpen ()) {
			return;
		}

		if (DependencyContainer.of (this).getCustomiseMode ().getValue ())
		{
			Intent intent = this.getIntent ();
			intent.putExtra ("customise", false);

			this.finish ();
			this.startActivity (intent);

			return;
		}

		this.viewModel.closeDash ();
		this.dash.close ();
	}

	public void openDash ()
	{
		// Pick up launches since the dash was last built, so the usage-based sort
		// orders ("most recently used"/"most used") aren't stale on open //
		if (this.apps != null)
			this.apps.refreshDashSortOrder ();

		this.viewModel.openDash ();
		this.dash.open ();
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

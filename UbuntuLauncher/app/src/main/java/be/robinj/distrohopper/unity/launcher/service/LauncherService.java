package be.robinj.distrohopper.unity.launcher.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import java.util.List;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.R;

/**
 * Created by robin on 8/27/14.
 */
public class LauncherService extends Service
{
	private WindowManager wm;
	private TouchListener touchListener;
	private LinearLayout layout;
	private LinearLayout llListenerContainer;
	private LinearLayout llListener;
	private LinearLayout llLauncher;
	private LinearLayout llShadow;

	private int chameleonicBgColour;
	private List<App> pinned;

	@Override
	public IBinder onBind (Intent intent)
	{
		return null;
	}

	@Override
	public void onCreate ()
	{
		super.onCreate ();

		this.wm = (WindowManager) this.getSystemService (WINDOW_SERVICE);

		LayoutInflater inflater = (LayoutInflater) this.getSystemService (Service.LAYOUT_INFLATER_SERVICE);
		this.layout = (LinearLayout) inflater.inflate (R.layout.service_launcher, null, false);

		this.llLauncher = (LinearLayout) this.layout.findViewById (R.id.llLauncher);
		this.llListenerContainer = (LinearLayout) this.layout.findViewById (R.id.llListenerContainer);
		this.llListener = (LinearLayout) this.layout.findViewById (R.id.llListener);
		this.llShadow = (LinearLayout) this.layout.findViewById (R.id.llShadow);

		WindowManager.LayoutParams params = new WindowManager.LayoutParams(
			WindowManager.LayoutParams.WRAP_CONTENT,
			WindowManager.LayoutParams.MATCH_PARENT,
			WindowManager.LayoutParams.TYPE_PHONE,
			WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
			PixelFormat.TRANSLUCENT);

		params.gravity = Gravity.TOP | Gravity.LEFT;
		params.x = 0;
		params.y = 0;

		be.robinj.distrohopper.unity.launcher.AppLauncher lalBfb = (be.robinj.distrohopper.unity.launcher.AppLauncher) this.layout.findViewById (R.id.lalBfb);
		lalBfb.setColour (R.color.transparentblack80);
		lalBfb.init ();

		SharedPreferences prefs = this.getSharedPreferences ("prefs", MODE_PRIVATE);
		boolean right = prefs.getString ("launcher_edge", "left").equals ("right");

		this.touchListener = new TouchListener (this, right);
		if (right)
		{
			params.gravity = Gravity.TOP | Gravity.RIGHT;

			this.layout.removeView (this.llLauncher);
			this.layout.removeView (this.llListenerContainer);
			this.layout.removeView (this.llShadow);

			this.llShadow.setBackgroundResource (R.drawable.launcherservice_shadow_right);

			this.layout.addView (this.llShadow);
			this.layout.addView (this.llListenerContainer);
			this.layout.addView (this.llLauncher);
		}

		lalBfb.setOnTouchListener (this.touchListener);
		this.llListener.setOnTouchListener (this.touchListener);
		this.llShadow.setOnTouchListener (this.touchListener);

		/*if (Build.VERSION.SDK_INT >= 11)
		{
			LayoutTransition layout_transition = new LayoutTransition ();
			layout_transition.setStartDelay (LayoutTransition.APPEARING, 0);
			layout_transition.setStartDelay (LayoutTransition.DISAPPEARING, 0);
			this.layout.setLayoutTransition (layout_transition);
		}*/

		this.wm.addView (this.layout, params);
	}

	@Override
	public int onStartCommand (Intent intent, int flags, int id)
	{
		if (intent != null) // http://stackoverflow.com/a/20686768/521361 //
		{
			LinearLayout llLauncherPinnedApps = (LinearLayout) this.llLauncher.findViewById (R.id.llLauncherPinnedApps);

			if (intent.getBooleanExtra ("start", false))
			{
				be.robinj.distrohopper.unity.launcher.AppLauncher lalBfb = (be.robinj.distrohopper.unity.launcher.AppLauncher) this.llLauncher.findViewById (R.id.lalBfb);
				lalBfb.init ();

				this.chameleonicBgColour = intent.getIntExtra ("bgColour", Color.argb (40, 40, 40, 40));

				this.llLauncher.setBackgroundColor (this.chameleonicBgColour);
				lalBfb.setColour (intent.getIntExtra ("colour", Color.argb (40, 40, 40, 40)));

				List<App> pinned = intent.getParcelableArrayListExtra ("pinned");
				llLauncherPinnedApps.removeAllViews ();

				for (App app : pinned)
				{
					app.fixAfterUnpackingFromParcel (this);
					AppLauncher appLauncher = new AppLauncher (this, app);

					appLauncher.setOnClickListener (new AppLauncherClickListener ());
					appLauncher.setTag (app.getPackageName () + "\n" + app.getActivityName ());

					llLauncherPinnedApps.addView (appLauncher);
				}

				this.pinned = pinned;
			}

			this.layout.setVisibility (intent.getBooleanExtra ("show", true) ? View.VISIBLE : View.GONE);
			if (! intent.getBooleanExtra ("visible", false))
				this.swipeLeft ();

			for (int i = 0; i < llLauncherPinnedApps.getChildCount (); i++)
				((AppLauncher) llLauncherPinnedApps.getChildAt (i)).setRunning (false);

			List<App> running = intent.getParcelableArrayListExtra ("running");
			if (running != null)
			{
				LinearLayout llLauncherRunningApps = (LinearLayout) this.llLauncher.findViewById (R.id.llLauncherRunningApps);
				llLauncherRunningApps.removeAllViews ();

				for (App app : running)
				{
					app.fixAfterUnpackingFromParcel (this);

					if (this.isPinned (app))
					{
						AppLauncher appLauncher = (AppLauncher) llLauncherPinnedApps.findViewWithTag (app.getPackageName () + "\n" + app.getActivityName ());
						appLauncher.setRunning (true);
					}
					else
					{
						RunningAppLauncher appLauncher = new RunningAppLauncher (this, app);

						appLauncher.setOnClickListener (new AppLauncherClickListener ());
						appLauncher.setColour (this.chameleonicBgColour);

						llLauncherRunningApps.addView (appLauncher);
					}
				}
			}
		}

		return super.onStartCommand (intent, flags, id);
	}

	private boolean isPinned (App app) // this.pinned.contains () won't work because it's a different instance //
	{
		for (App pinnedApp : this.pinned)
		{
			if (app.getPackageName ().equals (pinnedApp.getPackageName ()) && app.getActivityName ().equals (pinnedApp.getActivityName ()))
				return true;
		}

		return false;
	}

	@Override
	public void onDestroy ()
	{
		super.onDestroy ();

		this.wm.removeView (this.layout);
	}

	//# Event handlers #//
	public void lalBfb_clicked (View view)
	{
		//ComponentName compName = new ComponentName ("be.robinj.distrohopper", "be.robinj.distrohopper.HomeActivity");
		Intent intent = new Intent (Intent.ACTION_MAIN);
		intent.addCategory (Intent.CATEGORY_HOME);
		intent.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra ("openDash", true);

		this.startActivity (intent);
	}

	public void swipeRight ()
	{
		this.llLauncher.setVisibility (View.VISIBLE);
		this.llShadow.setVisibility (View.VISIBLE);
		this.llListenerContainer.setVisibility (View.GONE);
	}

	public void swipeLeft ()
	{
		this.llLauncher.setVisibility (View.GONE);
		this.llShadow.setVisibility (View.GONE);
		this.llListenerContainer.setVisibility (View.VISIBLE);
	}
}
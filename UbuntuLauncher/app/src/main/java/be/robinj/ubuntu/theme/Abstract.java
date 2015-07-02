package be.robinj.ubuntu.theme;

import be.robinj.ubuntu.R;

/**
 * Created by robin on 21/01/15.
 */
public class Abstract extends Theme
{
	public Abstract ()
	{
		this.name = "Abstract";
		this.description = "Ugly experiment...";
		this.res_screenshots = new int[] {};

		// Launcher //
		this.launcher_background_dynamic = R.bool.theme_abstract_launcher_background_dynamic;
		this.launcher_background = R.drawable.theme_abstract_launcher_background;
		this.launcher_bfb_location = R.integer.theme_abstract_launcher_bfb_location;
		this.launcher_bfb_image = R.drawable.theme_abstract_launcher_bfb_image;
		this.launcher_preferences_location = R.integer.theme_abstract_launcher_preferences_location;
		this.launcher_preferences_image = R.drawable.theme_abstract_launcher_preferences_image;
		this.launcher_applauncher_backgroundcolour_dynamic = R.bool.theme_abstract_launcher_applauncher_backgroundcolour_dynamic;
		this.launcher_applauncher_backgroundcolour = R.color.theme_abstract_launcher_applauncher_backgroundcolour;
		this.launcher_applauncher_background = R.drawable.theme_abstract_launcher_applauncher_background;
		this.launcher_applauncher_gradient = R.drawable.theme_abstract_launcher_applauncher_gradient;
		this.launcher_applauncher_running = R.drawable.theme_abstract_launcher_applauncher_running;

		// Panel //
		this.panel_location = R.integer.theme_abstract_panel_location;
		this.panel_background = R.drawable.theme_abstract_panel_background;
		this.panel_close_location = R.integer.theme_abstract_panel_close_location;
		this.panel_close_image = R.drawable.theme_abstract_panel_close_image;
		this.panel_preferences_location = R.integer.theme_abstract_panel_preferences_location;
		this.panel_preferences_image = R.drawable.theme_abstract_panel_preferences_image;

		// Dash //
		this.dash_background_gradient = R.drawable.theme_abstract_dash_background_gradient;
	}
}

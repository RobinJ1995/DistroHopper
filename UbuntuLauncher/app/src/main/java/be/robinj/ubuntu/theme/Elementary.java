package be.robinj.ubuntu.theme;

import be.robinj.ubuntu.R;

/**
 * Created by robin on 21/01/15.
 */
public class Elementary extends Theme
{
	public Elementary ()
	{
		// Launcher //
		this.launcher_location = R.integer.theme_elementary_launcher_location;
		this.launcher_margin = R.array.theme_elementary_launcher_margin;
		this.launcher_background_dynamic = R.bool.theme_elementary_launcher_background_dynamic;
		this.launcher_background = R.drawable.theme_elementary_launcher_background;
		this.launcher_bfb_location = R.integer.theme_elementary_launcher_bfb_location;
		this.launcher_bfb_image = R.drawable.theme_elementary_launcher_bfb_image;
		this.launcher_preferences_location = R.integer.theme_elementary_launcher_preferences_location;
		this.launcher_preferences_image = R.drawable.theme_elementary_launcher_preferences_image;
		this.launcher_applauncher_backgroundcolour_dynamic = R.bool.theme_elementary_launcher_applauncher_backgroundcolour_dynamic;
		this.launcher_applauncher_backgroundcolour = R.color.theme_elementary_launcher_applauncher_backgroundcolour;
		this.launcher_applauncher_background = R.drawable.theme_elementary_launcher_applauncher_background;
		this.launcher_applauncher_gradient = R.drawable.theme_elementary_launcher_applauncher_gradient;
		this.launcher_applauncher_running = R.drawable.theme_elementary_launcher_applauncher_running;

		// Panel //
		this.panel_location = R.integer.theme_elementary_panel_location;
		this.panel_height = R.dimen.theme_default_panel_height;
		this.panel_background = R.drawable.theme_elementary_panel_background;
		this.panel_background_dynamic_if_dash_opened = R.bool.theme_elementary_panel_background_dynamic_if_dash_opened;
		this.panel_bfb_location = R.integer.theme_elementary_panel_bfb_location;
		this.panel_bfb_text = R.string.theme_elementary_panel_bfb_text;
		this.panel_bfb_text_colour = R.color.theme_elementary_panel_bfb_text_colour;
		this.panel_close_location = R.integer.theme_elementary_panel_close_location;
		this.panel_close_image = R.drawable.theme_elementary_panel_close_image;
		this.panel_preferences_location = R.integer.theme_elementary_panel_preferences_location;
		this.panel_preferences_image = R.drawable.theme_elementary_panel_preferences_image;

		// Dash //
		this.dash_background_gradient = R.drawable.theme_elementary_dash_background_gradient;
		this.dash_background_dynamic = R.bool.theme_elementary_dash_background_dynamic;
		this.dash_background = R.drawable.theme_elementary_dash_background;
		this.dash_applauncher_text_colour = R.color.theme_elementary_dash_applauncher_text_colour;
		this.dash_applauncher_text_shadow_colour = R.color.theme_elementary_dash_applauncher_text_shadow_colour;
	}
}

package be.robinj.distrohopper.theme;

import be.robinj.distrohopper.R;

/**
 * Created by robin on 21/01/15.
 */
public class Cinnamon extends Theme
{
	public Cinnamon()
	{
		this.name = "Cinnamon";
		this.description = "Linux Mint's Cinnamon desktop";

		this.wallpaper_overlay = R.drawable.theme_cinnamon_wallpaper_overlay;
		this.wallpaper_overlay_when_dash_opened = R.drawable.theme_cinnamon_wallpaper_overlay_when_dash_opened;

		// Launcher //
		this.launcher_location = R.integer.theme_cinnamon_launcher_location;
		this.launcher_location_supported = R.array.theme_cinnamon_launcher_location_supported;
		this.launcher_margin = R.array.theme_cinnamon_launcher_margin;
		this.launcher_expand = R.bool.theme_cinnamon_launcher_expand;
		this.launcher_background_dynamic = R.bool.theme_cinnamon_launcher_background_dynamic;
		this.launcher_background = R.array.theme_cinnamon_launcher_background;
		this.launcher_bfb_location = R.integer.theme_cinnamon_launcher_bfb_location;
		this.launcher_bfb_image = R.drawable.theme_cinnamon_launcher_bfb_image;
		this.launcher_bfb_hide_while_dragging = R.bool.theme_cinnamon_launcher_bfb_hide_while_dragging;
		this.launcher_preferences_location = R.integer.theme_cinnamon_launcher_preferences_location;
		this.launcher_preferences_image = R.drawable.theme_cinnamon_launcher_preferences_image;
		this.launcher_preferences_location_when_panel_hidden = R.integer.theme_cinnamon_launcher_preferences_location_when_panel_hidden;
		this.launcher_trash_image = R.drawable.theme_cinnamon_launcher_trash_image;
		this.launcher_applauncher_backgroundcolour_dynamic = R.bool.theme_cinnamon_launcher_applauncher_backgroundcolour_dynamic;
		this.launcher_applauncher_backgroundcolour = R.color.theme_cinnamon_launcher_applauncher_backgroundcolour;
		this.launcher_applauncher_background = R.drawable.theme_cinnamon_launcher_applauncher_background;
		this.launcher_applauncher_gradient = R.drawable.theme_cinnamon_launcher_applauncher_gradient;
		this.launcher_applauncher_running = R.drawable.theme_cinnamon_launcher_applauncher_running;
		this.launcher_applauncher_running_backgroundcolour_dynamic = R.bool.theme_cinnamon_launcher_applauncher_running_backgroundcolour_dynamic;
		this.launcher_applauncher_running_backgroundcolour = R.color.theme_cinnamon_launcher_applauncher_running_backgroundcolour;

		// Panel //
		this.panel_location = R.integer.theme_cinnamon_panel_location;
		this.panel_location_supported = R.array.theme_cinnamon_panel_location_supported;
		this.panel_height = R.dimen.theme_cinnamon_panel_height;
		this.panel_background = R.drawable.theme_cinnamon_panel_background;
		this.panel_background_dynamic_when_dash_opened = R.bool.theme_cinnamon_panel_background_dynamic_when_dash_opened;
		this.panel_bfb_location = R.integer.theme_cinnamon_panel_bfb_location;
		this.panel_bfb_text = R.string.theme_cinnamon_panel_bfb_text;
		this.panel_bfb_text_colour = R.color.theme_cinnamon_panel_bfb_text_colour;
		this.panel_close_location = R.integer.theme_cinnamon_panel_close_location;
		this.panel_close_image = R.drawable.theme_cinnamon_panel_close_image;
		this.panel_preferences_location = R.integer.theme_cinnamon_panel_preferences_location;
		this.panel_preferences_image = R.drawable.theme_cinnamon_panel_preferences_image;
		this.panel_swap_close_preferences_when_launcher_location = R.array.theme_cinnamon_panel_swap_close_preferences_when_launcher_location;

		// Dash //
		this.dash_background_gradient = R.drawable.theme_cinnamon_dash_background_gradient;
		this.dash_background_dynamic = R.bool.theme_cinnamon_dash_background_dynamic;
		this.dash_background = R.drawable.theme_cinnamon_dash_background;
		this.dash_applauncher_text_colour = R.color.theme_cinnamon_dash_applauncher_text_colour;
		this.dash_applauncher_text_shadow_colour = R.color.theme_cinnamon_dash_applauncher_text_shadow_colour;
		this.dash_customise_text_colour = R.color.theme_cinnamon_dash_customise_text_colour;
		this.dash_customise_text_shadow_colour = R.color.theme_cinnamon_dash_customise_text_shadow_colour;
		this.dash_customise_spinner_text_colour = R.color.theme_cinnamon_dash_customise_spinner_text_colour;
		this.dash_search_background = R.drawable.theme_cinnamon_dash_search_background;
		this.dash_search_text_colour = R.color.theme_cinnamon_dash_search_text_colour;
		this.dash_ribbon_show = R.bool.theme_cinnamon_dash_ribbon_show;
	}
}

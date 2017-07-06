package be.robinj.distrohopper.theme;

/**
 * Created by robin on 21/01/15.
 */
public abstract class Theme
{
	public String name;
	public String description;
	public int[] res_screenshots;
	public boolean dev_only = false;

	public int wallpaper_overlay;
	public int wallpaper_overlay_when_dash_opened;

	// Launcher //
	public int launcher_location;
	public int launcher_location_supported;
	public int launcher_margin;
	public int launcher_expand;
	public int launcher_background_dynamic;
	public int launcher_background;
	public int launcher_bfb_location;
	public int launcher_bfb_image;
	public int launcher_preferences_location;
	public int launcher_preferences_image;
	public int launcher_preferences_location_when_panel_hidden;
	public int launcher_trash_image;
	public int launcher_applauncher_backgroundcolour_dynamic;
	public int launcher_applauncher_backgroundcolour;
	public int launcher_applauncher_background;
	public int launcher_applauncher_gradient;
	public int launcher_applauncher_running;
	public int launcher_applauncher_running_backgroundcolour_dynamic;
	public int launcher_applauncher_running_backgroundcolour;

	// Panel //
	public int panel_location;
	public int panel_height;
	public int panel_background;
	public int panel_background_dynamic_when_dash_opened;
	public int panel_bfb_location;
	public int panel_bfb_text;
	public int panel_bfb_text_colour;
	public int panel_close_location;
	public int panel_close_image;
	public int panel_preferences_location;
	public int panel_preferences_image;
	public int panel_swap_close_preferences_when_launcher_location;

	// Dash //
	public int dash_background_gradient;
	public int dash_background_dynamic;
	public int dash_background;
	public int dash_applauncher_text_colour;
	public int dash_applauncher_text_shadow_colour;
	public int dash_search_background;
	public int dash_search_text_colour;
	public int dash_ribbon_show;
	
	public String getName ()
	{
		return this.getClass ().getSimpleName ().toLowerCase ();
	}
}

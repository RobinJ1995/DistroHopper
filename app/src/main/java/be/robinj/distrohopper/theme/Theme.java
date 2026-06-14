package be.robinj.distrohopper.theme;

import android.content.SharedPreferences;
import android.content.res.Resources;

import be.robinj.distrohopper.preferences.Preference;

/**
 * Created by robin on 21/01/15.
 */
public abstract class Theme
{
	public String name;
	public String description;
	public boolean dev_only = false;

	/*
	 * Panel-less themes (Budgie) have no panel to drive the status bar, so the
	 * status bar instead follows the launcher edge: opaque (statusbar_background)
	 * when the launcher is at the top, transparent otherwise. Off by default so
	 * panelled themes keep the panel-based resolution below.
	 */
	public boolean statusbar_follows_launcher_edge = false;

	/** The distro's brand/accent colour (used by the theme picker cards). */
	public int card_colour;

	/**
	 * The distro's logo as shown on the theme picker cards; usually the BFB
	 * image, but themes whose BFB is dark provide a light variant so it stays
	 * visible on the cards' dark background.
	 */
	public int card_logo;

	public int wallpaper_overlay;
	public int wallpaper_overlay_when_dash_opened;
	public int dynamic_background_opacity;

	// Launcher //
	public int launcher_location;
	public int launcher_location_supported;
	public int launcher_margin;
	public int launcher_expand;
	public int launcher_background_dynamic;
	public int launcher_background;
	public int launcher_bfb_location;
	public int launcher_bfb_image;
	public int launcher_bfb_image_vertical;
	public int launcher_bfb_hide_while_dragging;
	/*
	 * Whether the launcher's menu button (the "BFB") can be shown or hidden by
	 * the user in customise mode, and its default state when it can. Themes that
	 * are not toggleable always follow launcher_bfb_location.
	 */
	public int launcher_bfb_user_toggleable;
	public int launcher_bfb_visible_by_default;
	public int launcher_preferences_location;
	public int launcher_preferences_image;
	public int launcher_preferences_location_when_panel_hidden;
	public int launcher_trash_image;
	public int launcher_applauncher_backgroundcolour_dynamic;
	public int launcher_applauncher_backgroundcolour;
	public int launcher_applauncher_backgroundcolour_opacity;
	public int launcher_applauncher_margin;
	public int launcher_applauncher_margin_edge;
	public int launcher_applauncher_background;
	public int launcher_applauncher_gradient;
	public int launcher_applauncher_running;
	public int launcher_applauncher_running_backgroundcolour_dynamic;
	public int launcher_applauncher_running_backgroundcolour;

	// Panel //
	public int panel_location;
	public int panel_location_supported;
	public int panel_height;
	public int panel_background;
	public int panel_background_when_dash_opened;
	public int statusbar_background;
	public int statusbar_background_when_panel_not_top;
	public int statusbar_background_when_dash_opened;
	public int panel_background_dynamic_when_dash_opened;
	public int panel_bfb_location;
	public int panel_bfb_image;
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
	/*
	 * Optional per-launcher-edge dash backgrounds, as a 5-element drawable
	 * array indexed by Location.n ([none, top, right, bottom, left]); used by
	 * themes whose dash carries a directional element such as Budgie's ear,
	 * which must point at the BFB on whichever edge the launcher sits. 0 (the
	 * default) means the single dash_background is used for every edge.
	 */
	public int dash_background_edge;
	public int dash_applauncher_text_colour;
	public int dash_applauncher_text_shadow_colour;
	public int dash_customise_text_colour;
	public int dash_customise_text_shadow_colour;
	public int dash_customise_spinner_text_colour;
	public int dash_search_background;
	public int dash_search_width;
	public int dash_search_text_colour;
	public int dash_ribbon_show;
	public int dash_blur_radius;
	public int dash_animation;
	/** Integer mapping to ProfileIndicatorStyle; see profile_indicator.xml. */
	public int profile_indicator;
	/**
	 * Drawable for the personal profile's glyph in a glyph-based indicator (the
	 * Unity ribbon); other profiles use the badged base glyph. Defaults to the
	 * generic profile glyph; Unity overrides it to its house glyph.
	 */
	public int profile_indicator_personal_glyph;
	
	public String getName ()
	{
		return this.getClass ().getSimpleName ().toLowerCase ();
	}

	/*
	 * Whenever the panel does not sit at the top of the screen (hidden, or
	 * moved to another edge by the complementary placement rule), themes can
	 * choose a different status bar background (COSMIC goes transparent).
	 */
	public int statusbar_background_resolved (final Resources res, final SharedPreferences prefs)
	{
		int panelEdge = prefs.getInt (Preference.PANEL_EDGE.getName (),
				res.getInteger (this.panel_location));

		if (panelEdge != Location.NONE.n)
		{
			boolean supportsBottom = false;
			for (final int supported : res.getIntArray (this.panel_location_supported))
				supportsBottom |= supported == Location.BOTTOM.n;

			if (supportsBottom)
			{
				final int launcherEdge = prefs.getInt (Preference.LAUNCHER_EDGE.getName (),
						res.getInteger (this.launcher_location));
				panelEdge = launcherEdge == Location.TOP.n ? Location.BOTTOM.n : Location.TOP.n;
			}
		}
		else if (this.statusbar_follows_launcher_edge)
		{
			final int launcherEdge = prefs.getInt (Preference.LAUNCHER_EDGE.getName (),
					res.getInteger (this.launcher_location));
			return launcherEdge == Location.TOP.n
					? this.statusbar_background : this.statusbar_background_when_panel_not_top;
		}

		if (panelEdge != Location.TOP.n)
			return this.statusbar_background_when_panel_not_top;

		return this.statusbar_background;
	}

	/*
	 * Whether the launcher's menu button (BFB) should be shown. For themes that
	 * let the user toggle it (Pantheon, COSMIC) this follows the user's choice,
	 * falling back to the theme's default; other themes simply follow whether
	 * their themed BFB location is set at all.
	 */
	public boolean launcherBfbVisible(final Resources res, final SharedPreferences prefs) {
		if (res.getBoolean(this.launcher_bfb_user_toggleable)) {
			return prefs.getBoolean(Preference.LAUNCHER_MENU_BUTTON_VISIBLE.getName(),
					res.getBoolean(this.launcher_bfb_visible_by_default));
		}

		return Location.of(res.getInteger(this.launcher_bfb_location)) != Location.NONE;
	}

	public Location lalPreferences_getLocation(final Resources res, final SharedPreferences prefs) {
		if (prefs.getInt(Preference.PANEL_EDGE.getName(), Location.TOP.n) == Location.NONE.n) {
			return Location.of(res.getInteger (this.launcher_preferences_location_when_panel_hidden));
		}

		return Location.of(res.getInteger(this.launcher_preferences_location));
	}
}

package be.robinj.distrohopper.theme;

import android.content.SharedPreferences;
import android.content.res.Resources;

import be.robinj.distrohopper.preferences.BfbLocation;
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
	 * when the launcher is at the top, transparent otherwise. Off (R.bool false)
	 * for panelled themes, which keep the panel-based resolution below.
	 */
	public int statusbar_follows_launcher_edge;

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
	 * The launcher menu button (BFB) positions this theme offers, as a position_*
	 * int array (each mapped to a none/start/end side by bfbSide), mirroring
	 * launcher_location_supported. A theme is user-toggleable — and shows the
	 * customise-mode menu-button dropdown — only when it lists more than one
	 * position (see launcherBfbToggleable); otherwise the BFB always follows the
	 * theme's native launcher_bfb_location. launcher_bfb_visible_by_default is its
	 * default state when it is toggleable.
	 */
	public int launcher_bfb_location_supported;
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
	/*
	 * The status bar background while the panel is hidden (panel edge None), on
	 * panelled themes that can hide their panel (Unity, GNOME). Lets a theme blend
	 * the status bar with its launcher chrome instead of keeping the panelled
	 * status_background; resolved by statusbar_background_resolved.
	 */
	public int statusbar_colour_when_panel_hidden;
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
		else if (res.getBoolean (this.statusbar_follows_launcher_edge))
		{
			final int launcherEdge = prefs.getInt (Preference.LAUNCHER_EDGE.getName (),
					res.getInteger (this.launcher_location));
			return launcherEdge == Location.TOP.n
					? this.statusbar_background : this.statusbar_background_when_panel_not_top;
		}
		else
		{
			// Panel hidden on a panelled theme: its own panel-hidden background.
			return this.statusbar_colour_when_panel_hidden;
		}

		if (panelEdge != Location.TOP.n)
			return this.statusbar_background_when_panel_not_top;

		return this.statusbar_background;
	}

	/*
	 * Where the launcher's menu button (BFB) should sit. For themes that let the
	 * user move/hide it (those offering more than one launcher_bfb_location_supported
	 * position, e.g. Pantheon, COSMIC, GNOME) this follows the user's choice of
	 * none/start/end, falling back to the theme's default; other themes always
	 * use their fixed themed BFB location.
	 */
	public Location launcherBfbLocationResolved(final Resources res, final SharedPreferences prefs) {
		final Location nativeLocation = Location.of(res.getInteger(this.launcher_bfb_location));

		if (! this.launcherBfbToggleable(res))
			return nativeLocation;

		final BfbLocation choice;
		final String stored = prefs.getString(Preference.LAUNCHER_BFB_LOCATION.getName(), null);
		if (stored == null)
			choice = res.getBoolean(this.launcher_bfb_visible_by_default)
					? this.bfbSide(nativeLocation) : BfbLocation.NONE;
		else
			choice = BfbLocation.of(stored);

		switch (choice) {
			case START:
				return this.isStartSide(nativeLocation) ? nativeLocation : this.opposite(nativeLocation);
			case END:
				return this.isStartSide(nativeLocation) ? this.opposite(nativeLocation) : nativeLocation;
			case NONE:
			default:
				return Location.NONE;
		}
	}

	/** The user-facing default side for this (toggleable) theme: none, or its native side. */
	public BfbLocation launcherBfbDefaultChoice(final Resources res) {
		if (! res.getBoolean(this.launcher_bfb_visible_by_default))
			return BfbLocation.NONE;

		return this.bfbSide(Location.of(res.getInteger(this.launcher_bfb_location)));
	}

	/** Whether the BFB is shown at all; thin wrapper over the resolved location. */
	public boolean launcherBfbVisible(final Resources res, final SharedPreferences prefs) {
		return this.launcherBfbLocationResolved(res, prefs) != Location.NONE;
	}

	/**
	 * Whether the user can move/hide the BFB in customise mode: true when the theme
	 * offers more than one position (mirrors how launcher_location_supported drives
	 * the launcher-edge dropdown).
	 */
	public boolean launcherBfbToggleable(final Resources res) {
		return res.getIntArray(this.launcher_bfb_location_supported).length > 1;
	}

	/** The launcher's leading edge (top of a vertical bar, left of a horizontal one). */
	private boolean isStartSide(final Location location) {
		return location == Location.TOP || location == Location.LEFT;
	}

	/** The BFB side (none/start/end) a position_* location maps to. */
	public BfbLocation bfbSide(final Location location) {
		if (location == Location.NONE)
			return BfbLocation.NONE;

		return this.isStartSide(location) ? BfbLocation.START : BfbLocation.END;
	}

	private Location opposite(final Location location) {
		switch (location) {
			case TOP: return Location.BOTTOM;
			case BOTTOM: return Location.TOP;
			case LEFT: return Location.RIGHT;
			case RIGHT: return Location.LEFT;
			default: return Location.NONE;
		}
	}

	public Location lalPreferences_getLocation(final Resources res, final SharedPreferences prefs) {
		if (prefs.getInt(Preference.PANEL_EDGE.getName(), Location.TOP.n) == Location.NONE.n) {
			return Location.of(res.getInteger (this.launcher_preferences_location_when_panel_hidden));
		}

		return Location.of(res.getInteger(this.launcher_preferences_location));
	}
}

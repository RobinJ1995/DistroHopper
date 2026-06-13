package be.robinj.distrohopper.theme;

/**
 * How a theme renders the dash's profile (profile) tab indicator, chosen
 * per theme via the theme_*.xml profile_indicator integer (see
 * profile_indicator.xml for the values). Only relevant when more than one
 * profile exists; with a single profile the dash shows the plain grid and
 * no indicator regardless of this choice.
 */
public enum ProfileIndicatorStyle
{
	NONE(0), // No indicator yet (themes pending a follow-up) //
	UNITY_RIBBON(1), // Per-profile glyphs in the always-visible dash ribbon //
	GNOME_PANEL(2); // A profile pill at the panel's top-left, shown while the dash is open //

	public final int n;

	ProfileIndicatorStyle(final int n) {
		this.n = n;
	}

	public static ProfileIndicatorStyle of(final int n) {
		return ProfileIndicatorStyle.values()[n];
	}
}

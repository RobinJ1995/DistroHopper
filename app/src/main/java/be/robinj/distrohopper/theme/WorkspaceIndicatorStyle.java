package be.robinj.distrohopper.theme;

/**
 * How a theme renders the dash's workspace (profile) tab indicator, chosen
 * per theme via the theme_*.xml workspace_indicator integer (see
 * workspace_indicator.xml for the values). Only relevant when more than one
 * workspace exists; with a single workspace the dash shows the plain grid and
 * no indicator regardless of this choice.
 */
public enum WorkspaceIndicatorStyle
{
	NONE(0), // No indicator yet (themes pending a follow-up) //
	UNITY_RIBBON(1), // Per-profile glyphs in the always-visible dash ribbon //
	GNOME_PANEL(2); // A workspace pill at the panel's top-left, shown while the dash is open //

	public final int n;

	WorkspaceIndicatorStyle(final int n) {
		this.n = n;
	}

	public static WorkspaceIndicatorStyle of(final int n) {
		return WorkspaceIndicatorStyle.values()[n];
	}
}

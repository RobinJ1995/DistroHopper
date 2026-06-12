package be.robinj.distrohopper.theme;

/**
 * Dash open/close animation, chosen per theme via the theme_*.xml
 * dash_animation integer (see theme_dash_animation.xml for the values).
 * The wallpaper/widget blur always ramps gradually, regardless of this choice.
 */
public enum DashAnimation
{
	NONE(0),
	GNOME(1), // Dim fades in, icons genie out of the BFB //
	CINNAMON(2), // Dash slides in from the launcher's edge of the screen //
	ELEMENTARY(3), // Dash fades and zooms in from the Applications label //
	UNITY(4), // Dash and panel changes fade //
	MATE(5), // The whole dash genies out of the BFB //
	COSMIC(6); // The dash fades in with a slight zoom //

	public final int n;

	DashAnimation(final int n) {
		this.n = n;
	}

	@Override
	public String toString() {
		return this.name();
	}

	public static DashAnimation of(final int n) {
		return DashAnimation.values()[n];
	}
}

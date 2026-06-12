package be.robinj.distrohopper.theme;

/**
 * Dash open/close animation, chosen per theme via the theme_*.xml
 * dash_animation integer (see theme_dash_animation.xml for the values).
 */
public enum DashAnimation
{
	NONE(0),
	GENIE(1); // Dim fades in, blur ramps up, icons expand out of the BFB //

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

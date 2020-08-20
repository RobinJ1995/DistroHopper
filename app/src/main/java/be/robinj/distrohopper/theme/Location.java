package be.robinj.distrohopper.theme;

/**
 * Created by robin on 21/01/15.
 */
public enum Location
{
	NONE(0),
	TOP(1),
	RIGHT(2),
	BOTTOM(3),
	LEFT(4);

	public final int n;

	Location(final int n) {
		this.n = n;
	}

	@Override
	public String toString() {
		return this.name();
	}

	public static Location of(final int n) {
		return Location.values()[n];
	}
}

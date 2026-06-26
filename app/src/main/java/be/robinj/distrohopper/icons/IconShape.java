package be.robinj.distrohopper.icons;

/**
 * The mask shape the launcher clips adaptive icons to. {@link #SYSTEM} defers to
 * the device-configured mask (the same shape the system launcher uses); the
 * others force a specific silhouette regardless of the device default.
 */
public enum IconShape {
	SYSTEM("system"),
	CIRCLE("circle"),
	SQUIRCLE("squircle"),
	ROUNDED_SQUARE("rounded_square"),
	SQUARE("square");

	private final String preferenceValue;

	IconShape(final String preferenceValue) {
		this.preferenceValue = preferenceValue;
	}

	public String getPreferenceValue() {
		return this.preferenceValue;
	}

	/** Parse a stored preference value, falling back to {@link #SYSTEM} for null/unknown. */
	public static IconShape fromPreferenceValue(final String value) {
		if (value != null) {
			for (final IconShape shape : values()) {
				if (shape.preferenceValue.equals(value)) {
					return shape;
				}
			}
		}

		return SYSTEM;
	}
}

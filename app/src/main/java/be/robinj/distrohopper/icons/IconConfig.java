package be.robinj.distrohopper.icons;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;

import be.robinj.distrohopper.preferences.Preference;
import be.robinj.distrohopper.preferences.PreferencesRepository;

/**
 * An immutable snapshot of everything that affects how {@link IconRenderer}
 * rasterises an adaptive icon: the mask {@link IconShape}, whether monochrome
 * icons are recoloured ("tinted"), the canonical render size, and the tonal
 * background/foreground used for tinted icons.
 *
 * <p>{@link #signature()} folds all of these into a single string so a change to
 * any of them — including the resolved tint colour following the wallpaper or
 * theme — can be detected and used to invalidate the (shape-unaware) icon cache.
 */
public final class IconConfig {
	/** Adaptive icons are authored on a 108dp canvas; render at that size for crisp masking. */
	private static final int CANVAS_DP = 108;

	private final IconShape shape;
	private final boolean tintedIcons;
	private final int sizePx;
	private final int tintColor;
	private final int tintBackground;
	private final int tintForeground;

	public IconConfig(final IconShape shape, final boolean tintedIcons, final int sizePx,
					  final int tintColor, final int tintBackground, final int tintForeground) {
		this.shape = shape;
		this.tintedIcons = tintedIcons;
		this.sizePx = sizePx;
		this.tintColor = tintColor;
		this.tintBackground = tintBackground;
		this.tintForeground = tintForeground;
	}

	public static IconConfig fromPrefs(final Context context) {
		final PreferencesRepository prefs = new PreferencesRepository(context);

		final IconShape shape = IconShape.fromPreferenceValue(
			prefs.getString(Preference.ICON_SHAPE, Preference.ICON_SHAPE.getDefault()));
		final boolean tinted = prefs.getBoolean(Preference.TINTED_ICONS,
			Boolean.TRUE.equals(Preference.TINTED_ICONS.<Boolean>getDefault()));

		final int sizePx = Math.round(CANVAS_DP * context.getResources().getDisplayMetrics().density);

		final int tint = IconTint.resolve(context,
			prefs.getString(Preference.ICON_TINT, Preference.ICON_TINT.getDefault()));
		final boolean night = isNightMode(context);

		return new IconConfig(shape, tinted, sizePx, tint,
			tintBackground(tint, night), tintForeground(tint, night));
	}

	public static boolean isNightMode(final Context context) {
		return (context.getResources().getConfiguration().uiMode
			& Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
	}

	/** The tonal background a tinted icon sits on for [tint] in light/dark mode. */
	public static int tintBackground(final int tint, final boolean night) {
		return tone(tint, night ? 0.45f : 0.18f, night ? 0.30f : 0.95f);
	}

	/** The tonal foreground a tinted icon's glyph is recoloured to for [tint] in light/dark mode. */
	public static int tintForeground(final int tint, final boolean night) {
		return tone(tint, night ? 0.85f : 0.95f, night ? 0.92f : 0.42f);
	}

	/** A tonal variant of [base] keeping its hue but at the given saturation factor and value. */
	private static int tone(final int base, final float saturationFactor, final float value) {
		final float[] hsv = new float[3];
		Color.colorToHSV(base, hsv);
		hsv[1] = Math.min(1f, hsv[1] * saturationFactor);
		hsv[2] = value;

		return Color.HSVToColor(hsv);
	}

	public IconShape getShape() {
		return this.shape;
	}

	public boolean isTintedIcons() {
		return this.tintedIcons;
	}

	public int getSizePx() {
		return this.sizePx;
	}

	public int getTintColor() {
		return this.tintColor;
	}

	public int getTintBackground() {
		return this.tintBackground;
	}

	public int getTintForeground() {
		return this.tintForeground;
	}

	/** Whether tinted rendering can actually run (toggle on, and the platform supports monochrome). */
	public boolean tintedRenderingSupported() {
		return this.tintedIcons && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
	}

	/**
	 * A stable string covering every field that changes rendered output. Used to
	 * detect when cached (shape-unaware) icons no longer match the current config.
	 * The tint colours only count when tinting is on, so a theme/wallpaper change
	 * only invalidates the cache when it actually affects the rendered icons.
	 */
	public String signature() {
		final String base = this.shape.getPreferenceValue()
			+ "|tinted=" + this.tintedIcons
			+ "|sz=" + this.sizePx;

		if (!this.tintedIcons) {
			return base;
		}

		return base
			+ "|tint=" + this.tintColor
			+ "|bg=" + this.tintBackground
			+ "|fg=" + this.tintForeground;
	}
}

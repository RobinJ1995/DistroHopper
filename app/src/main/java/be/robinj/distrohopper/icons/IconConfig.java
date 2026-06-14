package be.robinj.distrohopper.icons;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import androidx.core.content.ContextCompat;

import be.robinj.distrohopper.preferences.Preference;
import be.robinj.distrohopper.preferences.PreferencesRepository;

/**
 * An immutable snapshot of everything that affects how {@link IconRenderer}
 * rasterises an adaptive icon: the mask {@link IconShape}, whether Material You
 * themed (monochrome) rendering is on, the canonical render size, and the
 * accent/background colours used for themed icons.
 *
 * <p>{@link #signature()} folds all of these into a single string so a change to
 * any of them can be detected and used to invalidate the (shape-unaware) icon
 * cache.
 */
public final class IconConfig {
	/** Adaptive icons are authored on a 108dp canvas; render at that size for crisp masking. */
	private static final int CANVAS_DP = 108;

	private final IconShape shape;
	private final boolean themedIcons;
	private final int sizePx;
	private final int accentColor;
	private final int themedBackground;

	public IconConfig(final IconShape shape, final boolean themedIcons, final int sizePx,
					  final int accentColor, final int themedBackground) {
		this.shape = shape;
		this.themedIcons = themedIcons;
		this.sizePx = sizePx;
		this.accentColor = accentColor;
		this.themedBackground = themedBackground;
	}

	public static IconConfig fromPrefs(final Context context) {
		final PreferencesRepository prefs = new PreferencesRepository(context);

		final IconShape shape = IconShape.fromPreferenceValue(
			prefs.getString(Preference.ICON_SHAPE, Preference.ICON_SHAPE.getDefault()));
		final boolean themed = prefs.getBoolean(Preference.THEMED_ICONS,
			Boolean.TRUE.equals(Preference.THEMED_ICONS.<Boolean>getDefault()));

		final int sizePx = Math.round(CANVAS_DP * context.getResources().getDisplayMetrics().density);

		final boolean night = (context.getResources().getConfiguration().uiMode
			& Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

		// Material You themed icons: a tonal foreground over a tonal background that //
		// tracks the light/dark theme, mirroring how the system renders themed icons. //
		final int accent = ContextCompat.getColor(context, night
			? android.R.color.system_accent1_100
			: android.R.color.system_accent1_600);
		final int background = ContextCompat.getColor(context, night
			? android.R.color.system_neutral1_800
			: android.R.color.system_accent1_100);

		return new IconConfig(shape, themed, sizePx, accent, background);
	}

	public IconShape getShape() {
		return this.shape;
	}

	public boolean isThemedIcons() {
		return this.themedIcons;
	}

	public int getSizePx() {
		return this.sizePx;
	}

	public int getAccentColor() {
		return this.accentColor;
	}

	public int getThemedBackground() {
		return this.themedBackground;
	}

	/** Whether themed rendering can actually run (toggle on, and the platform supports monochrome). */
	public boolean themedRenderingSupported() {
		return this.themedIcons && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
	}

	/**
	 * A stable string covering every field that changes rendered output. Used to
	 * detect when cached (shape-unaware) icons no longer match the current config.
	 */
	public String signature() {
		return this.shape.getPreferenceValue()
			+ "|themed=" + this.themedIcons
			+ "|sz=" + this.sizePx
			+ "|acc=" + this.accentColor
			+ "|bg=" + this.themedBackground;
	}
}

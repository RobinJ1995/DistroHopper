package be.robinj.distrohopper.icons;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import be.robinj.distrohopper.DependencyContainer;

/**
 * Resolves the icon-tint preference token to a concrete ARGB colour.
 *
 * <p>The stored value is one of the dynamic tokens {@link #WALLPAPER},
 * {@link #ACCENT}, {@link #THEME}, or an explicit {@code #RRGGBB} preset. The
 * dynamic tokens are resolved live so the tint follows the wallpaper, the system
 * accent, or the active distro theme as those change.
 */
public final class IconTint {
	public static final String WALLPAPER = "wallpaper";
	public static final String ACCENT = "accent";
	public static final String THEME = "theme";

	private IconTint() {
	}

	/** Resolve any tint token (dynamic or {@code #RRGGBB}) to an ARGB colour. */
	public static int resolve(final Context context, final String token) {
		if (TextUtils.isEmpty(token) || WALLPAPER.equals(token)) {
			final Integer wallpaper = wallpaper(context);
			return wallpaper != null ? wallpaper : accent(context);
		} else if (ACCENT.equals(token)) {
			return accent(context);
		} else if (THEME.equals(token)) {
			return theme(context);
		}

		try {
			return Color.parseColor(token);
		} catch (final IllegalArgumentException ex) {
			return accent(context);
		}
	}

	/** The wallpaper's primary colour, or null when the system can't report one (e.g. just after boot). */
	public static Integer wallpaper(final Context context) {
		try {
			final android.app.WallpaperColors colors = WallpaperManager.getInstance(context)
				.getWallpaperColors(WallpaperManager.FLAG_SYSTEM);
			return colors != null ? colors.getPrimaryColor().toArgb() : null;
		} catch (final Exception ex) {
			return null;
		}
	}

	/** The Material You system accent colour (available from API 31, our minimum). */
	public static int accent(final Context context) {
		return ContextCompat.getColor(context, android.R.color.system_accent1_500);
	}

	/** The active distro theme's brand colour. */
	public static int theme(final Context context) {
		final int colourRes = DependencyContainer.of(context).getThemeManager().getCurrent().card_colour;
		return ContextCompat.getColor(context, colourRes);
	}
}

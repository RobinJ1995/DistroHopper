package be.robinj.distrohopper.icons;

import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.AdaptiveIconDrawable;

/**
 * Builds the clip {@link Path} for an {@link IconShape} at a target pixel size.
 * The path is a closed silhouette in the square {@code [0,sizePx] x [0,sizePx]}
 * coordinate space, ready to be filled as an alpha mask.
 */
public final class IconMask {
	// Corner radii as a fraction of the icon's side. //
	private static final float ROUNDED_SQUARE_RADIUS_FRACTION = 0.18F;
	private static final float SQUIRCLE_RADIUS_FRACTION = 0.40F; // pragmatic superellipse stand-in //

	private IconMask() {
	}

	public static Path pathFor(final IconShape shape, final int sizePx) {
		final float size = sizePx;

		switch (shape) {
			case CIRCLE:
				return circle(size);
			case SQUIRCLE:
				return roundRect(size, SQUIRCLE_RADIUS_FRACTION * size);
			case ROUNDED_SQUARE:
				return roundRect(size, ROUNDED_SQUARE_RADIUS_FRACTION * size);
			case SQUARE:
				return roundRect(size, 0F);
			case SYSTEM:
			default:
				return systemMask(size);
		}
	}

	private static Path circle(final float size) {
		final Path path = new Path();
		path.addCircle(size / 2F, size / 2F, size / 2F, Path.Direction.CW);

		return path;
	}

	private static Path roundRect(final float size, final float radius) {
		final Path path = new Path();
		path.addRoundRect(new RectF(0F, 0F, size, size), radius, radius, Path.Direction.CW);

		return path;
	}

	/**
	 * The device-configured adaptive-icon mask, scaled onto {@code size}. The
	 * platform defines this mask on a 100x100 viewport via a maskless
	 * {@link AdaptiveIconDrawable}; if it is somehow empty we fall back to a
	 * circle so an icon is never left unmasked.
	 */
	private static Path systemMask(final float size) {
		final Path mask = new Path(new AdaptiveIconDrawable(null, null).getIconMask());

		if (mask.isEmpty()) {
			return circle(size);
		}

		final Matrix matrix = new Matrix();
		matrix.setScale(size / 100F, size / 100F);
		mask.transform(matrix);

		return mask;
	}
}

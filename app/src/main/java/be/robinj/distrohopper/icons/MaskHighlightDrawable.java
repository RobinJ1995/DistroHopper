package be.robinj.distrohopper.icons;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

/**
 * A selection highlight that traces an {@link IconShape} — a translucent fill
 * with a solid outline — so the indicator around a selected option matches the
 * shape of the option itself (a circle around a circular swatch, a squircle
 * around a squircle preview, and so on).
 */
public final class MaskHighlightDrawable extends Drawable {
	private final IconShape shape;
	private final float strokeWidth;
	private final Paint fill;
	private final Paint stroke;

	public MaskHighlightDrawable(final IconShape shape, final int accent, final float strokeWidth) {
		this.shape = shape;
		this.strokeWidth = strokeWidth;

		this.fill = new Paint(Paint.ANTI_ALIAS_FLAG);
		this.fill.setStyle(Paint.Style.FILL);
		this.fill.setColor(Color.argb(48, Color.red(accent), Color.green(accent), Color.blue(accent)));

		this.stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
		this.stroke.setStyle(Paint.Style.STROKE);
		this.stroke.setStrokeWidth(strokeWidth);
		this.stroke.setColor(accent);
	}

	@Override
	public void draw(@NonNull final Canvas canvas) {
		final Rect bounds = this.getBounds();
		final int size = Math.min(bounds.width(), bounds.height());
		if (size <= 0) {
			return;
		}

		// Inset by the stroke so the outline sits fully inside the bounds. //
		final int pathSize = Math.round(size - 2 * this.strokeWidth);
		final Path path = IconMask.pathFor(this.shape, pathSize);

		canvas.save();
		canvas.translate(
			bounds.left + (bounds.width() - size) / 2f + this.strokeWidth,
			bounds.top + (bounds.height() - size) / 2f + this.strokeWidth);
		canvas.drawPath(path, this.fill);
		canvas.drawPath(path, this.stroke);
		canvas.restore();
	}

	@Override
	public void setAlpha(final int alpha) {
		this.fill.setAlpha(alpha);
		this.stroke.setAlpha(alpha);
	}

	@Override
	public void setColorFilter(final ColorFilter colorFilter) {
		this.fill.setColorFilter(colorFilter);
		this.stroke.setColorFilter(colorFilter);
	}

	// getOpacity() is deprecated on Drawable but must still be overridden to
	// declare this drawable's opacity; suppress the inherited-deprecation warning.
	@SuppressWarnings("deprecation")
	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}
}

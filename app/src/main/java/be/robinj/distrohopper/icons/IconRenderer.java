package be.robinj.distrohopper.icons;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.NonNull;

/**
 * Renders an {@link AdaptiveIconDrawable} the way a launcher should: composites
 * the background and foreground layers with the correct safe-zone scaling and
 * clips them to a user-chosen mask {@link IconShape} with anti-aliased edges.
 * Optionally renders Material You themed (monochrome) icons.
 *
 * <p>Anything that is not an adaptive icon — legacy icons and icon-pack art — is
 * returned untouched, so those keep their original appearance.
 */
public final class IconRenderer {
	private final Context context;
	private final IconConfig config;

	public IconRenderer(final Context context, final IconConfig config) {
		this.context = context.getApplicationContext();
		this.config = config;
	}

	public IconConfig getConfig() {
		return this.config;
	}

	/**
	 * Returns a masked, consistently-sized rendering of {@code source} when it is
	 * an adaptive icon; otherwise returns {@code source} unchanged.
	 */
	public Drawable render(final Drawable source) {
		if (!(source instanceof AdaptiveIconDrawable)) {
			// Legacy and icon-pack drawables are left exactly as they are. //
			return source;
		}

		return new BitmapDrawable(this.context.getResources(),
			this.renderAdaptive((AdaptiveIconDrawable) source));
	}

	private Bitmap renderAdaptive(final AdaptiveIconDrawable adaptive) {
		final int size = this.config.getSizePx();
		final Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
		final Canvas canvas = new Canvas(bitmap);

		if (this.config.tintedRenderingSupported() && this.drawTinted(canvas, adaptive, size)) {
			this.applyMask(canvas, adaptive, size);
			return bitmap;
		}

		if (this.config.getShape() == IconShape.SYSTEM) {
			// Let the platform mask it exactly like the OS does, so "System default" //
			// always matches the device's real adaptive-icon shape. //
			adaptive.setBounds(0, 0, size, size);
			adaptive.draw(canvas);
			return bitmap;
		}

		// Standard adaptive compositing: background then foreground, each drawn //
		// oversized so the spec's 18/108 per-edge bleed falls under the mask. //
		this.drawLayer(canvas, adaptive.getBackground(), size);
		this.drawLayer(canvas, adaptive.getForeground(), size);
		this.applyMask(canvas, adaptive, size);

		return bitmap;
	}

	/**
	 * Draws the monochrome layer recoloured with the tint foreground over a tonal
	 * tint background. Returns {@code false} (so the caller falls back to standard
	 * compositing) when the icon carries no monochrome layer.
	 */
	private boolean drawTinted(final Canvas canvas, final AdaptiveIconDrawable adaptive, final int size) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
			return false;
		}

		final Drawable monochrome = adaptive.getMonochrome();
		if (monochrome == null) {
			return false;
		}

		canvas.drawColor(this.config.getTintBackground());

		final Drawable tinted = monochrome.mutate();
		tinted.setTint(this.config.getTintForeground());
		this.drawLayer(canvas, tinted, size);

		return true;
	}

	/** Draws a single adaptive layer at the inset-expanded bounds (no-op if null). */
	private void drawLayer(final Canvas canvas, final Drawable layer, final int size) {
		if (layer == null) {
			return;
		}

		final int inset = Math.round(size * AdaptiveIconDrawable.getExtraInsetFraction());
		final Rect bounds = layer.copyBounds();
		layer.setBounds(-inset, -inset, size + inset, size + inset);
		layer.draw(canvas);
		layer.setBounds(bounds); // restore, since layer drawables can be shared //
	}

	/** Clips whatever is already on {@code canvas} to the mask shape via anti-aliased DST_IN. */
	private void applyMask(final Canvas canvas, final AdaptiveIconDrawable adaptive, final int size) {
		final Bitmap mask = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
		final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
		fill.setColor(Color.WHITE);
		new Canvas(mask).drawPath(this.maskPath(adaptive, size), fill);

		final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
		canvas.drawBitmap(mask, 0F, 0F, maskPaint);
		mask.recycle();
	}

	/**
	 * The clip path for the configured shape. For {@link IconShape#SYSTEM} this is
	 * the real adaptive drawable's own device mask (more reliable than synthesising
	 * a maskless one), so tinted system icons match the OS silhouette exactly.
	 */
	private Path maskPath(final AdaptiveIconDrawable adaptive, final int size) {
		if (this.config.getShape() == IconShape.SYSTEM) {
			adaptive.setBounds(0, 0, size, size);
			final Path mask = new Path(adaptive.getIconMask());
			if (!mask.isEmpty()) {
				return mask;
			}
		}

		return IconMask.pathFor(this.config.getShape(), size);
	}

	@NonNull
	@Override
	public String toString() {
		return "IconRenderer{" + this.config.signature() + "}";
	}
}

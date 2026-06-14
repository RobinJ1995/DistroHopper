package be.robinj.distrohopper.icons;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.content.res.AppCompatResources;

import be.robinj.distrohopper.R;

/**
 * Lets the user pick the icon mask {@link IconShape} from a strip of live preview
 * thumbnails, each a sample adaptive icon rendered through {@link IconRenderer} in
 * that shape so it matches exactly what the launcher will draw.
 */
public class IconShapePreference extends IconStripPreference {
	public IconShapePreference(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	private static final int PREVIEW_SIZE_DP = 56;

	@Override
	protected void populate(final LinearLayout strip) {
		final Context context = this.getContext();
		final String current = this.getPersistedString(IconShape.SYSTEM.getPreferenceValue());
		final AdaptiveIconDrawable sample = this.sampleIcon(context);
		final int previewSize = this.dp(PREVIEW_SIZE_DP);

		for (final IconShape shape : IconShape.values()) {
			final ImageView preview = new ImageView(context);
			preview.setImageDrawable(this.renderPreview(context, sample, shape, previewSize));

			this.addOption(strip, preview, PREVIEW_SIZE_DP, context.getString(this.labelRes(shape)),
				shape, shape.getPreferenceValue().equals(current),
				() -> this.commit(shape.getPreferenceValue()));
		}
	}

	/**
	 * A penguin glyph on the active distro theme's brand colour — an on-brand
	 * sample whose masking into each shape is what the preview is showing off.
	 */
	private AdaptiveIconDrawable sampleIcon(final Context context) {
		final Drawable foreground = AppCompatResources.getDrawable(context, R.drawable.ic_icon_sample_foreground);
		return new AdaptiveIconDrawable(new ColorDrawable(IconTint.theme(context)), foreground);
	}

	private Drawable renderPreview(final Context context, final AdaptiveIconDrawable sample,
								   final IconShape shape, final int size) {
		// Shape previews are never tinted: they show the silhouette, not the colour. //
		final IconConfig config = new IconConfig(shape, false, size, Color.WHITE, Color.WHITE, Color.WHITE);
		return new IconRenderer(context, config).render(sample);
	}

	private int labelRes(final IconShape shape) {
		switch (shape) {
			case CIRCLE:
				return R.string.icon_shape_circle;
			case SQUIRCLE:
				return R.string.icon_shape_squircle;
			case ROUNDED_SQUARE:
				return R.string.icon_shape_rounded_square;
			case SQUARE:
				return R.string.icon_shape_square;
			case SYSTEM:
			default:
				return R.string.icon_shape_system;
		}
	}
}

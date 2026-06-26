package be.robinj.distrohopper.icons;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.content.res.AppCompatResources;

import java.util.ArrayList;
import java.util.List;

import be.robinj.distrohopper.R;

/**
 * Lets the user choose the colour used for tinted icons from a strip of swatches.
 * Each swatch is the same penguin sample as the shape preview, rendered tinted in
 * that swatch's colour, so the user previews the actual tinted look. The sources
 * are the wallpaper's primary colour (default), the system accent, the active
 * distro theme's brand colour, and a few fixed presets.
 */
public class IconTintPreference extends IconStripPreference {
	/** A handful of bright presets to complement the three dynamic sources. */
	private static final String[] PRESETS = {
		"#4285F4", // blue //
		"#34A853", // green //
		"#9334E6", // purple //
		"#FF7043", // orange //
		"#EC407A", // pink //
	};

	/** Same size as the shape preview thumbnails, so the two strips match. */
	private static final int SWATCH_SIZE_DP = 56;

	public IconTintPreference(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		// A compact, indented layout so the swatches read as part of the Tinted icons setting. //
		this.setLayoutResource(R.layout.pref_icon_swatches);
	}

	private static final class Swatch {
		final String token;
		final int colour;
		final CharSequence label;

		Swatch(final String token, final int colour, final CharSequence label) {
			this.token = token;
			this.colour = colour;
			this.label = label;
		}
	}

	@Override
	protected void populate(final LinearLayout strip) {
		final Context context = this.getContext();
		final String current = this.getPersistedString(IconTint.WALLPAPER);
		final int size = this.dp(SWATCH_SIZE_DP);
		final boolean night = IconConfig.isNightMode(context);
		final AdaptiveIconDrawable sample = this.sampleIcon(context);

		for (final Swatch swatch : this.swatches(context)) {
			final ImageView preview = new ImageView(context);
			preview.setImageDrawable(this.renderTinted(context, sample, swatch.colour, night, size));

			this.addOption(strip, preview, SWATCH_SIZE_DP, swatch.label, IconShape.CIRCLE,
				swatch.token.equalsIgnoreCase(current), () -> this.commit(swatch.token));
		}
	}

	/** The penguin sample with a monochrome layer so it can be recoloured when tinted. */
	private AdaptiveIconDrawable sampleIcon(final Context context) {
		final Drawable foreground = AppCompatResources.getDrawable(context, R.drawable.ic_icon_sample_foreground);
		final Drawable monochrome = AppCompatResources.getDrawable(context, R.drawable.ic_icon_sample_foreground);
		return new AdaptiveIconDrawable(new ColorDrawable(IconTint.theme(context)), foreground, monochrome);
	}

	private Drawable renderTinted(final Context context, final AdaptiveIconDrawable sample,
								  final int colour, final boolean night, final int size) {
		final IconConfig config = new IconConfig(IconShape.CIRCLE, true, size, colour,
			IconConfig.tintBackground(colour, night), IconConfig.tintForeground(colour, night));
		return new IconRenderer(context, config).render(sample);
	}

	private List<Swatch> swatches(final Context context) {
		final List<Swatch> swatches = new ArrayList<>();

		final Integer wallpaper = IconTint.wallpaper(context);
		swatches.add(new Swatch(IconTint.WALLPAPER,
			wallpaper != null ? wallpaper : IconTint.accent(context),
			context.getString(R.string.tint_wallpaper)));
		swatches.add(new Swatch(IconTint.ACCENT, IconTint.accent(context),
			context.getString(R.string.tint_accent)));
		swatches.add(new Swatch(IconTint.THEME, IconTint.theme(context),
			context.getString(R.string.tint_theme)));

		for (final String preset : PRESETS) {
			swatches.add(new Swatch(preset, android.graphics.Color.parseColor(preset), ""));
		}

		return swatches;
	}
}

package be.robinj.distrohopper.icons;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import be.robinj.distrohopper.R;
import be.robinj.distrohopper.cache.AppIconCache;

/**
 * Base for the custom icon preferences whose row is a title/summary header above
 * a horizontally scrollable strip of selectable options (shape thumbnails or
 * colour swatches). Subclasses just fill the strip; this class handles the row
 * layout, the selection ring, and persisting a chosen value (which also clears
 * the rendered-icon cache so the change takes effect on the next load).
 */
public abstract class IconStripPreference extends Preference {
	public IconStripPreference(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		this.setLayoutResource(R.layout.pref_icon_strip);
	}

	@Override
	public void onBindViewHolder(final PreferenceViewHolder holder) {
		super.onBindViewHolder(holder);

		final LinearLayout strip = (LinearLayout) holder.findViewById(R.id.icon_strip_options);
		strip.removeAllViews();
		this.populate(strip);
	}

	/** Add each selectable option to [strip] via {@link #addOption}. */
	protected abstract void populate(LinearLayout strip);

	/**
	 * Adds one option: [content] (sized [contentSizeDp] square) above an optional
	 * [label], framed by a selection highlight shaped like [highlightShape] when
	 * [selected], invoking [onClick] when tapped.
	 */
	protected void addOption(final LinearLayout strip, final View content, final int contentSizeDp,
							 final CharSequence label, final IconShape highlightShape,
							 final boolean selected, final Runnable onClick) {
		final Context context = this.getContext();
		final int contentPx = this.dp(contentSizeDp);
		final int framePadding = this.dp(8);
		final int frameSize = contentPx + 2 * framePadding;

		final LinearLayout option = new LinearLayout(context);
		option.setOrientation(LinearLayout.VERTICAL);
		option.setGravity(Gravity.CENTER_HORIZONTAL);
		final LinearLayout.LayoutParams optionParams = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		optionParams.setMarginEnd(this.dp(4));
		option.setLayoutParams(optionParams);
		option.setClickable(true);
		option.setFocusable(true);
		option.setOnClickListener(v -> onClick.run());

		// A fixed-size frame keeps the content centred under a same-shaped highlight. //
		final FrameLayout frame = new FrameLayout(context);
		frame.setLayoutParams(new LinearLayout.LayoutParams(frameSize, frameSize));
		if (selected) {
			frame.setBackground(new MaskHighlightDrawable(
				highlightShape, IconTint.accent(context), this.dp(2)));
		}
		content.setLayoutParams(new FrameLayout.LayoutParams(contentPx, contentPx, Gravity.CENTER));
		frame.addView(content);
		option.addView(frame);

		if (label != null && label.length() > 0) {
			final TextView text = new TextView(context);
			text.setText(label);
			text.setGravity(Gravity.CENTER_HORIZONTAL);
			text.setMaxLines(2);
			text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
			text.setTextColor(selected ? IconTint.accent(context) : this.secondaryTextColor());
			final LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
				frameSize, ViewGroup.LayoutParams.WRAP_CONTENT);
			textParams.topMargin = this.dp(6);
			text.setLayoutParams(textParams);
			option.addView(text);
		}

		strip.addView(option);
	}

	/** Persist [value], clear the rendered-icon cache, and refresh the strip. */
	protected void commit(final String value) {
		if (this.callChangeListener(value)) {
			this.persistString(value);
			try {
				AppIconCache.clearAll(this.getContext().getApplicationContext());
			} catch (final Exception ignored) {
				// A failed cache clear only means the change shows up a little later. //
			}
			this.notifyChanged();
		}
	}

	protected int dp(final int value) {
		return Math.round(value * this.getContext().getResources().getDisplayMetrics().density);
	}

	private int secondaryTextColor() {
		final TypedValue value = new TypedValue();
		this.getContext().getTheme().resolveAttribute(android.R.attr.textColorSecondary, value, true);
		return this.getContext().getColor(value.resourceId != 0 ? value.resourceId : android.R.color.darker_gray);
	}
}

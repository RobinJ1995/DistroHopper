/*
 * Copyright (c) 2001 - 2012 Sileria, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package be.robinj.ubuntu.thirdparty;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.*;
import android.widget.*;

/**
 * SeekBarPreference class lets you put a {@linkplain android.widget.SeekBar} into the Preferences or
 * the PreferencesActivity. The persisted value is an {@code int}.
 * <p/>
 * Example: <br>
 * <blockqoute><pre>
 * SeekBarPreference gameVol = new SeekBarPreference( this );
 * gameVol.setTitle( R.string.game_volume );
 * gameVol.setKey( OPT_GAME_VOLUME );
 * gameVol.setDefaultValue( DEF_GAME_VOLUME ) );
 *
 * main.addPreference( gameVol );
 * </pre></blockqoute>
 * </p>
 * 
 * @author Hassan Jawed
 * @author Ahmed Shakil
 * @date November 13th, 2011.
 */
public class SeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener {

	private SeekBar seekbar;
	private int progress;
	private int max = 100;

	private TextView summary;

	private boolean discard;

	/**
	 * Perform inflation from XML and apply a class-specific base style. 
	 *
	 * @param context The Context this is associated with, through which it can
	 *            access the current theme, resources, {@link android.content.SharedPreferences},
	 *            etc.
	 * @param attrs The attributes of the XML tag that is inflating the preference.
	 * @param defStyle The default style to apply to this preference. If 0, no style
	 *            will be applied (beyond what is included in the theme). This
	 *            may either be an attribute resource, whose value will be
	 *            retrieved from the current theme, or an explicit style
	 *            resource.
	 * @see #SeekBarPreference(android.content.Context, android.util.AttributeSet)
	 */
	public SeekBarPreference (Context context, AttributeSet attrs, int defStyle) {
		super( context, attrs, defStyle );
	}

	/**
	 * Constructor that is called when inflating a Preference from XML.
	 *
	 * @param context The Context this is associated with, through which it can
	 *            access the current theme, resources, {@link android.content.SharedPreferences},
	 *            etc.
	 * @param attrs The attributes of the XML tag that is inflating the
	 *            preference.
	 * @see #SeekBarPreference(android.content.Context, android.util.AttributeSet, int)
	 */
	public SeekBarPreference (Context context, AttributeSet attrs) {
		super( context, attrs );
	}

	/**
	 * Constructor to create a Preference.
	 *
	 * @param context The Context in which to store Preference values.
	 */
	public SeekBarPreference (Context context) {
		super( context );
	}

	/**
	 * Create progress bar and other view contents.
	 */
	protected View onCreateView (ViewGroup p) {

		final Context ctx = getContext();

		LinearLayout layout = new LinearLayout( ctx );
		layout.setId( android.R.id.widget_frame );
		layout.setOrientation (LinearLayout.VERTICAL);
		layout.setPadding (15, 15, 15, 15);

		TextView title = new TextView( ctx );
		title.setId (android.R.id.title);
		title.setSingleLine ();
		title.setTextAppearance (ctx, android.R.style.TextAppearance_Medium);
		layout.addView( title );

		seekbar = new SeekBar( ctx );
		seekbar.setId( android.R.id.progress );
		seekbar.setMax( max );
		seekbar.setOnSeekBarChangeListener( this );
		layout.addView( seekbar );

		summary = new TextView( ctx );
		summary.setId (android.R.id.summary);
		summary.setTextAppearance (ctx, android.R.style.TextAppearance_Small);
		layout.addView( summary );

		return layout;
	}

	/**
	 * Binds the created View to the data for this Preference.
	 */
	@Override
	protected void onBindView (View view) {
		super.onBindView( view );

		if (seekbar != null)
			seekbar.setProgress( progress );
	}

	/**
	 * <p>Set the current progress to the specified value. Does not do anything
	 * if the progress bar is in indeterminate mode.</p>
	 *
	 * @param pcnt the new progress, between 0 and {@link android.widget.SeekBar#getMax()}
	 */
	public void setProgress (int pcnt) {
		if (progress != pcnt) {
			persistInt( progress = pcnt );

			notifyDependencyChange( shouldDisableDependents() );
			notifyChanged();
		}
	}

	/**
	 * <p>Get the progress bar's current level of progress. Return 0 when the
	 * progress bar is in indeterminate mode.</p>
	 *
	 * @return the current progress, between 0 and {@link android.widget.SeekBar#getMax()}
	 */
	public int getProgress () {
		return progress;
	}

	/**
	 * Set the max value for the <code>SeekBar</code> object.
	 *
	 * @param max max value
	 */
	public void setMax (int max) {
		this.max = max;
		if (seekbar != null)
			seekbar.setMax( max );
	}

	/**
	 * Get the underlying <code>SeekBar</code> object.
	 *
	 * @return <code>SeekBar</code> object
	 */
	private SeekBar getSeekBar () {
		return seekbar;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object onGetDefaultValue (TypedArray a, int index) {
		return a.getInt( index, progress );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onSetInitialValue (boolean restoreValue, Object defaultValue) {
		setProgress( restoreValue ? getPersistedInt( progress ) : (Integer)defaultValue );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean shouldDisableDependents () {
		return progress == 0 || super.shouldDisableDependents();
	}

	/**
	 * Set the progress of the preference.
	 */
	public void onProgressChanged (SeekBar seekBar, int progress, boolean fromUser) {
		discard = !callChangeListener( progress );
	}

	/**
	 * {@inheritDoc}
	 */
	public void onStartTrackingTouch (SeekBar seekBar) {
		discard = false;
	}

	/**
	 * {@inheritDoc}
	 */
	public void onStopTrackingTouch (SeekBar seekBar) {
		if (discard)
			seekBar.setProgress( progress );
		else {
			setProgress( seekBar.getProgress() );

			OnPreferenceChangeListener listener = getOnPreferenceChangeListener();
			if (listener instanceof AbstractSeekBarListener)
				setSummary( ((AbstractSeekBarListener)listener).toSummary( seekBar.getProgress() ) );
		}
	}

	/**
	 * Abstract seek bar summary updater.
	 *
	 * @see #setSummary(String)
	 */
	public static abstract class AbstractSeekBarListener extends PrefsChangeListener<SeekBarPreference> {

		/**
		 * Construct a change lsitener for the specified widget.
		 */
		public AbstractSeekBarListener (SeekBarPreference pref) {
			super( pref );
		}

		/**
		 * Sets the summary string directly into the text view
		 * to avoid {@link be.robinj.ubuntu.thirdparty.SeekBarPreference#notifyChanged()} call
		 * which was interrupting in the seek bar's thumb movement.
		 */
		protected final void setSummary (String text) {
			if (pref.summary != null)
				pref.summary.setText( text );
		}

		/**
		 * Convert integer progress to summary string.
		 * @param newValue should be an Integer instance
		 */
		protected abstract String toSummary (Object newValue);

	}
}

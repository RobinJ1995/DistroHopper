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

import android.preference.Preference;

/**
 * Preference Change Listener handles upddating of the summary text for any preference item
 * in a {@code PreferenceActivity}.
 *
 * @author Ahmed Shakil
 * @date 1/23/11
 *
 * @see PrefsListListener
 * @see PrefsEditListener
 */
public class PrefsChangeListener<T extends Preference> implements Preference.OnPreferenceChangeListener {

	protected final T pref;

	/**
	 * Construct a change lsitener for the specified widget.
	 */
	public PrefsChangeListener (T pref) {
		this.pref = pref;
	}

	/**
	 * Preference change callback.
	 */
	public boolean onPreferenceChange (Preference preference, Object newValue) {
		updateSummary( newValue );

		return true;
	}

	/**
	 * Update the summary text.
	 */
	protected void updateSummary (Object newValue) {
		pref.setSummary( newValue == null ? "" : String.valueOf( newValue ) );
	}
}

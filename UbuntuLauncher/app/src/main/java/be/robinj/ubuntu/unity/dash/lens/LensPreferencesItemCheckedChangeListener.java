package be.robinj.ubuntu.unity.dash.lens;

import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

/**
 * Created by robin on 29/11/14.
 */
public class LensPreferencesItemCheckedChangeListener implements CheckBox.OnCheckedChangeListener
{
	private LensManager lensManager;

	public LensPreferencesItemCheckedChangeListener (LensManager lensManager)
	{
		super ();

		this.lensManager = lensManager;
	}

	@Override
	public void onCheckedChanged (CompoundButton view, boolean isChecked)
	{
		Lens lens = (Lens) view.getTag ();

		if (isChecked)
			this.lensManager.enableLens (lens);
		else
			this.lensManager.disableLens (lens);
	}
}

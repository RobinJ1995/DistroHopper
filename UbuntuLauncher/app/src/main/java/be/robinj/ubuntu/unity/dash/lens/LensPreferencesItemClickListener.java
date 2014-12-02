package be.robinj.ubuntu.unity.dash.lens;

import android.view.View;
import android.widget.CheckBox;

/**
 * Created by robin on 29/11/14.
 */
public class LensPreferencesItemClickListener implements View.OnClickListener // OnItemClickListener doesn't seem to work with a DragSortListView //
{
	private LensManager lensManager;
	private Lens lens;
	private CheckBox cbEnabled;

	public LensPreferencesItemClickListener (LensManager lensManager, Lens lens, CheckBox cbEnabled)
	{
		super ();

		this.lensManager = lensManager;
		this.lens = lens;
		this.cbEnabled = cbEnabled;
	}

	@Override
	public void onClick (View view)
	{
		boolean checked = this.cbEnabled.isChecked ();

		if (! (view instanceof CheckBox))
			this.cbEnabled.setChecked (! checked);

		if (this.cbEnabled.isChecked ())
			this.lensManager.enableLens (this.lens);
		else
			this.lensManager.disableLens (this.lens);
	}
}

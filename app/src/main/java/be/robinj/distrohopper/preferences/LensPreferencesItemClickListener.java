package be.robinj.distrohopper.preferences;

import android.app.Activity;
import android.view.View;
import android.widget.CompoundButton;

import be.robinj.distrohopper.ExceptionHandler;
import be.robinj.distrohopper.Permission;
import be.robinj.distrohopper.desktop.dash.lens.Lens;
import be.robinj.distrohopper.desktop.dash.lens.LensManager;

/**
 * Created by robin on 29/11/14.
 */
public class LensPreferencesItemClickListener implements View.OnClickListener // OnItemClickListener doesn't seem to work with a DragSortListView //
{
	private final Activity parent;
	private LensManager lensManager;
	private Lens lens;
	private CompoundButton cbEnabled;

	public LensPreferencesItemClickListener (final Activity parent, LensManager lensManager, Lens lens, CompoundButton cbEnabled)
	{
		super ();

		this.parent = parent;
		this.lensManager = lensManager;
		this.lens = lens;
		this.cbEnabled = cbEnabled;
	}

	@Override
	public void onClick (View view)
	{
		try
		{
			boolean checked = this.cbEnabled.isChecked ();

			if (!(view instanceof CompoundButton))
				this.cbEnabled.setChecked (!checked);

			if (this.cbEnabled.isChecked ())
			{
				this.lensManager.enableLens (this.lens);

				// The lens may have been disabled because its permissions were
				// never granted; enabling it is the moment to ask again //
				Permission.requestMultiple (this.parent, this.lens.requiredPermissions ());
			}
			else
			{
				this.lensManager.disableLens (this.lens);
			}
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (ex);
			exh.show (this.lensManager.getContext ());
		}
	}
}

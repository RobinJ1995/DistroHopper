package be.robinj.distrohopper.desktop.launcher;

import android.content.Context;
import android.util.AttributeSet;

import be.robinj.distrohopper.R;
import be.robinj.distrohopper.thirdparty.ProgressWheel;

/**
 * Created by robin on 8/21/14.
 */
public class SpinnerAppLauncher extends AppLauncher
{
	public SpinnerAppLauncher (Context context, AttributeSet attrs)
	{
		super (context, attrs, R.layout.widget_launcher_applauncher_spinner, R.layout.widget_launcher_applauncher_spinner);
	}

	public ProgressWheel getProgressWheel ()
	{
		return (ProgressWheel) this.findViewById (R.id.progressWheel);
	}

	@Override
	protected void iconChanged ()
	{
		// Do nothing //
	}
}

package be.robinj.ubuntu.unity.launcher.service;

import android.view.MotionEvent;
import android.view.View;

import be.robinj.ubuntu.R;

/**
 * Created by robin on 8/27/14.
 */
public class TouchListener implements View.OnTouchListener
{
	private LauncherService parent;
	private boolean right = false;

	private float llListener_x = -1;
	private float llLauncher_x = -1;

	public TouchListener (LauncherService parent, boolean right)
	{
		this.parent = parent;
		this.right = right;
	}

	@Override
	public boolean onTouch (View view, MotionEvent event)
	{
		int id = view.getId ();
		//String tag = (String) view.getTag ();

		if (id == R.id.llListener)
		{
			if (this.llListener_x == -1)
				this.llListener_x = (this.right ? -1 : 1) * event.getX (0);
			float x = (this.right ? -1 : 1) * event.getX (event.getPointerCount () - 1);

			if (x > this.llListener_x)
			{
				this.parent.swipeRight ();

				this.llListener_x = -1;
			}
			else if (x < this.llListener_x || event.getAction () == MotionEvent.ACTION_CANCEL || event.getAction () == MotionEvent.ACTION_UP)
			{
				this.llListener_x = -1;
			}

			return true;
		}
		// This doesn't work properly //
		/*else if ("partOfLauncher".equals (tag))
		{
			float x = event.getX ();

			switch (event.getAction ())
			{
				case MotionEvent.ACTION_DOWN:
				case MotionEvent.ACTION_MOVE:
				case MotionEvent.ACTION_HOVER_MOVE:
					if (this.llLauncher_x - x > 40)
					{
						this.parent.swipeLeft ();

						this.llLauncher_x = -1;
					}
					else
					{
						this.llLauncher_x = x;
					}
					break;
				case MotionEvent.ACTION_CANCEL:
				case MotionEvent.ACTION_UP:
					if (this.llLauncher_x - x > 40)
						this.parent.swipeLeft ();

					this.llLauncher_x = -1;

					return true;
			}
		}*/
		else if (id == R.id.llShadow)
		{
			this.parent.swipeLeft ();

			return true;
		}

		return false;
	}
}

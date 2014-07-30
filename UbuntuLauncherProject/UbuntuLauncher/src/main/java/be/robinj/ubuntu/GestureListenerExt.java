package be.robinj.ubuntu;

import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

public class GestureListenerExt extends SimpleOnGestureListener
{
	private MainActivity main;

	public GestureListenerExt (MainActivity main)
	{
		this.main = main;
	}

	@Override
	public boolean onFling (MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
	{
		float diffX = e1.getX () - e2.getX ();
		float diffY = Math.abs (e1.getY () - e2.getY ());

		if (diffY > 200)
			return false;

		if (diffX > 120)
			swipeLeft ();
		else if (-diffX > 120)
			swipeRight ();

		return false;
	}

	private void swipeLeft ()
	{
		this.main.runJs ("event_swipeLeft ();");
	}

	private void swipeRight ()
	{
		this.main.runJs ("event_swipeRight ();");
	}
}

package be.robinj.ubuntu;

import android.view.View;
import android.view.View.OnLongClickListener;

public class LongPressListenerExt implements OnLongClickListener
{
	private MainActivity main;

	public LongPressListenerExt (MainActivity main)
	{
		this.main = main;
	}

	@Override
	public boolean onLongClick (View v)
	{
		this.main.runJs ("event_longPress ();");

		return true;
	}

}
package be.robinj.ubuntu;

import android.webkit.JavascriptInterface;

import java.util.ArrayList;

/**
 * Created by Robin on 26/01/14.
 */
public class ArrayListExt<T> extends ArrayList<T>
{
	@Override
	@JavascriptInterface
	public int size ()
	{
		return super.size ();
	}

	@Override
	@JavascriptInterface
	public T get (int index)
	{
		return super.get (index);
	}

	@JavascriptInterface
	public String getString (int index)
	{
		return this.get (index).toString ();
	}
}

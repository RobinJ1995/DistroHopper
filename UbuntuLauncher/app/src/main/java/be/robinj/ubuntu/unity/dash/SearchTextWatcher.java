package be.robinj.ubuntu.unity.dash;

import android.text.Editable;
import android.text.TextWatcher;

import be.robinj.ubuntu.AppManager;
import be.robinj.ubuntu.ExceptionHandler;

/**
 * Created by robin on 8/21/14.
 */
public class SearchTextWatcher implements TextWatcher
{
	private AppManager apps;

	public SearchTextWatcher (AppManager apps)
	{
		this.apps = apps;
	}

	@Override
	public void beforeTextChanged (CharSequence s, int start, int count, int after)
	{
	}

	@Override
	public void onTextChanged (CharSequence s, int start, int before, int count)
	{
		try
		{
			apps.search (s.toString (), true);
		}
		catch (Exception ex)
		{
			ExceptionHandler exh = new ExceptionHandler (this.apps.getContext (), ex);
			exh.show ();
		}
	}

	@Override
	public void afterTextChanged (Editable s)
	{
	}
}

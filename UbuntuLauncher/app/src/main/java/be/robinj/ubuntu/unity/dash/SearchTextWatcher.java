package be.robinj.ubuntu.unity.dash;

import android.text.Editable;
import android.text.TextWatcher;

import be.robinj.ubuntu.AppManager;
import be.robinj.ubuntu.ExceptionHandler;
import be.robinj.ubuntu.unity.dash.lens.LensManager;

/**
 * Created by robin on 8/21/14.
 */
public class SearchTextWatcher implements TextWatcher
{
	private AppManager apps;
	private LensManager lenses;

	public SearchTextWatcher (AppManager apps, LensManager lenses)
	{
		this.apps = apps;
		this.lenses = lenses;
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
			this.apps.search (s.toString (), true);
			this.lenses.search (s.toString (), true);
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

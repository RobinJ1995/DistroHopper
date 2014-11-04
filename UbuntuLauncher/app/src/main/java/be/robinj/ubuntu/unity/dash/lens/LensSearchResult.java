package be.robinj.ubuntu.unity.dash.lens;

import android.graphics.drawable.Drawable;

/**
 * Created by robin on 4/11/14.
 */
public class LensSearchResult
{
	private String name;
	private String url;
	private Drawable icon;

	public LensSearchResult (String name, String url, Drawable icon)
	{
		this.name = name;
		this.url = url;
		this.icon = icon;
	}

	public String getName ()
	{
		return this.name;
	}

	public void setName (String name)
	{
		this.name = name;
	}

	public String getUrl ()
	{
		return this.url;
	}

	public void setUrl (String url)
	{
		this.url = url;
	}

	public Drawable getIcon ()
	{
		return this.icon;
	}

	public void setIcon (Drawable icon)
	{
		this.icon = icon;
	}
}

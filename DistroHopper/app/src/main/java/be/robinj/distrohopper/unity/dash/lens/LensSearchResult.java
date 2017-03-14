package be.robinj.distrohopper.unity.dash.lens;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * Created by robin on 4/11/14.
 */
public class LensSearchResult extends LinearLayout
{
	private Context context;
	private String name;
	private String url;
	private Drawable icon;
	private Object obj;

	public LensSearchResult (Context context, String name, String url, Drawable icon)
	{
		super (context);

		this.context = context;
		this.name = name;
		this.url = url;
		this.icon = icon;
	}

	public LensSearchResult (Context context, String name, String url, Drawable icon, Object obj)
	{
		this (context, name, url, icon);

		this.obj = obj;
	}

	public LensSearchResult (Context context)
	{
		super (context);
	}

	public LensSearchResult (Context context, AttributeSet attrs)
	{
		super (context, attrs);
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

	public Object getObj ()
	{
		return this.obj;
	}

	public void setObj (Object obj)
	{
		this.obj = obj;
	}
}

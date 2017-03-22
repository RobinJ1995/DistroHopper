package be.robinj.distrohopper.desktop;

import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import be.robinj.distrohopper.App;
import be.robinj.distrohopper.HomeActivity;
import be.robinj.distrohopper.R;

/**
 * Created by robin on 8/20/14.
 */
public class AppLauncher extends LinearLayout
{
	private String label;
	private String description;
	private boolean special;
	private AppIcon icon;

	private Context context;

	private App app;

	protected AppLauncher (Context context, App app, int layout, int layoutSpecial)
	{
		this (context, (AttributeSet) null, layout, layoutSpecial);

		this.app = app;

		this.label = app.getLabel ();
		this.description = app.getDescription ();
		this.icon = app.getIcon ();
		this.setTag (app);

		this.onFinishInflate ();
	}

	protected AppLauncher (Context context, AttributeSet attrs, int layout, int layoutSpecial) // This constructor shouldn't be called directly //
	{
		super (context, attrs);

		this.context = context;

		TypedArray styleAttrs = context.getTheme ().obtainStyledAttributes (attrs, R.styleable.AppLauncher, 0, 0);
		if (styleAttrs != null)
		{
			this.label = styleAttrs.getString (R.styleable.AppLauncher_applauncher_label);
			this.description = styleAttrs.getString (R.styleable.AppLauncher_applauncher_description);
			this.special = styleAttrs.getBoolean (R.styleable.AppLauncher_applauncher_special, false);
			Drawable icon = styleAttrs.getDrawable (R.styleable.AppLauncher_applauncher_icon);

			if (icon != null)
				this.icon = new AppIcon (icon);
		}

		LayoutInflater inflater = (LayoutInflater) context.getSystemService (Service.LAYOUT_INFLATER_SERVICE);
		inflater.inflate (this.special ? layoutSpecial : layout, this, true);

		if (styleAttrs != null)
			styleAttrs.recycle ();

		if (! this.isInEditMode ()) // Don't run init method when rendering preview in IDE //
			this.init ();
	}

	@Override
	public void onFinishInflate ()
	{
		super.onFinishInflate ();

		this.labelChanged ();
		this.descriptionChanged ();
		this.specialChanged ();
		this.iconChanged ();
	}

	protected void init ()
	{
	}

	public String getLabel ()
	{
		return label;
	}

	public void setLabel (String label)
	{
		this.label = label;

		this.labelChanged ();
	}

	private void labelChanged ()
	{
		if (this.label != null)
		{
			TextView tvLabel = (TextView) this.findViewById (R.id.tvLabel);

			if (tvLabel != null)
				tvLabel.setText (this.label);
		}
	}

	public String getDescription ()
	{
		return description;
	}

	public void setDescription (String description)
	{
		this.description = description;

		this.descriptionChanged ();
	}

	private void descriptionChanged ()
	{
	}

	protected boolean isSpecial ()
	{
		return special;
	}

	public void setSpecial (boolean special)
	{
		this.special = special;

		this.specialChanged ();
	}

	private void specialChanged ()
	{
		if (! this.special)
			this.iconChanged ();
	}

	public AppIcon getIcon ()
	{
		return icon;
	}

	public void setIcon (AppIcon icon)
	{
		this.icon = icon;

		this.iconChanged ();
	}

	public void setIcon (Drawable drawable)
	{
		this.icon = new AppIcon (drawable);

		this.iconChanged ();
	}

	protected void iconChanged ()
	{
		if (! this.isInEditMode () && (this.icon != null))
		{
			ImageView imgIcon = (ImageView) this.findViewById (R.id.imgIcon);
			imgIcon.setImageDrawable (this.icon.getDrawable ());

			LinearLayout llBackground = (LinearLayout) this.findViewById (R.id.llBackground);
			if (llBackground != null && (! this.special) && this.getResources ().getBoolean (HomeActivity.theme.launcher_applauncher_backgroundcolour_dynamic))
			{
				SharedPreferences prefs = this.context.getSharedPreferences ("prefs", Context.MODE_PRIVATE);

				int avgColour = this.icon.getAverageColour
				(
					prefs.getBoolean ("colourcalc_advanced", true),
					prefs.getBoolean ("colourcalc_hsv", true),
					prefs.getInt ("launchericon_opacity", 204)
				);

				GradientDrawable gd = (GradientDrawable) llBackground.getBackground ();
				gd.setColor (avgColour);
			}
		}
	}

	public App getApp ()
	{
		return this.app;
	}
}
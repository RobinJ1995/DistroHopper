package be.robinj.distrohopper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import be.robinj.distrohopper.unity.AppIcon;

/**
 * Created by robin on 06/09/14.
 */
public class IconPackHelper
{
	private Context context;

	private final String[] actions = new String[]
	{
		"org.adw.launcher.THEMES",
		"com.gau.go.launcherex.theme"
	};
	private final String[] categories = new String[]
	{
		"com.fede.launcher.THEME_ICONPACK",
		"com.anddoes.launcher.THEME",
		"com.teslacoilsw.launcher.THEME"
	};

	private String name;
	private boolean iconPackLoaded = false;
	private Map<String, String> resourceMap;
	private Resources resources;
	private float fallbackScale;
	private List<String> fallback;

	public IconPackHelper (Context context)
	{
		this.context = context;
	}

	public Map<String, ResolveInfo> getIconPacks ()
	{
		PackageManager pacMan = this.context.getPackageManager ();
		Intent intent = new Intent ();

		Map<String, ResolveInfo> iconPacks = new HashMap<String, ResolveInfo> ();

		for (String action : this.actions)
		{
			intent.setAction (action);

			for (ResolveInfo resInf : pacMan.queryIntentActivities (intent, 0))
				iconPacks.put (resInf.activityInfo.packageName, resInf);
		}

		intent.setAction (Intent.ACTION_MAIN);
		for (String category : this.categories)
		{
			intent.addCategory (category);

			for (ResolveInfo resInf : pacMan.queryIntentActivities (intent, 0))
				iconPacks.put (resInf.activityInfo.packageName, resInf);

			intent.removeCategory (category);
		}

		return iconPacks;
	}

	public void loadIconPack (String packageName) throws PackageManager.NameNotFoundException, IOException, XmlPullParserException
	{
		PackageManager pacMan = this.context.getPackageManager ();
		Resources res = pacMan.getResourcesForApplication (packageName);

		/*InputStream stream = res.getAssets ().open ("appfilter.xml");
		XmlPullParser parser = XmlPullParserFactory.newInstance ().newPullParser ();
		parser.setInput (stream, "UTF-8");*/

		int resId = res.getIdentifier ("appfilter", "xml", packageName);
		XmlPullParser parser = res.getXml (resId);

		this.name = packageName;
		this.iconPackLoaded = true;
		this.resources = res;

		this.parseXml (parser);
	}

	public AppIcon getIconForApp (App app)
	{
		String iconName = this.resourceMap.get (app.getActivityName ());
		if (iconName == null)
			iconName = this.resourceMap.get (app.getPackageName ());

		if (iconName == null)
			return null;

		Drawable icon = this.getIcon (iconName);

		if (icon == null)
			return null;

		return new AppIcon (icon);
	}

	public AppIcon getFallbackIcon (Drawable appIcon)
	{
		Drawable icon = appIcon;

		if (this.fallback != null && (! this.fallback.isEmpty ()))
		{
			Random random = new Random ();
			int n = random.nextInt (this.fallback.size ());

			String fallbackName = this.fallback.get (n);
			Drawable fallback = this.getIcon (fallbackName);

			if (fallback == null)
				return null;

			Bitmap bmIcon = ((BitmapDrawable) icon).getBitmap ();
			Bitmap bmBackground = ((BitmapDrawable) fallback).getBitmap ();

			Bitmap result = Bitmap.createBitmap (bmIcon.getWidth (), bmIcon.getHeight (), Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas (result);

			Paint paint = new Paint ();
			paint.setAntiAlias (true);
			paint.setFilterBitmap (true);

			float margin = (1.0F - this.fallbackScale) / 2.0F;

			canvas.drawBitmap
			(
				bmBackground,
				new Rect
				(
					0,
					0,
					bmBackground.getWidth (),
					bmBackground.getHeight ()
				),
				new Rect
				(
					0,
					0,
					canvas.getWidth (),
					canvas.getHeight ()
				),
				paint
			);
			canvas.drawBitmap
			(
				bmIcon,
				new Rect
				(
					0,
					0,
					bmIcon.getWidth (),
					bmIcon.getHeight ()
				),
				new Rect
				(
					Math.round ((float) canvas.getWidth () * margin),
					Math.round ((float) canvas.getHeight () * margin),
					Math.round ((float) canvas.getWidth () * (1.0F - margin)),
					Math.round ((float) canvas.getHeight () * (1.0F - margin))
				),
				paint
			);

			icon = new BitmapDrawable (result);
		}

		return new AppIcon (icon);
	}

	public Drawable getIcon (String iconName)
	{
		int resId = this.resources.getIdentifier (iconName, "drawable", this.name);
		if (resId != 0)
		{
			Drawable icon = this.resources.getDrawable (resId);

			return icon;
		}

		return null;
	}

	private void parseXml (XmlPullParser parser) throws XmlPullParserException, IOException
	{
		Map<String, String> resourceMap = new HashMap<String, String> ();
		List<String> fallback = new ArrayList<String> ();
		float fallbackScale = 0.75F;

		int event;

		while ((event = parser.next ()) != XmlPullParser.END_DOCUMENT)
		{
			if (event == XmlPullParser.START_TAG)
			{
				String tag = parser.getName ();

				if (tag.equalsIgnoreCase ("item"))
				{
					String component = parser.getAttributeValue (null, "component");
					String drawable = parser.getAttributeValue (null, "drawable");

					if
						(
						!(TextUtils.isEmpty (component) || TextUtils.isEmpty (drawable))
							&& component.startsWith ("ComponentInfo{") && component.endsWith ("}") && component.length () >= 16
						)
					{
						component = component.substring (14, component.length () - 1).toLowerCase ();

						if (component.contains ("/"))
						{
							ComponentName compName = ComponentName.unflattenFromString (component);

							if (compName != null)
							{
								resourceMap.put (compName.getPackageName (), drawable);
								resourceMap.put (compName.getClassName (), drawable);
							}
						}
						else
						{
							resourceMap.put (component, drawable);
						}
					}
				}
				else if (tag.equalsIgnoreCase ("iconback"))
				{
					int i = 1;
					String img = parser.getAttributeValue (null, "img");

					if (img != null)
						fallback.add (img);

					while ((img = parser.getAttributeValue (null, "img" + i)) != null)
					{
						fallback.add (img);

						i++;
					}
				}
				/*else if (tag.equalsIgnoreCase ("scale"))
				{
					String factor = parser.getAttributeValue (null, "factor");

					if (factor != null)
						fallbackScale = Float.parseFloat (factor);
				}*/
			}
		}

		this.resourceMap = resourceMap;
		this.fallback = fallback;
		this.fallbackScale = fallbackScale;
	}

	public boolean isIconPackLoaded ()
	{
		return this.iconPackLoaded;
	}
}

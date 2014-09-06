package be.robinj.ubuntu;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.text.TextUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private Map<String, String> resourceMap;
	private Resources resources;

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

		Map<String, String> resMap = this.parseXml (parser);
	}

	private Map<String, String> parseXml (XmlPullParser parser) throws XmlPullParserException, IOException
	{
		Map<String, String> resourceMap = new HashMap<String, String> ();
		int event;

		while ((event = parser.next ()) != XmlPullParser.END_DOCUMENT)
		{
			if (event == XmlPullParser.START_TAG && parser.getName ().equalsIgnoreCase ("item"))
			{
				String component = parser.getAttributeValue (null, "component");
				String drawable = parser.getAttributeValue (null, "drawable");

				if
				(
					! (TextUtils.isEmpty (component) || TextUtils.isEmpty (drawable))
					&& component.startsWith ("ComponentInfo{") && component.endsWith ("}") && component.length () >= 16
				)
				{
					component = component.substring (14, component.length () -1).toLowerCase ();

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
		}

		return resourceMap;
	}
}

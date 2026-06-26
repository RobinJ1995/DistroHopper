package be.robinj.distrohopper;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import be.robinj.distrohopper.desktop.AppIcon;

/**
 * Created by robin on 06/09/14.
 */
public class IconPackHelper
{
	private final Context context;
	private String name;
	private boolean iconPackLoaded = false;

	private Resources iconPackRes = null;
	private final Map<String, String> componentToDrawable = new HashMap<>();
	private final Map<String, String> packageToDrawable = new HashMap<>();

	public IconPackHelper (Context context)
	{
		this.context = context;
	}

	public Map<String, ResolveInfo> getIconPacks ()
	{
		final PackageManager pm = this.context.getPackageManager();
		final Map<String, ResolveInfo> result = new LinkedHashMap<>();

		final String[] actions = new String[] {
			"org.adw.launcher.THEMES",
			"com.gau.go.launcherex.theme",
			"com.novalauncher.THEME",
			"com.teslacoilsw.launcher.THEME",
			"com.anddoes.launcher.THEME"
		};

		for (final String action : actions) {
			final Intent intent = new Intent(action);
			final List<ResolveInfo> infos = pm.queryIntentActivities(intent, 0);
			if (infos == null) continue;
			for (final ResolveInfo ri : infos) {
				final String pkg = ri.activityInfo != null && ri.activityInfo.packageName != null
						? ri.activityInfo.packageName
						: (ri.resolvePackageName != null ? ri.resolvePackageName : null);
				if (pkg != null && !result.containsKey(pkg)) {
					result.put(pkg, ri);
				}
			}
		}

		return result;
	}

	public void loadIconPack (String packageName) throws PackageManager.NameNotFoundException, IOException, XmlPullParserException
	{
		this.name = packageName;
		this.iconPackLoaded = false;
		this.componentToDrawable.clear();
		this.packageToDrawable.clear();

		if (TextUtils.isEmpty(packageName)) {
			this.iconPackRes = null;
			return;
		}

		final PackageManager pm = this.context.getPackageManager();
		this.iconPackRes = pm.getResourcesForApplication(packageName);

		// Try res/xml/appfilter.xml first
		int xmlId = this.iconPackRes.getIdentifier("appfilter", "xml", packageName);
		XmlPullParser xpp = null;
		InputStream assetStream = null;
		try {
			if (xmlId != 0) {
				xpp = this.iconPackRes.getXml(xmlId);
			} else {
				// Fallback to assets/appfilter.xml
				try {
					assetStream = this.iconPackRes.getAssets().open("appfilter.xml");
					xpp = android.util.Xml.newPullParser();
					xpp.setInput(assetStream, "utf-8");
				} catch (IOException ignore) { /* ignore */ }
			}

			if (xpp != null) {
				parseAppFilter(xpp);
			}
		} finally {
			if (assetStream != null) try { assetStream.close(); } catch (IOException ignore) {}
		}

		this.iconPackLoaded = this.iconPackRes != null && (!this.componentToDrawable.isEmpty() || !this.packageToDrawable.isEmpty());
	}

	private void parseAppFilter(XmlPullParser xpp) throws IOException, XmlPullParserException {
		int eventType = xpp.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG) {
				final String tag = xpp.getName();
				if ("item".equalsIgnoreCase(tag)) {
					final String component = xpp.getAttributeValue(null, "component");
					final String drawable = xpp.getAttributeValue(null, "drawable");
					final String pkg = xpp.getAttributeValue(null, "package");

					if (!TextUtils.isEmpty(drawable)) {
						if (!TextUtils.isEmpty(component)) {
							componentToDrawable.put(component.trim(), drawable.trim());
						}
						if (!TextUtils.isEmpty(pkg)) {
							packageToDrawable.put(pkg.trim(), drawable.trim());
						}
					}
				}
			}
			eventType = xpp.next();
		}
	}

	public AppIcon getIconForApp (App app)
	{
		if (!this.iconPackLoaded || this.iconPackRes == null) return null;

		// First try full ComponentInfo key as used by most icon packs
		final String pkg = app.getPackageName();
		final String act = app.getActivityName();

		final String compFull = "ComponentInfo{" + pkg + "/" + act + "}";
		String drawableName = componentToDrawable.get(compFull);

		if (drawableName == null) {
			// Try with a shortened activity name (with leading dot) if applicable
			String shortAct = act;
			if (shortAct.startsWith(pkg)) {
				shortAct = shortAct.substring(pkg.length());
				if (!shortAct.startsWith(".")) shortAct = "." + shortAct;
			}
			final String compShort = "ComponentInfo{" + pkg + "/" + shortAct + "}";
			drawableName = componentToDrawable.get(compShort);
		}

		if (drawableName == null) {
			// As a last resort try package-only mapping
			drawableName = packageToDrawable.get(pkg);
		}

		if (TextUtils.isEmpty(drawableName)) {
			return null;
		}

		final Drawable d = getIcon(drawableName);
		if (d == null) return null;

		return new AppIcon(d);
	}

	public AppIcon getFallbackIcon (Drawable appIcon)
	{
		// Minimal implementation: return the original app icon wrapped.
		return new AppIcon(appIcon);
	}

	public Drawable getIcon (String iconName)
	{
		if (this.iconPackRes == null || TextUtils.isEmpty(iconName)) return null;

		// Try drawable then mipmap
		int id = this.iconPackRes.getIdentifier(iconName, "drawable", this.name);
		if (id == 0) id = this.iconPackRes.getIdentifier(iconName, "mipmap", this.name);
		if (id == 0) return null;

		try {
			return this.iconPackRes.getDrawable(id, null);
		} catch (Resources.NotFoundException ex) {
			return null;
		}
	}

	public boolean isIconPackLoaded ()
	{
		return this.iconPackLoaded;
	}
}

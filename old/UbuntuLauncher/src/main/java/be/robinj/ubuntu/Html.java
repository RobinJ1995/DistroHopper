package be.robinj.ubuntu;

import android.webkit.JavascriptInterface;

public class Html
{
	private static String tplAppLauncher = "<div class=\"-[class]- appLauncher\" -[data]- -[style]->" +
		"<img src=\"-[icon]-\" alt=\"-[label]-\" />" +
		"<span>-[label]-</span>" +
		"</div>";
	private static String tplDashPage = "<div class=\"dashPage -[active]-\" -[index]->" +
		"-[content]-" +
		"</div>";
	private static String tplAppInfo = "<h2>-[label]-</h2>" +
		"<p><img src=\"-[icon]-\" alt=\"-[icon]-\" />-[info]-</p>";

	@JavascriptInterface
	public static String appLauncher (AppLauncher app)
	{
		return Html.appLauncher (app, "");
	}

	@JavascriptInterface
	public static String appLauncher (AppLauncher app, String htmlClass)
	{
		return Html.appLauncher (app, "", null, false);
	}

	@JavascriptInterface
	public static String appLauncher (AppLauncher app, String[][] data)
	{
		return Html.appLauncher (app, "", data, false);
	}

	@JavascriptInterface
	public static String appLauncher (AppLauncher app, String htmlClass, String[][] data, boolean backgroundColour)
	{
		StringBuilder html = new StringBuilder (Html.tplAppLauncher);

		if (data != null)
		{
			StringBuilder buildStrData = new StringBuilder ();
			for (String[] attr : data)
				buildStrData.append (" data-" + attr[0] + "=\"" + attr[1] + "\"");

			Html.replace (html, "data", buildStrData.toString ());
		}

		Html.replace (html, "class", htmlClass);
		Html.replace (html, "icon", app.getIcon ().getPath ());
		Html.replace (html, "label", app.getLabel ());
		Html.replace (html, "style", (backgroundColour ? "style=\"background-color: rgb(" + app.getIcon ().getAverageColourRgb () + ");\"" : ""));

		return html.toString ();
	}

	@JavascriptInterface
	public static String appInfo (AppLauncher app)
	{
		StringBuilder html = new StringBuilder (Html.tplAppInfo);

		StringBuilder buildStr = new StringBuilder ();
		String description = app.getDescription ();
		if (! (description == null || description.equals ("")))
			buildStr.append ("<strong>Description:</strong> ").append (app.getDescription ()).append ("<br />");
		buildStr.append ("<strong>Times launched:</strong> ").append (app.getTimesLaunched ()).append ("<br />");
		buildStr.append ("<strong>Data folder:</strong> ").append (app.getDataFolder ()).append ("<br />");
		buildStr.append ("<strong>Target SDK:</strong> ").append (app.getTargetSdk ());

		Html.replace (html, "icon", app.getIcon ().getPath ());
		Html.replace (html, "label", app.getLabel ());
		Html.replace (html, "info", buildStr.toString ());

		return html.toString ();
	}

	@JavascriptInterface
	public static String dashApps (int index, boolean active, String content)
	{
		String html = Html.tplDashPage;

		html = Html.replace (html, "active", active ? "activeDashPage" : "");
		html = Html.replace (html, "index", "data-index=\"" + index + "\"");
		html = Html.replace (html, "content", content);

		return html;
	}

	@JavascriptInterface
	public static String rgbToString (int[] rgb)
	{
		return rgb[0] + "," + rgb[1] + "," + rgb[2];
	}

	private static String replace (String stack, String needle, String replacement)
	{
		StringBuilder search = new StringBuilder ("-[");
		search.append (needle);
		search.append ("]-");

		String str = search.toString ();

		while (stack.contains (str))
			stack = stack.replace (str, replacement);

		return stack;
	}

	public static void replace (StringBuilder stack, String needle, String replacement)
	{
		StringBuilder search = new StringBuilder ("-[");
		search.append (needle);
		search.append ("]-");

		String str = search.toString ();

		int index = stack.indexOf (str);

		while (index != -1)
		{
			stack.replace (index, index + str.length (), replacement);
			index += replacement.length ();
			index = stack.indexOf (str, index);
		}
	}
}

package be.robinj.distrohopper.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import be.robinj.distrohopper.DependencyContainer;
import be.robinj.distrohopper.InsetsHelper;
import be.robinj.distrohopper.R;
import be.robinj.distrohopper.theme.Theme;
import be.robinj.distrohopper.theme.ThemeCards;
import be.robinj.distrohopper.theme.ThemeRegistry;

public class ThemePreferencesActivity extends AppCompatActivity
{

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_theme_preferences);
		InsetsHelper.applySystemBarsPadding (this);

		List<Theme> themes = new ArrayList<Theme> ();
		for (final kotlin.jvm.functions.Function0<Theme> factory : ThemeRegistry.INSTANCE.getThemes ().values ())
			themes.add (factory.invoke ());

		SharedPreferences prefs = Preferences.getSharedPreferences(this, Preferences.PREFERENCES);

		if (! prefs.getBoolean (Preference.DEV.getName(), false))
		{
			// removeIf, not an index loop: removing by index while advancing skips
			// the element that shifts into the freed slot, so consecutive dev-only
			// themes would leak through //
			themes.removeIf (theme -> theme.dev_only);
		}

		final LinearLayout llThemeCards = this.findViewById (R.id.llThemeCards);
		new ThemeCards (
			themes,
			() -> DependencyContainer.of (this).getThemeManager ().getCurrent ().getName (),
			theme ->
			{
				// Stay on this screen: the card highlight is the feedback, and
				// the home screen recreates itself in the new theme underneath //
				ThemeCards.applyTheme (this, theme);

				return kotlin.Unit.INSTANCE;
			}
		).bind (llThemeCards);
	}

	@Override
	public boolean onCreateOptionsMenu (Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater ().inflate (R.menu.menu_theme_preferences, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected (MenuItem item)
	{
		return super.onOptionsItemSelected (item);
	}
}

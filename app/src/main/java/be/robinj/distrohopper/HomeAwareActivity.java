package be.robinj.distrohopper;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Base class for the settings / "about" sub-screens that are launched on top of
 * {@link HomeActivity} while DistroHopper is the default launcher.
 *
 * <p>When DistroHopper holds the HOME role, these screens live in the launcher's
 * own task, sitting on top of {@link HomeActivity} at the task root. Pressing the
 * device HOME button (or the home navigation gesture) re-launches the HOME intent,
 * but the system simply brings the already-foreground task forward as-is, leaving
 * the settings screen on top — so HOME appears to do nothing. Users expect HOME to
 * drop them back onto the home screen.</p>
 *
 * <p>{@link #onUserLeaveHint()} fires precisely when the user leaves of their own
 * accord (HOME / Recents), and crucially <em>not</em> when this screen starts
 * another activity (e.g. navigating Preferences → About). When we are the default
 * launcher, we use it to clear back down to {@link HomeActivity} so returning to
 * the task shows the home screen rather than the settings stack.</p>
 *
 * <p>This is deliberately gated on {@link HomeRole#isHeld}: when DistroHopper is an
 * ordinary app, HOME should leave for the real launcher, so we must not interfere.
 * The first-run wizard ({@code OnboardingActivity}) does not extend this and so is
 * never short-circuited.</p>
 */
public abstract class HomeAwareActivity extends AppCompatActivity
{
	@Override
	protected void onUserLeaveHint ()
	{
		super.onUserLeaveHint ();

		try
		{
			// Only take over HOME while we are the launcher; otherwise let the user
			// leave for their actual home screen undisturbed. //
			if (! HomeRole.isHeld (this))
				return;

			// Bring HomeActivity to the front, finishing every settings screen stacked
			// above it (CLEAR_TOP), and deliver the intent to the existing instance
			// rather than recreating it (SINGLE_TOP). //
			final Intent home = new Intent (this, HomeActivity.class)
				.addFlags (Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			this.startActivity (home);
		}
		catch (Exception ex)
		{
			new ExceptionHandler (ex).logAndTrack ();
		}
	}
}

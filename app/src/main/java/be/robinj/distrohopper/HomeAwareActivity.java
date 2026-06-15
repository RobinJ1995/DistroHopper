package be.robinj.distrohopper;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
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
 * <p>{@link #onUserLeaveHint()} fires when the user leaves of their own accord. That
 * covers a HOME / Recents press, but <em>also</em> the user navigating deeper — e.g.
 * tapping "Theme" or "Icons", which launch another activity. We must only return to
 * the home screen for the former, never the latter (otherwise opening a sub-screen
 * would bounce the user to the home screen). {@link #startActivityForResult} — the
 * single funnel every activity / fragment / {@code ActivityResultLauncher} start
 * routes through — sets {@link #launchingChildActivity}, which {@link #onUserLeaveHint()}
 * uses to tell "I'm opening another screen" apart from "the user pressed HOME".</p>
 *
 * <p>This is deliberately gated on {@link HomeRole#isHeld}: when DistroHopper is an
 * ordinary app, HOME should leave for the real launcher, so we must not interfere.
 * The first-run wizard ({@code OnboardingActivity}) does not extend this and so is
 * never short-circuited.</p>
 */
public abstract class HomeAwareActivity extends AppCompatActivity
{
	// Set whenever this screen itself starts another activity (the user navigating
	// deeper, e.g. Preferences -> Theme), so the onUserLeaveHint that immediately
	// follows isn't mistaken for a HOME press. Reset once we're resumed again. //
	private boolean launchingChildActivity = false;

	// Every startActivity / startActivityForResult / ActivityResultLauncher start
	// ultimately routes through here, so this is the one place that needs to record
	// that we are the ones leaving (as opposed to the system delivering HOME). //
	@Override
	public void startActivityForResult (final Intent intent, final int requestCode,
		@Nullable final Bundle options)
	{
		this.launchingChildActivity = true;
		super.startActivityForResult (intent, requestCode, options);
	}

	@Override
	protected void onResume ()
	{
		super.onResume ();

		// Back on this screen: any child launch that was in flight is done. //
		this.launchingChildActivity = false;
	}

	@Override
	protected void onUserLeaveHint ()
	{
		super.onUserLeaveHint ();

		try
		{
			// Navigating to a sub-screen (Theme/Icons/About/...) also triggers this;
			// only a genuine HOME / Recents press should return to the home screen. //
			final boolean leavingForChild = this.launchingChildActivity;
			this.launchingChildActivity = false;
			if (leavingForChild)
				return;

			this.returnToHomeScreenIfLauncher ();
		}
		catch (Exception ex)
		{
			new ExceptionHandler (ex).logAndTrack ();
		}
	}

	/**
	 * While DistroHopper is the default launcher, clear back down to
	 * {@link HomeActivity} so a HOME press from a settings screen lands the user on
	 * the home screen. A no-op when DistroHopper is an ordinary app, where HOME
	 * should leave for the real launcher undisturbed. Package-visible for tests.
	 */
	void returnToHomeScreenIfLauncher ()
	{
		if (! HomeRole.isHeld (this))
			return;

		// Bring HomeActivity to the front, finishing every settings screen stacked
		// above it (CLEAR_TOP), and deliver to the existing instance rather than
		// recreating it (SINGLE_TOP). //
		final Intent home = new Intent (this, HomeActivity.class)
			.addFlags (Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		this.startActivity (home);
	}
}

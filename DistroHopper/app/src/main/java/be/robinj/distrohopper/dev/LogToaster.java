package be.robinj.distrohopper.dev;

import android.app.Activity;
import android.widget.Toast;

import be.robinj.distrohopper.IObserver;

public class LogToaster implements IObserver {
	private final Log log;
	private final Activity parent;

	public LogToaster(final Activity parent) {
		this.log = Log.getInstance();
		this.parent = parent;
	}

	@Override
	public void nudge() {
		this.parent.runOnUiThread(() ->
			Toast.makeText(parent, log.getLastEntry(), Toast.LENGTH_LONG).show()
		);
	}
}

package be.robinj.distrohopper;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by robin on 25/07/15.
 */
public abstract class Observed
{
	// Copy-on-write because observables are nudged from background threads (the dev log
	// is written by the app loaders) while observers attach and detach on the main one:
	// a plain ArrayList throws ConcurrentModificationException out of nudgeObservers. //
	private List<IObserver> observers = new CopyOnWriteArrayList<IObserver> ();

	public void attachObserver (IObserver observer)
	{
		this.observers.add (observer);
	}

	public void detachObserver (IObserver observer)
	{
		this.observers.remove (observer);
	}

	protected void nudgeObservers ()
	{
		for (IObserver observer : this.observers)
			observer.nudge ();
	}
}

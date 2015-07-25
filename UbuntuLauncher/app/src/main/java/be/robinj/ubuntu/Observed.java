package be.robinj.ubuntu;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by robin on 25/07/15.
 */
public abstract class Observed
{
	private List<IObserver> observers = new ArrayList<IObserver> ();

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
